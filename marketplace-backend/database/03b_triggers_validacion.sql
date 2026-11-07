-- =============================================================================
-- Marketplace - 03b_triggers_validacion.sql
-- Validaciones al insertar. Se ejecuta después de 03_triggers.sql. Se puede
-- ejecutar sobre una base con datos y repetir: solo actúa sobre INSERT nuevos.
--
-- Son una segunda barrera: el backend ya valida todo esto y responde con
-- mensajes propios, así que estos errores solo deberían verse con SQL directo.
--
-- Principio: VALIDAN, NO CORRIGEN. Ningún trigger reescribe un valor: si algo no
-- cuadra, la fila se rechaza. Corregir en silencio ocultaría el error y dejaría a
-- Hibernate con valores distintos de los guardados.
--
-- Dos niveles de validación:
--   * Coherencia interna (siempre): los cálculos y la relación con la cotización
--     o la reserva.
--   * Estado vigente (solo en operación normal): alojamiento activo, precio y
--     tarifa actuales, rol del usuario, reserva nueva en PENDIENTE_PAGO, etc.
--     Se omite en "modo carga", para poder cargar datos históricos:
--       PowerShell: $env:PGOPTIONS = '-c marketplace.carga_datos=on'
--       o dentro de la transacción: SET LOCAL marketplace.carga_datos = 'on';
--     No es un control de seguridad (cualquier sesión puede activarlo): evita
--     errores. La seguridad la dan los permisos de 08_produccion.sql.
--
-- Códigos de error: los de 03_triggers.sql (check_violation, foreign_key_violation).
-- =============================================================================
SET client_encoding = 'UTF8';

CREATE OR REPLACE FUNCTION fn_es_carga_datos() RETURNS BOOLEAN
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(current_setting('marketplace.carga_datos', true), '') = 'on';
$$;

-- -----------------------------------------------------------------------------
-- 7. Cotización: los cálculos de CotizacionService.calcular y el catálogo vigente.
--    Tabla: cotizacion. Evento: BEFORE INSERT.
--    El total y las noches ya los exigen ck_cotizacion_total y ck_cotizacion_noches.
--    valor_adicionales no se recalcula: depende del JSON de desglose.
--    Si el anfitrión cambia el precio o el administrador la tarifa justo mientras
--    se cotiza, la cotización se rechaza y el usuario vuelve a cotizar.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_cotizacion() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_alojamiento alojamiento%ROWTYPE;
    v_limpieza    NUMERIC(5, 2);
    v_servicio    NUMERIC(5, 2);
    v_neto        NUMERIC(18, 2);
