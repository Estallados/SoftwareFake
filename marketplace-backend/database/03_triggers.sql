-- =============================================================================
-- Marketplace - 03_triggers.sql
-- Reglas de integridad que una restricción declarativa no puede expresar:
-- registros inmutables, columnas que no cambian después de insertar,
-- transiciones de estado y reglas que dependen de otra tabla.
--
-- Ninguno modifica filas: o dejan pasar la operación o la rechazan. Así lo que
-- Hibernate tiene en memoria es siempre lo que quedó en la base.
--
-- Se puede ejecutar sobre una base con datos y repetir sin problema: reemplaza
-- las funciones y vuelve a crear los triggers. Solo actúa sobre operaciones nuevas.
--
-- Códigos de error (SQLSTATE), los mismos en todos los triggers del proyecto:
--   restrict_violation    (23001) modificar o borrar algo que no se puede.
--   check_violation       (23514) una regla de negocio no se cumple.
--   foreign_key_violation (23503) falta el registro relacionado.
-- Spring los traduce a DataIntegrityViolationException.
--
-- No se crearon, a propósito:
--   * fecha_actualizacion: la asigna PublicacionService.marcarActualizado.
--   * historial y auditoría automáticos: los escriben PublicacionService y
--     AuditoriaService con actor y motivo, que un trigger no conoce.
-- =============================================================================
SET client_encoding = 'UTF8';

-- -----------------------------------------------------------------------------
-- 1. Auditoría, historial y mensajes inmutables (CA-150).
--    Tablas: evento_auditoria, historial_estado_alojamiento, mensaje.
--    Eventos: UPDATE, DELETE y TRUNCATE. @Immutable solo protege dentro de
--    Hibernate; esto también protege frente a SQL directo. El backend nunca
--    edita ni borra mensajes.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_rechazar_cambio_inmutable() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de % no se pueden modificar ni borrar', TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END;
$$;

DROP TRIGGER IF EXISTS trg_evento_inmutable ON evento_auditoria;
CREATE TRIGGER trg_evento_inmutable
    BEFORE UPDATE OR DELETE ON evento_auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_cambio_inmutable();
DROP TRIGGER IF EXISTS trg_evento_sin_truncate ON evento_auditoria;
CREATE TRIGGER trg_evento_sin_truncate
    BEFORE TRUNCATE ON evento_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_rechazar_cambio_inmutable();

DROP TRIGGER IF EXISTS trg_historial_inmutable ON historial_estado_alojamiento;
CREATE TRIGGER trg_historial_inmutable
    BEFORE UPDATE OR DELETE ON historial_estado_alojamiento
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_cambio_inmutable();
DROP TRIGGER IF EXISTS trg_historial_sin_truncate ON historial_estado_alojamiento;
CREATE TRIGGER trg_historial_sin_truncate
    BEFORE TRUNCATE ON historial_estado_alojamiento
    FOR EACH STATEMENT EXECUTE FUNCTION fn_rechazar_cambio_inmutable();

DROP TRIGGER IF EXISTS trg_mensaje_inmutable ON mensaje;
CREATE TRIGGER trg_mensaje_inmutable
    BEFORE UPDATE OR DELETE ON mensaje
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_cambio_inmutable();

-- -----------------------------------------------------------------------------
-- 2. Registros de negocio: no se borran y sus datos fijos no cambian.
--    Tablas: cotizacion, reserva, pago, cancelacion, resena, conversacion.
--    Evento: BEFORE UPDATE OR DELETE.
--    Función genérica: los argumentos del trigger son las columnas protegidas.
--    Hibernate reenvía todas las columnas al guardar, pero con los mismos
--    valores, así que solo falla un cambio real. Lo que sí cambia el backend
--    (estado, cotizacion.id_reserva, pago.detalle y los datos del reembolso)
--    queda fuera de la lista. El backend nunca borra estas filas.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_proteger_registro() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_nuevo   JSONB;
    v_viejo   JSONB;
    v_columna TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los registros de % no se borran: cambia su estado', TG_TABLE_NAME
            USING ERRCODE = 'restrict_violation';
    END IF;

    v_nuevo := to_jsonb(NEW);
    v_viejo := to_jsonb(OLD);
    FOR i IN 0 .. TG_NARGS - 1 LOOP
        v_columna := TG_ARGV[i];
        -- Un nombre mal escrito haría que la protección no hiciera nada: se avisa.
        IF NOT (v_viejo ? v_columna) THEN
            RAISE EXCEPTION 'El trigger % protege la columna %, que no existe en %',
                TG_NAME, v_columna, TG_TABLE_NAME USING ERRCODE = 'undefined_column';
        END IF;
        IF (v_nuevo -> v_columna) IS DISTINCT FROM (v_viejo -> v_columna) THEN
            RAISE EXCEPTION 'La columna %.% no se puede modificar (registro %)',
                TG_TABLE_NAME, v_columna, v_viejo -> TG_ARGV[0]
                USING ERRCODE = 'restrict_violation';
        END IF;
    END LOOP;
    RETURN NEW;
