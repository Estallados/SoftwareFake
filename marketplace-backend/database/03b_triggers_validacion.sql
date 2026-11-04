-- =============================================================================
-- Marketplace - 03b_triggers_validacion.sql
-- Validaciones y cálculos al insertar. Se ejecuta después de 03_triggers.sql y
-- antes de 05_seed_data.sql. También se puede ejecutar sobre una base que ya tiene
-- datos: solo actúa sobre los INSERT nuevos y se puede repetir sin problema.
--
-- Son una segunda barrera: el backend ya valida todo esto y responde con mensajes
-- propios, así que estos errores solo deberían verse con SQL directo. Los valores
-- que calculan coinciden con los que envía el backend, por lo que lo que Hibernate
-- tiene en memoria sigue siendo correcto.
-- =============================================================================
SET client_encoding = 'UTF8';

-- TRIGGER 5
-- Validar las cotizaciones y calcular automaticamente las noches y el total

DROP TRIGGER IF EXISTS trg_validar_cotizacion ON cotizacion;

CREATE OR REPLACE FUNCTION fn_validar_cotizacion()
RETURNS TRIGGER AS $$
DECLARE
    v_estado_alojamiento VARCHAR(20);
    v_capacidad INTEGER;
    v_precio_noche NUMERIC(12,2);
BEGIN
    -- Obtener informacion del alojamiento
    SELECT estado, capacidad, precio_noche
    INTO v_estado_alojamiento, v_capacidad, v_precio_noche
    FROM alojamiento
    WHERE id_alojamiento = NEW.id_alojamiento;

    IF v_estado_alojamiento IS NULL THEN
        RAISE EXCEPTION 'El alojamiento % no existe', NEW.id_alojamiento;
    END IF;

    IF v_estado_alojamiento <> 'ACTIVA' THEN
        RAISE EXCEPTION 'No se puede cotizar un alojamiento en estado %', v_estado_alojamiento;
    END IF;

    IF NEW.huespedes > v_capacidad THEN
        RAISE EXCEPTION 'La cantidad de huespedes (%) supera la capacidad del alojamiento (%)',
            NEW.huespedes, v_capacidad;
    END IF;

    IF NEW.fecha_salida <= NEW.fecha_entrada THEN
        RAISE EXCEPTION 'La fecha de salida debe ser posterior a la fecha de entrada';
    END IF;

    -- Calcular las noches, el precio vigente y el total
    NEW.noches := NEW.fecha_salida - NEW.fecha_entrada;
    NEW.precio_noche := v_precio_noche;
    NEW.total := NEW.precio_base - NEW.descuento + NEW.valor_limpieza
                 + NEW.valor_servicio + NEW.valor_adicionales;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_cotizacion
    BEFORE INSERT ON cotizacion
    FOR EACH ROW EXECUTE FUNCTION fn_validar_cotizacion();


-- TRIGGER 6
-- Validar las reservas nuevas y copiar automaticamente el total de la cotizacion

DROP TRIGGER IF EXISTS trg_validar_reserva ON reserva;

CREATE OR REPLACE FUNCTION fn_validar_reserva()
RETURNS TRIGGER AS $$
DECLARE
    v_estado_alojamiento VARCHAR(20);
    v_anfitrion BIGINT;
    v_rol VARCHAR(20);
    v_estado_usuario VARCHAR(20);
    v_cotizacion cotizacion%ROWTYPE;
BEGIN
    -- Las noches siempre se calculan a partir de las fechas
    NEW.noches := NEW.fecha_salida - NEW.fecha_entrada;

    -- El backend siempre crea las reservas en PENDIENTE_PAGO. Las que entran en otro
    -- estado son cargas de datos: su alojamiento o su cotizacion pudieron cambiar despues.
    IF NEW.estado <> 'PENDIENTE_PAGO' THEN
        RETURN NEW;
    END IF;

    -- Obtener informacion del alojamiento
    SELECT estado, id_anfitrion
    INTO v_estado_alojamiento, v_anfitrion
    FROM alojamiento
    WHERE id_alojamiento = NEW.id_alojamiento;

    IF v_estado_alojamiento IS NULL THEN
        RAISE EXCEPTION 'El alojamiento % no existe', NEW.id_alojamiento;
    END IF;

    IF v_estado_alojamiento <> 'ACTIVA' THEN
        RAISE EXCEPTION 'No se puede reservar un alojamiento en estado %', v_estado_alojamiento;
    END IF;

    IF v_anfitrion = NEW.id_huesped THEN
        RAISE EXCEPTION 'Un anfitrion no puede reservar su propio alojamiento';
    END IF;

    -- Obtener informacion del huesped
    SELECT rol, estado
    INTO v_rol, v_estado_usuario
    FROM usuario
    WHERE id_usuario = NEW.id_huesped;

    IF v_rol IS NULL THEN
        RAISE EXCEPTION 'El usuario % no existe', NEW.id_huesped;
    END IF;

    IF v_rol <> 'HUESPED' THEN
        RAISE EXCEPTION 'Solo un usuario con rol HUESPED puede crear reservas';
    END IF;

    IF v_estado_usuario <> 'ACTIVO' THEN
        RAISE EXCEPTION 'El usuario se encuentra en estado %', v_estado_usuario;
    END IF;

    -- Obtener la cotizacion
    SELECT *
    INTO v_cotizacion
    FROM cotizacion
    WHERE id_cotizacion = NEW.id_cotizacion;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cotizacion % no existe', NEW.id_cotizacion;
    END IF;

    IF v_cotizacion.id_usuario <> NEW.id_huesped
        OR v_cotizacion.id_alojamiento <> NEW.id_alojamiento THEN
        RAISE EXCEPTION 'La cotizacion % no corresponde a este huesped y alojamiento', NEW.id_cotizacion;
    END IF;

    IF v_cotizacion.id_reserva IS NOT NULL THEN
        RAISE EXCEPTION 'La cotizacion % ya fue usada', NEW.id_cotizacion;
    END IF;

    -- Copiar los valores congelados de la cotizacion
    NEW.fecha_entrada := v_cotizacion.fecha_entrada;
    NEW.fecha_salida := v_cotizacion.fecha_salida;
    NEW.noches := v_cotizacion.noches;
    NEW.total := v_cotizacion.total;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_reserva
    BEFORE INSERT ON reserva
    FOR EACH ROW EXECUTE FUNCTION fn_validar_reserva();


