-- =============================================================================
-- Marketplace - 03_triggers.sql
-- Solo reglas que una restricción declarativa no puede expresar. Ninguno modifica
-- filas (no hay recursividad posible) y ninguno cambia lo que Hibernate cree que
-- escribió: o dejan pasar la operación o la rechazan.
--
-- No se crearon, a propósito:
--   * fecha_actualizacion: la asigna PublicacionService.marcarActualizado.
--   * historial y auditoría automáticos: los escriben PublicacionService y
--     AuditoriaService con actor y motivo, que un trigger no conoce.
--   * "reserva cancelada al insertar en cancelacion": Hibernate ejecuta los INSERT
--     antes que los UPDATE al sincronizar, así que CancelacionService fallaría.
-- =============================================================================
SET client_encoding = 'UTF8';

-- -----------------------------------------------------------------------------
-- 1. Auditoría e historial inmutables (CA-150).
--    Tablas: evento_auditoria, historial_estado_alojamiento.
--    Eventos: UPDATE, DELETE y TRUNCATE. @Immutable solo protege dentro de
--    Hibernate; esto también protege frente a SQL directo.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_rechazar_cambio_inmutable() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de % no se pueden modificar ni borrar', TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER trg_evento_inmutable
    BEFORE UPDATE OR DELETE ON evento_auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_cambio_inmutable();
CREATE TRIGGER trg_evento_sin_truncate
    BEFORE TRUNCATE ON evento_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_rechazar_cambio_inmutable();

CREATE TRIGGER trg_historial_inmutable
    BEFORE UPDATE OR DELETE ON historial_estado_alojamiento
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_cambio_inmutable();
CREATE TRIGGER trg_historial_sin_truncate
    BEFORE TRUNCATE ON historial_estado_alojamiento
    FOR EACH STATEMENT EXECUTE FUNCTION fn_rechazar_cambio_inmutable();

-- -----------------------------------------------------------------------------
-- 2. Transiciones válidas de una reserva.
--    Tabla: reserva. Evento: BEFORE UPDATE OF estado.
--    Un CHECK solo ve la fila nueva, no el estado anterior. Las transiciones salen
--    de ReservaService, CancelacionService y PublicacionService.bloquear; la
--    EN_REVISION -> CONFIRMADA la usa sp_actualizar_estado_reserva al resolver una
--    revisión. Hibernate reenvía el mismo estado al guardar: eso siempre se permite.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_transicion_reserva() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;
    IF (OLD.estado = 'PENDIENTE_PAGO' AND NEW.estado IN ('CONFIRMADA', 'EN_REVISION', 'CANCELADA'))
        OR (OLD.estado = 'CONFIRMADA' AND NEW.estado IN ('EN_REVISION', 'CANCELADA'))
        OR (OLD.estado = 'EN_REVISION' AND NEW.estado IN ('CONFIRMADA', 'CANCELADA')) THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'La reserva % no puede pasar de % a %', OLD.id_reserva, OLD.estado, NEW.estado
        USING ERRCODE = 'check_violation';
END;
$$;

CREATE TRIGGER trg_reserva_transicion
    BEFORE UPDATE OF estado ON reserva
    FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_reserva();

-- -----------------------------------------------------------------------------
-- 3. Transiciones válidas de un pago.
--    Tabla: pago. Evento: BEFORE UPDATE OF estado.
--    Evita, por ejemplo, reembolsar un pago rechazado. PagoService solo hace
--    APROBADO -> REEMBOLSADO; PENDIENTE existe en el enum aunque hoy no se usa.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_transicion_pago() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;
    IF OLD.estado = 'PENDIENTE' OR (OLD.estado = 'APROBADO' AND NEW.estado = 'REEMBOLSADO') THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'El pago % no puede pasar de % a %', OLD.id_pago, OLD.estado, NEW.estado
        USING ERRCODE = 'check_violation';
END;
$$;

CREATE TRIGGER trg_pago_transicion
    BEFORE UPDATE OF estado ON pago
    FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_pago();

-- -----------------------------------------------------------------------------
-- 4. Una reseña solo sobre una estancia completada (CA-016).
--    Tabla: resena. Evento: BEFORE INSERT.
--    Depende de otra tabla, así que no puede ser un CHECK. Se admite un día de
--    margen porque el backend usa la fecha de su zona horaria y CURRENT_DATE la de
--    la base (por ejemplo, Colombia frente a UTC).
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

CREATE TRIGGER trg_resena_estancia_completada
    BEFORE INSERT ON resena
    FOR EACH ROW EXECUTE FUNCTION fn_validar_resena();
