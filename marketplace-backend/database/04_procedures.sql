-- =============================================================================
-- Marketplace - 04_procedures.sql
-- Operaciones de mantenimiento para ejecutar desde psql u otra consola SQL. El
-- backend NO las llama: sus flujos siguen en los servicios de Spring.
--
-- Transacciones: un CALL es atómico. Si algo falla se revierte todo. Ninguno hace
-- COMMIT, así que también se pueden usar dentro de un BEGIN ... COMMIT.
--
-- Importante: sp_cancelar_reserva y sp_registrar_devolucion_simulada rechazan los
-- pagos de Stripe. Una devolución real exige llamar a Stripe, y eso solo puede
-- hacerlo el backend (POST /reservas/{id}/cancelar).
-- =============================================================================
SET client_encoding = 'UTF8';

-- -----------------------------------------------------------------------------
-- Devolución simulada del último pago aprobado de una reserva.
-- Uso: CALL sp_registrar_devolucion_simulada(4, 414000);
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_registrar_devolucion_simulada(p_id_reserva BIGINT, p_valor NUMERIC)
LANGUAGE plpgsql AS $$
DECLARE
    v_pago pago%ROWTYPE;
BEGIN
    IF p_valor IS NULL OR p_valor <= 0 THEN
        RAISE EXCEPTION 'El valor de la devolución debe ser positivo' USING ERRCODE = 'check_violation';
    END IF;

    SELECT * INTO v_pago FROM pago
     WHERE id_reserva = p_id_reserva
     ORDER BY fecha DESC, id_pago DESC
     LIMIT 1
     FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La reserva % no tiene pagos', p_id_reserva USING ERRCODE = 'no_data_found';
    END IF;
    IF v_pago.estado <> 'APROBADO' THEN
        RAISE EXCEPTION 'El pago % está en estado % y no se puede devolver', v_pago.id_pago, v_pago.estado
            USING ERRCODE = 'check_violation';
    END IF;
    IF v_pago.proveedor <> 'SIMULADO' THEN
        RAISE EXCEPTION 'El pago % es de %: la devolución debe hacerse desde el backend',
            v_pago.id_pago, v_pago.proveedor USING ERRCODE = 'check_violation';
    END IF;
    IF p_valor > v_pago.valor THEN
        RAISE EXCEPTION 'La devolución (%) supera el valor pagado (%)', p_valor, v_pago.valor
            USING ERRCODE = 'check_violation';
    END IF;

    UPDATE pago
       SET estado = 'REEMBOLSADO',
           valor_reembolsado = p_valor,
           referencia_reembolso = 'sim_re_' || gen_random_uuid(),
           detalle = 'Devolución simulada de ' || p_valor || '.'
     WHERE id_pago = v_pago.id_pago;
END;
$$;

-- -----------------------------------------------------------------------------
-- Cancela una reserva con las mismas reglas de CancelacionService.cancelar.
-- La puede pedir el huésped de la reserva o un administrador.
-- Uso: CALL sp_cancelar_reserva(3, 4, 'Cambio de planes');
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_cancelar_reserva(p_id_reserva BIGINT, p_id_actor BIGINT, p_motivo VARCHAR)
LANGUAGE plpgsql AS $$
DECLARE
    v_reserva    reserva%ROWTYPE;
    v_politica   politica_cancelacion%ROWTYPE;
    v_pago       pago%ROWTYPE;
    v_anfitrion  BIGINT;
    v_rol        VARCHAR(20);
    v_dias       INTEGER;
    v_porcentaje INTEGER;
    v_devolucion NUMERIC(18, 2);
