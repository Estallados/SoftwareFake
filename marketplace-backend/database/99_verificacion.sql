-- =============================================================================
-- Marketplace - 99_verificacion.sql
-- Comprueba en PostgreSQL lo que solo se puede probar ahí: triggers, procedimientos,
-- restricciones EXCLUDE y los datos de prueba. Se ejecuta al final, después de
-- 05_seed_data.sql. Termina en ROLLBACK: no deja ningún cambio en la base.
-- Si algo falla, se detiene y muestra el nombre de la comprobación.
--   psql -v ON_ERROR_STOP=1 -d marketplace -f 99_verificacion.sql
-- =============================================================================
SET client_encoding = 'UTF8';
BEGIN;

DO $$
DECLARE
    v_total INTEGER;
    v_texto TEXT;
BEGIN
    -- Cada prueba negativa va en su propio bloque BEGIN/EXCEPTION: si la sentencia
    -- falla con el error esperado, se deshace solo ese bloque y se continúa.

    -- ---- Estructura ---------------------------------------------------------
    SELECT COUNT(*) INTO v_total FROM information_schema.tables
     WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
    IF v_total <> 20 THEN RAISE EXCEPTION 'tablas: se esperaban 20 y hay %', v_total; END IF;

    SELECT COUNT(*) INTO v_total FROM pg_constraint WHERE contype = 'x';
    IF v_total <> 2 THEN RAISE EXCEPTION 'EXCLUDE: se esperaban 2 y hay %', v_total; END IF;

    SELECT COUNT(*) INTO v_total FROM pg_trigger WHERE NOT tgisinternal;
    IF v_total <> 12 THEN RAISE EXCEPTION 'triggers: se esperaban 12 y hay %', v_total; END IF;

    -- Índices duplicados: dos índices de una tabla con exactamente las mismas columnas y condición.
    SELECT COUNT(*) INTO v_total FROM (
        SELECT indrelid, indkey::text, COALESCE(pg_get_expr(indexprs, indrelid), ''),
               COALESCE(pg_get_expr(indpred, indrelid), '')
          FROM pg_index i JOIN pg_class c ON c.oid = i.indrelid JOIN pg_namespace n ON n.oid = c.relnamespace
         WHERE n.nspname = 'public'
         GROUP BY 1, 2, 3, 4 HAVING COUNT(*) > 1) d;
    IF v_total <> 0 THEN RAISE EXCEPTION 'hay % índices duplicados', v_total; END IF;

    -- ---- Datos de prueba ----------------------------------------------------
    SELECT COUNT(*) INTO v_total FROM cotizacion
     WHERE total <> precio_base - descuento + valor_limpieza + valor_servicio + valor_adicionales;
    IF v_total <> 0 THEN RAISE EXCEPTION 'cotizaciones con total incoherente: %', v_total; END IF;

    SELECT COUNT(*) INTO v_total FROM reserva r JOIN cotizacion c ON c.id_cotizacion = r.id_cotizacion
     WHERE r.total <> c.total OR r.fecha_entrada <> c.fecha_entrada OR r.fecha_salida <> c.fecha_salida
        OR r.id_huesped <> c.id_usuario OR r.id_alojamiento <> c.id_alojamiento;
    IF v_total <> 0 THEN RAISE EXCEPTION 'reservas distintas de su cotización: %', v_total; END IF;

    SELECT COUNT(*) INTO v_total FROM reserva r
     WHERE r.estado IN ('CONFIRMADA', 'EN_REVISION')
       AND NOT EXISTS (SELECT 1 FROM pago p WHERE p.id_reserva = r.id_reserva AND p.estado = 'APROBADO');
    IF v_total <> 0 THEN RAISE EXCEPTION 'reservas confirmadas sin pago aprobado: %', v_total; END IF;

    -- ---- Restricciones ------------------------------------------------------
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, estado, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 31, CURRENT_DATE + 33, 2, 1, 'CONFIRMADA', 1, 5, 3, 8);
        RAISE EXCEPTION 'FALLO: se aceptó una reserva superpuesta';
    EXCEPTION WHEN exclusion_violation THEN NULL;
    END;

    -- Fechas contiguas (salir el día que otro entra) sí se permiten.
    INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, estado, total, id_huesped, id_alojamiento, id_cotizacion)
    VALUES (CURRENT_DATE + 34, CURRENT_DATE + 36, 2, 1, 'CONFIRMADA', 1, 5, 3, 8);

    BEGIN
        INSERT INTO periodo_disponibilidad (id_alojamiento, fecha_inicio, fecha_fin)
        VALUES (1, CURRENT_DATE, CURRENT_DATE + 5);
        RAISE EXCEPTION 'FALLO: se aceptó un periodo superpuesto';
    EXCEPTION WHEN exclusion_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO resena (calificacion, id_usuario, id_reserva, id_alojamiento) VALUES (5, 5, 3, 3);
        RAISE EXCEPTION 'FALLO: se aceptó una reseña con un autor distinto al huésped';
    EXCEPTION WHEN foreign_key_violation OR check_violation THEN NULL;
    END;

    BEGIN
        UPDATE alojamiento SET capacidad = NULL WHERE id_alojamiento = 1;
        RAISE EXCEPTION 'FALLO: se aceptó un alojamiento activo sin capacidad';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    -- ---- Triggers -----------------------------------------------------------
    BEGIN
        UPDATE evento_auditoria SET detalle = 'x' WHERE id_evento = 1;
        RAISE EXCEPTION 'FALLO: se modificó un evento de auditoría';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        DELETE FROM historial_estado_alojamiento WHERE id_alojamiento = 1;
        RAISE EXCEPTION 'FALLO: se borró historial';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        UPDATE reserva SET estado = 'CONFIRMADA' WHERE id_reserva = 4;
        RAISE EXCEPTION 'FALLO: una reserva cancelada volvió a confirmarse';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        UPDATE pago SET estado = 'APROBADO' WHERE id_pago = 6;
        RAISE EXCEPTION 'FALLO: un pago rechazado pasó a aprobado';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO resena (calificacion, id_usuario, id_reserva, id_alojamiento) VALUES (5, 4, 3, 3);
        RAISE EXCEPTION 'FALLO: se aceptó una reseña de una estancia futura';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    -- ---- Triggers de validación (03b_triggers_validacion.sql) ------------------
    -- raise_exception es el código de un RAISE EXCEPTION sin ERRCODE (P0001).
    BEGIN
        INSERT INTO cotizacion (fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, porcentaje_limpieza,
                                valor_limpieza, porcentaje_servicio, valor_servicio, total, id_usuario, id_alojamiento, fecha_expiracion)
        VALUES (CURRENT_DATE + 90, CURRENT_DATE + 91, 1, 1, 1, 1, 0, 0, 0, 0, 1, 4, 7, LOCALTIMESTAMP + INTERVAL '1' DAY);
        RAISE EXCEPTION 'FALLO: se cotizó un alojamiento bloqueado' USING ERRCODE = 'data_exception';
    EXCEPTION WHEN raise_exception THEN NULL;
    END;
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, estado, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 60, CURRENT_DATE + 63, 3, 1, 'PENDIENTE_PAGO', 1, 3, 3, 8);
        RAISE EXCEPTION 'FALLO: un anfitrión reservó su propio alojamiento' USING ERRCODE = 'data_exception';
    EXCEPTION WHEN raise_exception THEN NULL;
    END;
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, estado, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 60, CURRENT_DATE + 63, 3, 1, 'PENDIENTE_PAGO', 1, 4, 3, 8);
        RAISE EXCEPTION 'FALLO: se usó la cotización de otro huésped' USING ERRCODE = 'data_exception';
    EXCEPTION WHEN raise_exception THEN NULL;
    END;
    BEGIN
        INSERT INTO mensaje (contenido, id_conversacion, id_usuario) VALUES ('Hola', 1, 5);
        RAISE EXCEPTION 'FALLO: escribió un usuario ajeno a la conversación' USING ERRCODE = 'data_exception';
    EXCEPTION WHEN raise_exception THEN NULL;
    END;
    BEGIN
        INSERT INTO pago (proveedor, valor, estado, id_reserva) VALUES ('OTRO', 1, 'RECHAZADO', 1);
        RAISE EXCEPTION 'FALLO: se aceptó un proveedor inválido' USING ERRCODE = 'data_exception';
    EXCEPTION WHEN raise_exception THEN NULL;
    END;

    -- Los cálculos: el pago toma el total de la reserva, aunque se envíe otro valor.
    INSERT INTO pago (proveedor, valor, estado, id_reserva) VALUES ('SIMULADO', 1, 'RECHAZADO', 1);
    PERFORM 1 FROM pago WHERE id_reserva = 1 AND estado = 'RECHAZADO' AND valor = 621000.00;
    IF NOT FOUND THEN RAISE EXCEPTION 'trg_validar_pago no calculó el valor'; END IF;

    -- Hibernate reenvía el mismo estado al guardar: no debe fallar.
    UPDATE reserva SET estado = 'CONFIRMADA' WHERE id_reserva = 3;

    -- ---- Procedimientos -----------------------------------------------------
    CALL sp_cancelar_reserva(3, 4, 'Prueba de verificación');   -- ESTRICTA, 30 días: 50 %
    SELECT estado INTO v_texto FROM reserva WHERE id_reserva = 3;
    IF v_texto <> 'CANCELADA' THEN RAISE EXCEPTION 'sp_cancelar_reserva no canceló'; END IF;
    PERFORM 1 FROM cancelacion WHERE id_reserva = 3 AND porcentaje_reembolso = 50 AND valor_reembolso = 966000.00;
    IF NOT FOUND THEN RAISE EXCEPTION 'sp_cancelar_reserva calculó mal la devolución'; END IF;
    PERFORM 1 FROM pago WHERE id_reserva = 3 AND estado = 'REEMBOLSADO' AND valor_reembolsado = 966000.00;
    IF NOT FOUND THEN RAISE EXCEPTION 'sp_cancelar_reserva no registró la devolución'; END IF;

    BEGIN
        CALL sp_cancelar_reserva(3, 4, 'Segunda vez');
        RAISE EXCEPTION 'FALLO: se canceló dos veces';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        CALL sp_cancelar_reserva(5, 5, 'No es mi reserva');
        RAISE EXCEPTION 'FALLO: otro huésped canceló una reserva ajena';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;

    CALL sp_actualizar_estado_reserva(5, 'CONFIRMADA', 1, 'Revisión resuelta');
    SELECT estado INTO v_texto FROM reserva WHERE id_reserva = 5;
    IF v_texto <> 'CONFIRMADA' THEN RAISE EXCEPTION 'sp_actualizar_estado_reserva no cambió el estado'; END IF;

    CALL sp_agregar_disponibilidad(4, CURRENT_DATE + 200, CURRENT_DATE + 230, 3);
    PERFORM 1 FROM periodo_disponibilidad WHERE id_alojamiento = 4 AND fecha_inicio = CURRENT_DATE + 200;
    IF NOT FOUND THEN RAISE EXCEPTION 'sp_agregar_disponibilidad no agregó el periodo'; END IF;
    BEGIN
        CALL sp_agregar_disponibilidad(4, CURRENT_DATE + 210, CURRENT_DATE + 215, 3);
        RAISE EXCEPTION 'FALLO: sp_agregar_disponibilidad aceptó un cruce';
    EXCEPTION WHEN exclusion_violation THEN NULL;
    END;

    SELECT COUNT(*) INTO v_total FROM fn_reservas_usuario(4);
    IF v_total = 0 THEN RAISE EXCEPTION 'fn_reservas_usuario no devolvió filas'; END IF;

    v_total := NULL;
    CALL sp_purgar_idempotencia(1, v_total);
    IF v_total <> 1 THEN RAISE EXCEPTION 'sp_purgar_idempotencia borró % filas en lugar de 1', v_total; END IF;

    RAISE NOTICE 'VERIFICACION CORRECTA: estructura, datos, triggers y procedimientos';
END;
$$;

ROLLBACK;