BEGIN
    -- Coherencia interna, con el mismo redondeo (HALF_UP a 2 decimales).
    IF NEW.precio_base IS DISTINCT FROM ROUND(NEW.precio_noche * (NEW.fecha_salida - NEW.fecha_entrada), 2) THEN
        RAISE EXCEPTION 'El precio base (%) no es precio por noche (%) por noches (%)',
            NEW.precio_base, NEW.precio_noche, NEW.fecha_salida - NEW.fecha_entrada
            USING ERRCODE = 'check_violation';
    END IF;
    v_neto := NEW.precio_base - NEW.descuento;
    IF NEW.valor_limpieza IS DISTINCT FROM ROUND(v_neto * NEW.porcentaje_limpieza / 100, 2)
       OR NEW.valor_servicio IS DISTINCT FROM ROUND(v_neto * NEW.porcentaje_servicio / 100, 2) THEN
        RAISE EXCEPTION 'Los valores de limpieza (%) y servicio (%) no corresponden a sus porcentajes',
            NEW.valor_limpieza, NEW.valor_servicio USING ERRCODE = 'check_violation';
    END IF;

    IF fn_es_carga_datos() THEN
        RETURN NEW;
    END IF;

    -- Catálogo vigente.
    SELECT * INTO v_alojamiento FROM alojamiento WHERE id_alojamiento = NEW.id_alojamiento;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El alojamiento % no existe', NEW.id_alojamiento
            USING ERRCODE = 'foreign_key_violation';
    END IF;
    IF v_alojamiento.estado <> 'ACTIVA' THEN
        RAISE EXCEPTION 'No se puede cotizar un alojamiento en estado %', v_alojamiento.estado
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.huespedes > v_alojamiento.capacidad THEN
        RAISE EXCEPTION 'La cantidad de huéspedes (%) supera la capacidad del alojamiento (%)',
            NEW.huespedes, v_alojamiento.capacidad USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.precio_noche IS DISTINCT FROM v_alojamiento.precio_noche THEN
        RAISE EXCEPTION 'El precio por noche (%) no es el vigente del alojamiento (%)',
            NEW.precio_noche, v_alojamiento.precio_noche USING ERRCODE = 'check_violation';
    END IF;

    -- Igual que TarifaService: la del alojamiento, si no la global vigente, si no 5 % y 10 %.
    SELECT porcentaje_limpieza, porcentaje_servicio INTO v_limpieza, v_servicio
      FROM configuracion_tarifa
     ORDER BY fecha DESC, id_configuracion DESC
     LIMIT 1;
    v_limpieza := COALESCE(v_alojamiento.porcentaje_limpieza, v_limpieza, 5);
    v_servicio := COALESCE(v_alojamiento.porcentaje_servicio, v_servicio, 10);
    IF NEW.porcentaje_limpieza IS DISTINCT FROM v_limpieza
       OR NEW.porcentaje_servicio IS DISTINCT FROM v_servicio THEN
        RAISE EXCEPTION 'Los porcentajes (limpieza %, servicio %) no son los vigentes (%, %)',
            NEW.porcentaje_limpieza, NEW.porcentaje_servicio, v_limpieza, v_servicio
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_cotizacion ON cotizacion;
CREATE TRIGGER trg_validar_cotizacion
    BEFORE INSERT ON cotizacion
    FOR EACH ROW EXECUTE FUNCTION fn_validar_cotizacion();

-- -----------------------------------------------------------------------------
-- 8. Reserva: copia fiel de su cotización y reglas de ReservaService.reservarPendiente.
--    Tabla: reserva. Evento: BEFORE INSERT.
--    Bloquea la cotización (FOR UPDATE): dos reservas con la misma cotización
--    quedan en fila y la segunda ya la ve usada. El backend llena
--    cotizacion.id_reserva justo después, en la misma transacción.
--    La vigencia de la cotización la valida el backend con la hora de la JVM.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_reserva() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_carga       BOOLEAN := fn_es_carga_datos();
    v_cotizacion  cotizacion%ROWTYPE;
    v_alojamiento alojamiento%ROWTYPE;
    v_rol         VARCHAR(20);
    v_estado      VARCHAR(20);