-- TRIGGER 7
-- Validar los pagos y calcular automaticamente el valor a partir de la reserva
-- No se valida el estado de la reserva: si un pago tarda, la reserva pudo cancelarse
-- mientras tanto, y un cobro ya realizado debe quedar registrado de todas formas.

DROP TRIGGER IF EXISTS trg_validar_pago ON pago;

CREATE OR REPLACE FUNCTION fn_validar_pago()
RETURNS TRIGGER AS $$
DECLARE
    v_total NUMERIC(18,2);
BEGIN
    -- Obtener el total de la reserva
    SELECT total
    INTO v_total
    FROM reserva
    WHERE id_reserva = NEW.id_reserva;

    IF v_total IS NULL THEN
        RAISE EXCEPTION 'La reserva % no existe', NEW.id_reserva;
    END IF;

    IF NEW.proveedor NOT IN ('STRIPE_TEST', 'SIMULADO') THEN
        RAISE EXCEPTION 'El proveedor de pago % no es valido', NEW.proveedor;
    END IF;

    -- El valor del pago siempre es el total de la reserva
    NEW.valor := v_total;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_pago
    BEFORE INSERT ON pago
    FOR EACH ROW EXECUTE FUNCTION fn_validar_pago();


-- TRIGGER 8
-- Validar las cancelaciones y calcular automaticamente el valor de la devolucion
-- Se acepta la reserva aun CONFIRMADA: Hibernate inserta la cancelacion antes de
-- enviar el UPDATE que deja la reserva CANCELADA.

DROP TRIGGER IF EXISTS trg_validar_cancelacion ON cancelacion;

CREATE OR REPLACE FUNCTION fn_validar_cancelacion()
RETURNS TRIGGER AS $$
DECLARE
    v_estado_reserva VARCHAR(20);
    v_total NUMERIC(18,2);
    v_huesped BIGINT;
    v_rol VARCHAR(20);
BEGIN
    -- Obtener informacion de la reserva
    SELECT estado, total, id_huesped
    INTO v_estado_reserva, v_total, v_huesped
    FROM reserva
    WHERE id_reserva = NEW.id_reserva;

    IF v_estado_reserva IS NULL THEN
        RAISE EXCEPTION 'La reserva % no existe', NEW.id_reserva;
    END IF;

    IF v_estado_reserva = 'PENDIENTE_PAGO' THEN
        RAISE EXCEPTION 'No se puede cancelar una reserva pendiente de pago';
    END IF;

    -- Validar quien cancela
    SELECT rol
    INTO v_rol
    FROM usuario
    WHERE id_usuario = NEW.id_usuario;

    IF v_rol IS NULL THEN
        RAISE EXCEPTION 'El usuario % no existe', NEW.id_usuario;
    END IF;

    IF NEW.id_usuario <> v_huesped AND v_rol <> 'ADMIN' THEN
        RAISE EXCEPTION 'Solo el huesped de la reserva o un administrador pueden cancelarla';
    END IF;

    -- Calcular la devolucion con el mismo redondeo de CancelacionService
    NEW.valor_reembolso := ROUND(v_total * NEW.porcentaje_reembolso / 100.0, 2);
    NEW.estado_final_reserva := 'CANCELADA';

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_cancelacion
    BEFORE INSERT ON cancelacion
    FOR EACH ROW EXECUTE FUNCTION fn_validar_cancelacion();


-- TRIGGER 9
-- Validar que quien envia un mensaje participe en la conversacion
-- No se rechazan las conversaciones CERRADAS porque el backend hoy no lo hace.

DROP TRIGGER IF EXISTS trg_validar_mensaje ON mensaje;

CREATE OR REPLACE FUNCTION fn_validar_mensaje()
RETURNS TRIGGER AS $$
DECLARE
    v_huesped BIGINT;
    v_anfitrion BIGINT;
BEGIN
    -- Obtener los participantes de la conversacion
    SELECT id_usuario, id_anfitrion
    INTO v_huesped, v_anfitrion
    FROM conversacion
    WHERE id_conversacion = NEW.id_conversacion;

    IF v_huesped IS NULL THEN
        RAISE EXCEPTION 'La conversacion % no existe', NEW.id_conversacion;
    END IF;

    IF NEW.id_usuario NOT IN (v_huesped, v_anfitrion) THEN
        RAISE EXCEPTION 'El usuario % no participa en la conversacion %', NEW.id_usuario, NEW.id_conversacion;
    END IF;

    -- Guardar el contenido sin espacios sobrantes
    NEW.contenido := TRIM(NEW.contenido);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_mensaje
    BEFORE INSERT ON mensaje
    FOR EACH ROW EXECUTE FUNCTION fn_validar_mensaje();