END;
$$;

-- El primer argumento es siempre la clave primaria (se usa en el mensaje).
DROP TRIGGER IF EXISTS trg_cotizacion_proteger ON cotizacion;
CREATE TRIGGER trg_cotizacion_proteger
    BEFORE UPDATE OR DELETE ON cotizacion
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro(
        'id_cotizacion', 'fecha_entrada', 'fecha_salida', 'huespedes', 'noches', 'precio_noche',
        'precio_base', 'descuento', 'porcentaje_limpieza', 'valor_limpieza', 'porcentaje_servicio',
        'valor_servicio', 'valor_adicionales', 'total', 'desglose', 'id_usuario', 'id_alojamiento',
        'fecha_creacion', 'fecha_expiracion');

DROP TRIGGER IF EXISTS trg_reserva_proteger ON reserva;
CREATE TRIGGER trg_reserva_proteger
    BEFORE UPDATE OR DELETE ON reserva
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro(
        'id_reserva', 'fecha_entrada', 'fecha_salida', 'noches', 'huespedes', 'total', 'desglose',
        'id_huesped', 'id_alojamiento', 'id_cotizacion', 'fecha_creacion');

DROP TRIGGER IF EXISTS trg_pago_proteger ON pago;
CREATE TRIGGER trg_pago_proteger
    BEFORE UPDATE OR DELETE ON pago
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro(
        'id_pago', 'referencia', 'proveedor', 'valor', 'metodo', 'fecha', 'id_reserva');

DROP TRIGGER IF EXISTS trg_cancelacion_proteger ON cancelacion;
CREATE TRIGGER trg_cancelacion_proteger
    BEFORE UPDATE OR DELETE ON cancelacion
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro(
        'id_cancelacion', 'motivo', 'politica', 'porcentaje_reembolso', 'valor_reembolso',
        'estado_final_reserva', 'fecha', 'id_usuario', 'id_reserva');

-- La moderación solo cambia el estado (PUBLICADA / OCULTA).
DROP TRIGGER IF EXISTS trg_resena_proteger ON resena;
CREATE TRIGGER trg_resena_proteger
    BEFORE UPDATE OR DELETE ON resena
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro(
        'id_resena', 'calificacion', 'comentario', 'fecha', 'id_usuario', 'id_reserva', 'id_alojamiento');

DROP TRIGGER IF EXISTS trg_conversacion_proteger ON conversacion;
CREATE TRIGGER trg_conversacion_proteger
    BEFORE UPDATE OR DELETE ON conversacion
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_registro(
        'id_conversacion', 'fecha_creacion', 'id_usuario', 'id_anfitrion', 'id_alojamiento');

-- -----------------------------------------------------------------------------
-- 3. Transiciones válidas de una reserva.
--    Tabla: reserva. Evento: BEFORE UPDATE OF estado.
--    Un CHECK solo ve la fila nueva, no el estado anterior. Las transiciones salen
--    de ReservaService, CancelacionService y PublicacionService.bloquear; la
--    EN_REVISION -> CONFIRMADA la usa sp_actualizar_estado_reserva.
--    Ninguna reserva queda CONFIRMADA sin un pago APROBADO: ReservaService.cerrar
--    confirma después de que PagoService guardó el pago en su propia transacción.
--    Hibernate reenvía el mismo estado al guardar: eso siempre se permite.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_transicion_reserva() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;

    IF NOT ((OLD.estado = 'PENDIENTE_PAGO' AND NEW.estado IN ('CONFIRMADA', 'EN_REVISION', 'CANCELADA'))
         OR (OLD.estado = 'CONFIRMADA'     AND NEW.estado IN ('EN_REVISION', 'CANCELADA'))
         OR (OLD.estado = 'EN_REVISION'    AND NEW.estado IN ('CONFIRMADA', 'CANCELADA'))) THEN
        RAISE EXCEPTION 'La reserva % no puede pasar de % a %', OLD.id_reserva, OLD.estado, NEW.estado
            USING ERRCODE = 'check_violation';
    END IF;

    IF NEW.estado = 'CONFIRMADA' AND NOT EXISTS (
            SELECT 1 FROM pago WHERE id_reserva = NEW.id_reserva AND estado = 'APROBADO') THEN
        RAISE EXCEPTION 'La reserva % no tiene un pago aprobado y no se puede confirmar', NEW.id_reserva
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_reserva_transicion ON reserva;
CREATE TRIGGER trg_reserva_transicion
    BEFORE UPDATE OF estado ON reserva
    FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_reserva();