BEGIN
    -- Una reserva confirmada sin pasar por el pago no puede nacer por SQL directo.
    IF NEW.estado <> 'PENDIENTE_PAGO' AND NOT v_carga THEN
        RAISE EXCEPTION 'Una reserva nueva empieza en PENDIENTE_PAGO (recibido: %)', NEW.estado
            USING ERRCODE = 'check_violation';
    END IF;

    SELECT * INTO v_cotizacion FROM cotizacion WHERE id_cotizacion = NEW.id_cotizacion FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cotización % no existe', NEW.id_cotizacion
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF NOT v_carga THEN
        SELECT * INTO v_alojamiento FROM alojamiento WHERE id_alojamiento = NEW.id_alojamiento;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'El alojamiento % no existe', NEW.id_alojamiento
                USING ERRCODE = 'foreign_key_violation';
        END IF;
        IF v_alojamiento.estado <> 'ACTIVA' THEN
            RAISE EXCEPTION 'No se puede reservar un alojamiento en estado %', v_alojamiento.estado
                USING ERRCODE = 'check_violation';
        END IF;
        IF v_alojamiento.id_anfitrion = NEW.id_huesped THEN
            RAISE EXCEPTION 'Un anfitrión no puede reservar su propio alojamiento'
                USING ERRCODE = 'check_violation';
        END IF;

        SELECT rol, estado INTO v_rol, v_estado FROM usuario WHERE id_usuario = NEW.id_huesped;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'El usuario % no existe', NEW.id_huesped
                USING ERRCODE = 'foreign_key_violation';
        END IF;
        IF v_rol <> 'HUESPED' OR v_estado <> 'ACTIVO' THEN
            RAISE EXCEPTION 'Solo un HUESPED ACTIVO puede reservar (usuario %: %, %)', NEW.id_huesped, v_rol, v_estado
                USING ERRCODE = 'check_violation';
        END IF;

        IF v_cotizacion.id_reserva IS NOT NULL THEN
            RAISE EXCEPTION 'La cotización % ya fue usada por la reserva %', NEW.id_cotizacion, v_cotizacion.id_reserva
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;

    -- Coherencia con la cotización, también en modo carga.
    IF v_cotizacion.id_usuario <> NEW.id_huesped OR v_cotizacion.id_alojamiento <> NEW.id_alojamiento THEN
        RAISE EXCEPTION 'La cotización % no corresponde a este huésped y alojamiento', NEW.id_cotizacion
            USING ERRCODE = 'check_violation';
    END IF;
    IF (NEW.fecha_entrada, NEW.fecha_salida, NEW.noches, NEW.huespedes, NEW.total)
       IS DISTINCT FROM (v_cotizacion.fecha_entrada, v_cotizacion.fecha_salida, v_cotizacion.noches,
                         v_cotizacion.huespedes, v_cotizacion.total) THEN
        RAISE EXCEPTION 'Las fechas, huéspedes o total de la reserva no son los de la cotización %', NEW.id_cotizacion
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_reserva ON reserva;
CREATE TRIGGER trg_validar_reserva
    BEFORE INSERT ON reserva
    FOR EACH ROW EXECUTE FUNCTION fn_validar_reserva();

-- -----------------------------------------------------------------------------
-- 9. Pago: proveedor válido, valor igual al total de la reserva y reembolso
--    coherente.
--    Tabla: pago. Evento: BEFORE INSERT.
--    No se valida el estado de la reserva: si un pago tarda, la reserva pudo
--    cancelarse mientras tanto, y un cobro ya realizado debe quedar registrado.
--    Un pago nace con el resultado de la pasarela; REEMBOLSADO llega después por
--    UPDATE (salvo en modo carga).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_pago() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_total NUMERIC(18, 2);
BEGIN
    -- PagoService.PROVEEDOR_SIMULADO y StripeService.PROVEEDOR.
    IF NEW.proveedor NOT IN ('STRIPE_TEST', 'SIMULADO') THEN
        RAISE EXCEPTION 'El proveedor de pago % no es válido', NEW.proveedor
            USING ERRCODE = 'check_violation';
    END IF;

    SELECT total INTO v_total FROM reserva WHERE id_reserva = NEW.id_reserva;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La reserva % no existe', NEW.id_reserva
            USING ERRCODE = 'foreign_key_violation';
    END IF;
    IF NEW.valor IS DISTINCT FROM v_total THEN
        RAISE EXCEPTION 'El valor del pago (%) no es el total de la reserva % (%)', NEW.valor, NEW.id_reserva, v_total
            USING ERRCODE = 'check_violation';
    END IF;

    IF NEW.estado = 'REEMBOLSADO' AND NOT fn_es_carga_datos() THEN
        RAISE EXCEPTION 'Un pago se registra con el resultado del cobro; el reembolso se aplica después'
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.estado <> 'REEMBOLSADO'
       AND (NEW.valor_reembolsado IS NOT NULL OR NEW.referencia_reembolso IS NOT NULL) THEN
        RAISE EXCEPTION 'Un pago en estado % no puede tener datos de reembolso', NEW.estado
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_pago ON pago;
CREATE TRIGGER trg_validar_pago
    BEFORE INSERT ON pago
    FOR EACH ROW EXECUTE FUNCTION fn_validar_pago();