BEGIN
    IF p_motivo IS NULL OR LENGTH(TRIM(p_motivo)) = 0 THEN
        RAISE EXCEPTION 'El motivo de la cancelación es obligatorio' USING ERRCODE = 'check_violation';
    END IF;

    -- El bloqueo evita que dos cancelaciones simultáneas calculen la misma devolución.
    SELECT * INTO v_reserva FROM reserva WHERE id_reserva = p_id_reserva FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La reserva % no existe', p_id_reserva USING ERRCODE = 'no_data_found';
    END IF;

    SELECT rol INTO v_rol FROM usuario WHERE id_usuario = p_id_actor;
    IF v_rol IS NULL OR (v_reserva.id_huesped <> p_id_actor AND v_rol <> 'ADMIN') THEN
        RAISE EXCEPTION 'El usuario % no puede cancelar la reserva %', p_id_actor, p_id_reserva
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF v_reserva.estado NOT IN ('CONFIRMADA', 'EN_REVISION') THEN
        RAISE EXCEPTION 'La reserva no se puede cancelar en estado %', v_reserva.estado
            USING ERRCODE = 'check_violation';
    END IF;
    IF v_reserva.fecha_entrada <= CURRENT_DATE THEN
        RAISE EXCEPTION 'Solo se pueden cancelar reservas futuras' USING ERRCODE = 'check_violation';
    END IF;

    SELECT id_anfitrion INTO v_anfitrion FROM alojamiento WHERE id_alojamiento = v_reserva.id_alojamiento;
    SELECT p.* INTO v_politica
      FROM alojamiento a JOIN politica_cancelacion p ON p.id_politica = a.id_politica
     WHERE a.id_alojamiento = v_reserva.id_alojamiento;
    IF NOT FOUND THEN
        SELECT * INTO v_politica FROM politica_cancelacion WHERE nombre = 'FLEXIBLE';
    END IF;

    v_dias := v_reserva.fecha_entrada - CURRENT_DATE;
    v_porcentaje := CASE
        WHEN v_politica.dias_reembolso_total IS NOT NULL AND v_dias >= v_politica.dias_reembolso_total THEN 100
        WHEN v_politica.dias_reembolso_parcial IS NOT NULL AND v_dias >= v_politica.dias_reembolso_parcial
            THEN COALESCE(v_politica.porcentaje_parcial, 0)
        ELSE 0
    END;
    v_devolucion := ROUND(v_reserva.total * v_porcentaje / 100.0, 2);

    SELECT * INTO v_pago FROM pago
     WHERE id_reserva = p_id_reserva
     ORDER BY fecha DESC, id_pago DESC
     LIMIT 1;
    IF FOUND AND v_pago.estado = 'APROBADO' AND v_pago.proveedor <> 'SIMULADO' THEN
        RAISE EXCEPTION 'La reserva se pagó con %: cancélala desde la API para devolver el dinero', v_pago.proveedor
            USING ERRCODE = 'check_violation';
    END IF;

    UPDATE reserva SET estado = 'CANCELADA' WHERE id_reserva = p_id_reserva;

    INSERT INTO cancelacion (motivo, politica, porcentaje_reembolso, valor_reembolso, estado,
                             estado_final_reserva, fecha, id_usuario, id_reserva)
    VALUES (TRIM(p_motivo), v_politica.nombre, v_porcentaje, v_devolucion, 'COMPLETADA',
            'CANCELADA', LOCALTIMESTAMP, p_id_actor, p_id_reserva);

    -- v_pago queda en NULL si la reserva no tiene pagos, y entonces no hay nada que devolver.
    IF v_devolucion > 0 AND v_pago.estado = 'APROBADO' THEN
        CALL sp_registrar_devolucion_simulada(p_id_reserva, v_devolucion);
    END IF;

    INSERT INTO notificacion (tipo, mensaje, fecha, leida, id_usuario)
    VALUES ('RESERVA_CANCELADA', 'Se canceló la reserva ' || p_id_reserva || '. Devolución: ' || v_devolucion || '.',
            LOCALTIMESTAMP, FALSE, v_reserva.id_huesped),
           ('RESERVA_CANCELADA', 'La reserva ' || p_id_reserva || ' fue cancelada.',
            LOCALTIMESTAMP, FALSE, v_anfitrion);

    INSERT INTO evento_auditoria (accion, fecha, detalle, id_usuario, entidad, id_entidad, resultado)
    VALUES ('CANCELACION', LOCALTIMESTAMP,
            v_politica.nombre || ' ' || v_porcentaje || ' %, devolución ' || v_devolucion || ' (por SQL)',
            p_id_actor, 'RESERVA', p_id_reserva, 'EXITOSO');
END;
$$;

-- -----------------------------------------------------------------------------
-- Pone una reserva en revisión o resuelve una revisión. Solo administradores.
-- Para cancelar se usa sp_cancelar_reserva, que además calcula la devolución.
-- Uso: CALL sp_actualizar_estado_reserva(5, 'CONFIRMADA', 1, 'Revisión resuelta');
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_actualizar_estado_reserva(p_id_reserva BIGINT, p_estado VARCHAR,
                                                         p_id_admin BIGINT, p_motivo VARCHAR)
LANGUAGE plpgsql AS $$
DECLARE
    v_anterior VARCHAR(20);
BEGIN
    IF p_estado NOT IN ('CONFIRMADA', 'EN_REVISION') THEN
        RAISE EXCEPTION 'Este procedimiento solo pasa a CONFIRMADA o EN_REVISION (recibido: %)', p_estado
            USING ERRCODE = 'check_violation';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM usuario WHERE id_usuario = p_id_admin AND rol = 'ADMIN') THEN
        RAISE EXCEPTION 'El usuario % no es administrador', p_id_admin USING ERRCODE = 'insufficient_privilege';
    END IF;

    SELECT estado INTO v_anterior FROM reserva WHERE id_reserva = p_id_reserva FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La reserva % no existe', p_id_reserva USING ERRCODE = 'no_data_found';
    END IF;

    BEGIN
        -- trg_reserva_transicion valida la transición y ex_reserva_sin_cruces las fechas.
        UPDATE reserva SET estado = p_estado WHERE id_reserva = p_id_reserva;
    EXCEPTION WHEN exclusion_violation THEN
        RAISE EXCEPTION 'Las fechas de la reserva % se cruzan con otra reserva activa', p_id_reserva
            USING ERRCODE = 'exclusion_violation';
    END;

    INSERT INTO evento_auditoria (accion, fecha, detalle, id_usuario, entidad, id_entidad, resultado)
    VALUES ('RESERVA_ESTADO', LOCALTIMESTAMP,
            v_anterior || ' -> ' || p_estado || COALESCE('. ' || p_motivo, '') || ' (por SQL)',
            p_id_admin, 'RESERVA', p_id_reserva, 'EXITOSO');