-- -----------------------------------------------------------------------------
-- 4. Transiciones válidas de un pago y coherencia del reembolso.
--    Tabla: pago. Evento: BEFORE UPDATE (todas las columnas, para que no se
--    pueda tocar el reembolso sin pasar por aquí).
--    PagoService solo hace APROBADO -> REEMBOLSADO. PENDIENTE existe en el enum
--    aunque hoy no se usa: solo puede resolverse a un resultado de la pasarela.
--    Los datos del reembolso solo existen en un pago REEMBOLSADO y, una vez
--    registrados, no cambian.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_transicion_pago() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado <> OLD.estado
       AND NOT ((OLD.estado = 'PENDIENTE' AND NEW.estado IN ('APROBADO', 'RECHAZADO', 'ERROR'))
             OR (OLD.estado = 'APROBADO'  AND NEW.estado = 'REEMBOLSADO')) THEN
        RAISE EXCEPTION 'El pago % no puede pasar de % a %', OLD.id_pago, OLD.estado, NEW.estado
            USING ERRCODE = 'check_violation';
    END IF;

    IF NEW.estado <> 'REEMBOLSADO'
       AND (NEW.valor_reembolsado IS NOT NULL OR NEW.referencia_reembolso IS NOT NULL) THEN
        RAISE EXCEPTION 'El pago % no está REEMBOLSADO y no puede tener datos de reembolso', OLD.id_pago
            USING ERRCODE = 'check_violation';
    END IF;

    IF OLD.estado = 'REEMBOLSADO'
       AND (NEW.valor_reembolsado IS DISTINCT FROM OLD.valor_reembolsado
            OR NEW.referencia_reembolso IS DISTINCT FROM OLD.referencia_reembolso) THEN
        RAISE EXCEPTION 'El reembolso del pago % ya se registró y no se puede modificar', OLD.id_pago
            USING ERRCODE = 'restrict_violation';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_pago_transicion ON pago;
CREATE TRIGGER trg_pago_transicion
    BEFORE UPDATE ON pago
    FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_pago();

-- -----------------------------------------------------------------------------
-- 5. Una reseña solo sobre una estancia completada (CA-016).
--    Tabla: resena. Evento: BEFORE INSERT.
--    Depende de otra tabla, así que no puede ser un CHECK. Se admite un día de
--    margen porque el backend usa la fecha de su zona horaria y CURRENT_DATE la de
--    la base (ver 08_produccion.sql, que las iguala).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_resena() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_estado VARCHAR(20);
    v_salida DATE;
BEGIN
    SELECT estado, fecha_salida INTO v_estado, v_salida
      FROM reserva WHERE id_reserva = NEW.id_reserva;
    -- Si la reserva no existe, responde la FK con su propio error.
    IF FOUND AND (v_estado <> 'CONFIRMADA' OR v_salida > CURRENT_DATE + 1) THEN
        RAISE EXCEPTION 'La reserva % no es una estancia completada', NEW.id_reserva
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_resena_estancia_completada ON resena;
CREATE TRIGGER trg_resena_estancia_completada
    BEFORE INSERT ON resena
    FOR EACH ROW EXECUTE FUNCTION fn_validar_resena();

-- -----------------------------------------------------------------------------
-- 6. Una cancelación siempre deja la reserva CANCELADA.
--    Tabla: cancelacion. Evento: AFTER INSERT, diferido hasta el COMMIT.
--    Diferido porque el orden varía: CancelacionService inserta la cancelación
--    antes de que Hibernate envíe el UPDATE de la reserva, y sp_cancelar_reserva
--    lo hace al revés. Al confirmar la transacción ambos ya ocurrieron.
--    Para comprobarlo antes: SET CONSTRAINTS trg_cancelacion_reserva_cancelada IMMEDIATE.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_verificar_reserva_cancelada() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM reserva WHERE id_reserva = NEW.id_reserva AND estado = 'CANCELADA') THEN
        RAISE EXCEPTION 'Se registró la cancelación % pero la reserva % no quedó CANCELADA',
            NEW.id_cancelacion, NEW.id_reserva USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_cancelacion_reserva_cancelada ON cancelacion;
CREATE CONSTRAINT TRIGGER trg_cancelacion_reserva_cancelada
    AFTER INSERT ON cancelacion
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_verificar_reserva_cancelada();