-- -----------------------------------------------------------------------------
-- 10. Cancelación: quién cancela y cuánto se devuelve.
--     Tabla: cancelacion. Evento: BEFORE INSERT.
--     Se acepta la reserva aún CONFIRMADA o EN_REVISION (CancelacionService la
--     inserta antes del UPDATE de la reserva) o ya CANCELADA (sp_cancelar_reserva
--     actualiza primero). Que termine CANCELADA lo exige al COMMIT
--     trg_cancelacion_reserva_cancelada (03_triggers.sql).
--     La devolución usa el mismo redondeo de CancelacionService.devolucion.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_cancelacion() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_estado    VARCHAR(20);
    v_total     NUMERIC(18, 2);
    v_huesped   BIGINT;
    v_rol       VARCHAR(20);
    v_esperado  NUMERIC(18, 2);
BEGIN
    SELECT estado, total, id_huesped INTO v_estado, v_total, v_huesped
      FROM reserva WHERE id_reserva = NEW.id_reserva;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La reserva % no existe', NEW.id_reserva
            USING ERRCODE = 'foreign_key_violation';
    END IF;
    IF v_estado = 'PENDIENTE_PAGO' THEN
        RAISE EXCEPTION 'No se puede cancelar una reserva pendiente de pago'
            USING ERRCODE = 'check_violation';
    END IF;

    SELECT rol INTO v_rol FROM usuario WHERE id_usuario = NEW.id_usuario;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El usuario % no existe', NEW.id_usuario
            USING ERRCODE = 'foreign_key_violation';
    END IF;
    IF NEW.id_usuario <> v_huesped AND v_rol <> 'ADMIN' THEN
        RAISE EXCEPTION 'Solo el huésped de la reserva o un administrador pueden cancelarla'
            USING ERRCODE = 'check_violation';
    END IF;

    IF NEW.estado_final_reserva <> 'CANCELADA' THEN
        RAISE EXCEPTION 'Una cancelación deja la reserva CANCELADA (recibido: %)', NEW.estado_final_reserva
            USING ERRCODE = 'check_violation';
    END IF;

    v_esperado := ROUND(v_total * NEW.porcentaje_reembolso / 100, 2);
    IF NEW.valor_reembolso IS DISTINCT FROM v_esperado THEN
        RAISE EXCEPTION 'La devolución (%) no es el % %% del total de la reserva (%)',
            NEW.valor_reembolso, NEW.porcentaje_reembolso, v_esperado
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_cancelacion ON cancelacion;
CREATE TRIGGER trg_validar_cancelacion
    BEFORE INSERT ON cancelacion
    FOR EACH ROW EXECUTE FUNCTION fn_validar_cancelacion();

-- -----------------------------------------------------------------------------
-- 11. Mensaje: quien escribe participa en la conversación.
--     Tabla: mensaje. Evento: BEFORE INSERT.
--     No se rechazan las conversaciones CERRADAS porque el backend hoy no lo hace.
--     El contenido llega recortado desde MensajeriaService; el vacío lo rechaza
--     ck_mensaje_contenido.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_mensaje() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_huesped   BIGINT;
    v_anfitrion BIGINT;
BEGIN
    SELECT id_usuario, id_anfitrion INTO v_huesped, v_anfitrion
      FROM conversacion WHERE id_conversacion = NEW.id_conversacion;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La conversación % no existe', NEW.id_conversacion
            USING ERRCODE = 'foreign_key_violation';
    END IF;
    IF NEW.id_usuario IS DISTINCT FROM v_huesped AND NEW.id_usuario IS DISTINCT FROM v_anfitrion THEN
        RAISE EXCEPTION 'El usuario % no participa en la conversación %', NEW.id_usuario, NEW.id_conversacion
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_mensaje ON mensaje;
CREATE TRIGGER trg_validar_mensaje
    BEFORE INSERT ON mensaje
    FOR EACH ROW EXECUTE FUNCTION fn_validar_mensaje();