END;
$$;

-- -----------------------------------------------------------------------------
-- Agrega un periodo de disponibilidad. Lo puede hacer el anfitrión o un administrador.
-- Uso: CALL sp_agregar_disponibilidad(1, DATE '2027-01-10', DATE '2027-02-10', 2);
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_agregar_disponibilidad(p_id_alojamiento BIGINT, p_inicio DATE, p_fin DATE,
                                                      p_id_actor BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_anfitrion BIGINT;
    v_rol       VARCHAR(20);
    v_periodo   BIGINT;
BEGIN
    IF p_inicio IS NULL OR p_fin IS NULL OR p_fin <= p_inicio THEN
        RAISE EXCEPTION 'La fecha final debe ser posterior a la inicial' USING ERRCODE = 'check_violation';
    END IF;
    IF p_fin < CURRENT_DATE THEN
        RAISE EXCEPTION 'El periodo no puede estar completamente en el pasado' USING ERRCODE = 'check_violation';
    END IF;

    SELECT id_anfitrion INTO v_anfitrion FROM alojamiento WHERE id_alojamiento = p_id_alojamiento FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El alojamiento % no existe', p_id_alojamiento USING ERRCODE = 'no_data_found';
    END IF;
    SELECT rol INTO v_rol FROM usuario WHERE id_usuario = p_id_actor;
    IF v_rol IS NULL OR (v_anfitrion <> p_id_actor AND v_rol <> 'ADMIN') THEN
        RAISE EXCEPTION 'El usuario % no puede modificar este alojamiento', p_id_actor
            USING ERRCODE = 'insufficient_privilege';
    END IF;

    BEGIN
        INSERT INTO periodo_disponibilidad (id_alojamiento, fecha_inicio, fecha_fin)
        VALUES (p_id_alojamiento, p_inicio, p_fin)
        RETURNING id_periodo INTO v_periodo;
    EXCEPTION WHEN exclusion_violation THEN
        RAISE EXCEPTION 'El periodo se cruza con otro del mismo alojamiento' USING ERRCODE = 'exclusion_violation';
    END;

    INSERT INTO evento_auditoria (accion, fecha, detalle, id_usuario, entidad, id_entidad, resultado)
    VALUES ('DISPONIBILIDAD', LOCALTIMESTAMP, p_inicio || ' a ' || p_fin || ' (por SQL)',
            p_id_actor, 'PERIODO_DISPONIBILIDAD', v_periodo, 'EXITOSO');
END;
$$;

-- -----------------------------------------------------------------------------
-- Borra los registros de idempotencia antiguos: el backend no los limpia nunca y un
-- reintento solo ocurre poco después de la petición original. Conserva los EN_PROCESO.
-- Uso: CALL sp_purgar_idempotencia(7, NULL);
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_purgar_idempotencia(p_dias INTEGER, INOUT p_borrados INTEGER DEFAULT NULL)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_dias IS NULL OR p_dias < 1 THEN
        RAISE EXCEPTION 'Debes conservar al menos un día de registros' USING ERRCODE = 'check_violation';
    END IF;
    DELETE FROM registro_idempotencia
     WHERE estado = 'COMPLETADA'
       AND fecha_creacion < LOCALTIMESTAMP - make_interval(days => p_dias);
    GET DIAGNOSTICS p_borrados = ROW_COUNT;
END;
$$;

-- -----------------------------------------------------------------------------
-- Reservas de un usuario, como huésped o como anfitrión. Es una función porque un
-- procedimiento no devuelve filas.
-- Uso: SELECT * FROM fn_reservas_usuario(4);
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reservas_usuario(p_id_usuario BIGINT, p_limite INTEGER DEFAULT 50,
                                               p_desplazamiento INTEGER DEFAULT 0)
RETURNS TABLE (id_reserva BIGINT, papel TEXT, alojamiento VARCHAR, ciudad VARCHAR, fecha_entrada DATE,
               fecha_salida DATE, estado VARCHAR, total NUMERIC, estado_pago VARCHAR)
LANGUAGE sql STABLE AS $$
    SELECT r.id_reserva,
           CASE WHEN r.id_huesped = p_id_usuario THEN 'HUESPED' ELSE 'ANFITRION' END,
           a.titulo, a.ciudad, r.fecha_entrada, r.fecha_salida, r.estado, r.total,
           (SELECT p.estado FROM pago p WHERE p.id_reserva = r.id_reserva
             ORDER BY p.fecha DESC, p.id_pago DESC LIMIT 1)
      FROM reserva r
      JOIN alojamiento a ON a.id_alojamiento = r.id_alojamiento
     WHERE r.id_huesped = p_id_usuario OR a.id_anfitrion = p_id_usuario
     ORDER BY r.fecha_creacion DESC, r.id_reserva DESC
     LIMIT p_limite OFFSET p_desplazamiento;
$$;
