-- =============================================================================
-- Marketplace - 99_verificacion.sql
-- Comprueba en PostgreSQL lo que solo se puede probar ahí: triggers, procedimientos,
-- restricciones EXCLUDE y los datos de prueba. Se ejecuta al final, después de
-- 05_seed_data.sql. Termina en ROLLBACK: no deja ningún cambio en la base.
-- Si algo falla, se detiene y muestra el nombre de la comprobación.
--   psql -v ON_ERROR_STOP=1 -d marketplace -f 99_verificacion.sql
--
-- Depende de los ids de 05_seed_data.sql: úsalo en una base de prueba, NO en
-- producción.
-- =============================================================================
SET client_encoding = 'UTF8';
BEGIN;

DO $$
DECLARE
    v_total INTEGER;
    v_texto TEXT;
    v_cot   BIGINT;
    v_res   BIGINT;
    v_cot_c BIGINT;
    v_res_c BIGINT;
BEGIN
    -- Cada prueba negativa va en su propio bloque BEGIN/EXCEPTION: si la sentencia
    -- falla con el error esperado, se deshace solo ese bloque y se continúa. El
    -- RAISE 'FALLO' usa el código P0001, que ningún bloque captura.

    -- ---- Estructura ---------------------------------------------------------
    SELECT COUNT(*) INTO v_total FROM information_schema.tables
     WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
    IF v_total <> 20 THEN RAISE EXCEPTION 'tablas: se esperaban 20 y hay %', v_total; END IF;

    SELECT COUNT(*) INTO v_total FROM pg_constraint WHERE contype = 'x';
    IF v_total <> 2 THEN RAISE EXCEPTION 'EXCLUDE: se esperaban 2 y hay %', v_total; END IF;

    -- 15 de 03_triggers.sql y 5 de 03b_triggers_validacion.sql.
    SELECT COUNT(*) INTO v_total FROM pg_trigger WHERE NOT tgisinternal;
    IF v_total <> 20 THEN RAISE EXCEPTION 'triggers: se esperaban 20 y hay %', v_total; END IF;

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
     WHERE r.estado = 'CONFIRMADA'
       AND NOT EXISTS (SELECT 1 FROM pago p WHERE p.id_reserva = r.id_reserva AND p.estado = 'APROBADO');
    IF v_total <> 0 THEN RAISE EXCEPTION 'reservas confirmadas sin pago aprobado: %', v_total; END IF;

    -- ---- Flujo normal del backend y EXCLUDE de reservas -----------------------
    -- Alojamiento 3: 420000 por noche, tarifa global 5 % y 10 %. Huésped 5 (Diego).
    -- Dos noches: base 840000, limpieza 42000, servicio 84000, total 966000.

    -- A: se cruza con la reserva 3 (días 30 a 34): no se puede confirmar.
    INSERT INTO cotizacion (fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, porcentaje_limpieza,
                            valor_limpieza, porcentaje_servicio, valor_servicio, total, id_usuario, id_alojamiento, fecha_expiracion)
    VALUES (CURRENT_DATE + 31, CURRENT_DATE + 33, 1, 2, 420000, 840000, 5, 42000, 10, 84000, 966000, 5, 3,
            LOCALTIMESTAMP + INTERVAL '30' MINUTE)
    RETURNING id_cotizacion INTO v_cot;
    INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, total, id_huesped, id_alojamiento, id_cotizacion)
    VALUES (CURRENT_DATE + 31, CURRENT_DATE + 33, 2, 1, 966000, 5, 3, v_cot)
    RETURNING id_reserva INTO v_res;
    UPDATE cotizacion SET id_reserva = v_res WHERE id_cotizacion = v_cot;   -- como ReservaService
    INSERT INTO pago (proveedor, valor, estado, metodo, id_reserva) VALUES ('SIMULADO', 966000, 'APROBADO', 'pm_card_visa', v_res);
    BEGIN
        UPDATE reserva SET estado = 'CONFIRMADA' WHERE id_reserva = v_res;
        RAISE EXCEPTION 'FALLO: se confirmó una reserva superpuesta';
    EXCEPTION WHEN exclusion_violation THEN NULL;
    END;

    -- B: fechas contiguas (salir el día que otro entra) sí se permiten.
    INSERT INTO cotizacion (fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, porcentaje_limpieza,
                            valor_limpieza, porcentaje_servicio, valor_servicio, total, id_usuario, id_alojamiento, fecha_expiracion)
    VALUES (CURRENT_DATE + 34, CURRENT_DATE + 36, 1, 2, 420000, 840000, 5, 42000, 10, 84000, 966000, 5, 3,
            LOCALTIMESTAMP + INTERVAL '30' MINUTE)
    RETURNING id_cotizacion INTO v_cot;
    INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, total, id_huesped, id_alojamiento, id_cotizacion)
    VALUES (CURRENT_DATE + 34, CURRENT_DATE + 36, 2, 1, 966000, 5, 3, v_cot)
    RETURNING id_reserva INTO v_res;
    INSERT INTO pago (proveedor, valor, estado, metodo, id_reserva) VALUES ('SIMULADO', 966000, 'APROBADO', 'pm_card_visa', v_res);
    UPDATE reserva SET estado = 'CONFIRMADA' WHERE id_reserva = v_res;

    -- C: sin pago aprobado no se confirma.
    INSERT INTO cotizacion (fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, porcentaje_limpieza,
                            valor_limpieza, porcentaje_servicio, valor_servicio, total, id_usuario, id_alojamiento, fecha_expiracion)
    VALUES (CURRENT_DATE + 45, CURRENT_DATE + 47, 1, 2, 420000, 840000, 5, 42000, 10, 84000, 966000, 5, 3,
            LOCALTIMESTAMP + INTERVAL '30' MINUTE)
    RETURNING id_cotizacion INTO v_cot_c;
    INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, total, id_huesped, id_alojamiento, id_cotizacion)
    VALUES (CURRENT_DATE + 45, CURRENT_DATE + 47, 2, 1, 966000, 5, 3, v_cot_c)
    RETURNING id_reserva INTO v_res_c;
    BEGIN
        UPDATE reserva SET estado = 'CONFIRMADA' WHERE id_reserva = v_res_c;
        RAISE EXCEPTION 'FALLO: se confirmó una reserva sin pago aprobado';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    -- ---- Restricciones ------------------------------------------------------
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

    -- ---- Inmutabilidad (03_triggers.sql, secciones 1 y 2) ---------------------
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
        UPDATE mensaje SET contenido = 'editado' WHERE id_mensaje = 1;
        RAISE EXCEPTION 'FALLO: se editó un mensaje';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        UPDATE reserva SET total = 1 WHERE id_reserva = 1;
        RAISE EXCEPTION 'FALLO: se cambió el total de una reserva';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        UPDATE cotizacion SET precio_noche = 1 WHERE id_cotizacion = 1;
        RAISE EXCEPTION 'FALLO: se cambió el precio de una cotización';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        UPDATE pago SET valor = 1 WHERE id_pago = 1;
        RAISE EXCEPTION 'FALLO: se cambió el valor de un pago';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        UPDATE resena SET calificacion = 1 WHERE id_resena = 1;
        RAISE EXCEPTION 'FALLO: se cambió la calificación de una reseña';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        DELETE FROM pago WHERE id_pago = 6;
        RAISE EXCEPTION 'FALLO: se borró un pago';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        DELETE FROM reserva WHERE id_reserva = 7;
        RAISE EXCEPTION 'FALLO: se borró una reserva';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;

    -- Lo que el backend sí cambia sigue permitido.
    UPDATE resena SET estado = 'OCULTA' WHERE id_resena = 1;          -- ResenaService.moderar
    UPDATE cotizacion SET id_reserva = v_res_c WHERE id_cotizacion = v_cot_c;
    UPDATE reserva SET estado = 'CONFIRMADA' WHERE id_reserva = 3;     -- Hibernate reenvía el mismo estado

    -- ---- Transiciones (03_triggers.sql, secciones 3 y 4) ----------------------
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
        UPDATE pago SET valor_reembolsado = 1 WHERE id_pago = 1;
        RAISE EXCEPTION 'FALLO: un pago aprobado recibió datos de reembolso';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        UPDATE pago SET valor_reembolsado = 1 WHERE id_pago = 4;
        RAISE EXCEPTION 'FALLO: se modificó un reembolso ya registrado';
    EXCEPTION WHEN restrict_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO resena (calificacion, id_usuario, id_reserva, id_alojamiento) VALUES (5, 4, 3, 3);
        RAISE EXCEPTION 'FALLO: se aceptó una reseña de una estancia futura';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    -- ---- Validaciones al insertar (03b_triggers_validacion.sql) ---------------
    BEGIN
        INSERT INTO cotizacion (fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, porcentaje_limpieza,
                                valor_limpieza, porcentaje_servicio, valor_servicio, total, id_usuario, id_alojamiento, fecha_expiracion)
        VALUES (CURRENT_DATE + 90, CURRENT_DATE + 91, 1, 1, 200000, 200000, 5, 10000, 10, 20000, 230000, 4, 7,
                LOCALTIMESTAMP + INTERVAL '1' DAY);
        RAISE EXCEPTION 'FALLO: se cotizó un alojamiento bloqueado';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        -- Cálculo coherente, pero con un precio que no es el del alojamiento 1 (180000).
        INSERT INTO cotizacion (fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, porcentaje_limpieza,
                                valor_limpieza, porcentaje_servicio, valor_servicio, total, id_usuario, id_alojamiento, fecha_expiracion)
        VALUES (CURRENT_DATE + 90, CURRENT_DATE + 91, 1, 1, 100, 100, 5, 5, 10, 10, 115, 4, 1,
                LOCALTIMESTAMP + INTERVAL '1' DAY);
        RAISE EXCEPTION 'FALLO: se cotizó con un precio distinto del vigente';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO cotizacion (fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, porcentaje_limpieza,
                                valor_limpieza, porcentaje_servicio, valor_servicio, total, id_usuario, id_alojamiento, fecha_expiracion)
        VALUES (CURRENT_DATE + 90, CURRENT_DATE + 92, 1, 2, 180000, 180000, 5, 9000, 10, 18000, 207000, 4, 1,
                LOCALTIMESTAMP + INTERVAL '1' DAY);
        RAISE EXCEPTION 'FALLO: se aceptó un precio base que no es precio por noches';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 60, CURRENT_DATE + 63, 3, 2, 1449000, 3, 3, 8);
        RAISE EXCEPTION 'FALLO: un anfitrión reservó su propio alojamiento';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 60, CURRENT_DATE + 63, 3, 2, 1449000, 4, 3, 8);
        RAISE EXCEPTION 'FALLO: se usó la cotización de otro huésped';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 60, CURRENT_DATE + 63, 3, 2, 1, 5, 3, 8);
        RAISE EXCEPTION 'FALLO: se aceptó una reserva con un total distinto al de su cotización';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, estado, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 60, CURRENT_DATE + 63, 3, 2, 'CONFIRMADA', 1449000, 5, 3, 8);
        RAISE EXCEPTION 'FALLO: se insertó una reserva ya confirmada sin pasar por el pago';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO mensaje (contenido, id_conversacion, id_usuario) VALUES ('Hola', 1, 5);
        RAISE EXCEPTION 'FALLO: escribió un usuario ajeno a la conversación';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO pago (proveedor, valor, estado, id_reserva) VALUES ('OTRO', 621000, 'RECHAZADO', 1);
        RAISE EXCEPTION 'FALLO: se aceptó un proveedor inválido';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO pago (proveedor, valor, estado, id_reserva) VALUES ('SIMULADO', 1, 'RECHAZADO', 1);
        RAISE EXCEPTION 'FALLO: se aceptó un pago por un valor distinto del total';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO pago (proveedor, valor, estado, valor_reembolsado, referencia_reembolso, id_reserva)
        VALUES ('SIMULADO', 621000, 'REEMBOLSADO', 621000, 'sim_re_x', 1);
        RAISE EXCEPTION 'FALLO: se insertó un pago ya reembolsado';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    INSERT INTO pago (proveedor, valor, estado, id_reserva) VALUES ('SIMULADO', 621000, 'RECHAZADO', 1);
    BEGIN
        INSERT INTO cancelacion (motivo, politica, porcentaje_reembolso, valor_reembolso, estado, estado_final_reserva, id_usuario, id_reserva)
        VALUES ('x', 'FLEXIBLE', 50, 1, 'COMPLETADA', 'CANCELADA', 4, 1);
        RAISE EXCEPTION 'FALLO: se aceptó una devolución mal calculada';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

    -- Modo carga: omite las reglas del estado vigente, no la coherencia.
    PERFORM set_config('marketplace.carga_datos', 'on', true);
    INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, estado, total, id_huesped, id_alojamiento, id_cotizacion)
    VALUES (CURRENT_DATE + 45, CURRENT_DATE + 47, 2, 1, 'CANCELADA', 966000, 5, 3, v_cot_c);
    BEGIN
        INSERT INTO reserva (fecha_entrada, fecha_salida, noches, huespedes, estado, total, id_huesped, id_alojamiento, id_cotizacion)
        VALUES (CURRENT_DATE + 45, CURRENT_DATE + 47, 2, 1, 'CANCELADA', 1, 5, 3, v_cot_c);
        RAISE EXCEPTION 'FALLO: el modo carga aceptó una reserva distinta de su cotización';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    PERFORM set_config('marketplace.carga_datos', 'off', true);

    -- ---- Procedimientos -----------------------------------------------------
    BEGIN
        CALL sp_registrar_devolucion_simulada(1, 1000);
        RAISE EXCEPTION 'FALLO: se devolvió dinero de una reserva vigente';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

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

    -- El trigger diferido se comprueba al COMMIT; aquí se adelanta. Primero valida
    -- la cancelación de sp_cancelar_reserva (debe pasar) y luego una sin cancelar la reserva.
    SET CONSTRAINTS trg_cancelacion_reserva_cancelada IMMEDIATE;
    BEGIN
        INSERT INTO cancelacion (motivo, politica, porcentaje_reembolso, valor_reembolso, estado, estado_final_reserva, id_usuario, id_reserva)
        VALUES ('Sin cancelar la reserva', 'MODERADA', 0, 0, 'COMPLETADA', 'CANCELADA', 4, 5);
        RAISE EXCEPTION 'FALLO: se registró una cancelación sin cancelar la reserva';
    EXCEPTION WHEN check_violation THEN NULL;
    END;

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
