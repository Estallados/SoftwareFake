-- =============================================================================
-- Marketplace - 05_seed_data.sql
-- Datos de prueba coherentes. Las fechas son relativas a CURRENT_DATE, así las
-- reservas "futuras" siguen siéndolo y las cotizaciones vigentes siguen vigentes
-- sin importar cuándo se ejecute. Todo o nada: corre en una sola transacción.
--
-- CREDENCIALES
--   Contraseña de todos los usuarios: Demo#2026 (hash BCrypt, como SecurityConfig).
--   El correo va cifrado y correo_hash se deriva de AES_SECRET_KEY. Estos valores
--   se generaron con AESUtil y una clave SOLO PARA DESARROLLO:
--       AES_SECRET_KEY=bWFya2V0cGxhY2Utc2VtaWxsYS1kZW1vLTIwMjYhISE=
--   Si usas otra clave, ejecuta herramientas/RecifrarUsuariosSemilla.java después
--   de este script (ver 07_documentacion_bd.md); si no, nadie podrá iniciar sesión.
--
-- IMÁGENES
--   No hay tabla de imágenes: viven en Cloudinary. Súbelas por la API
--   (POST /alojamientos/{id}/imagenes). Hasta entonces los alojamientos se ven sin foto.
-- =============================================================================
SET client_encoding = 'UTF8';
BEGIN;

-- ---- Usuarios ---------------------------------------------------------------
INSERT INTO usuario (id_usuario, nombre, correo, correo_hash, telefono, rol, estado, contrasenia,
                     motivo_restriccion, totp_habilitado, fecha_registro, correo_verificado) VALUES
(1, 'Administrador',  'l6HZFFb3xRX/GxeUrb3Bz+KvbhI1Uotoq0zYh673esMLT4PKiLzq0fMH7EbhTd/aLcxE',      'Zq9j5CrjFmGz85AO0o2LXX6k4CJ6vxs+LjkgtoPq3CA=',      NULL,         'ADMIN',     'ACTIVO',      '$2a$10$odaYIXaqmyE0x2QXWJIj6eDMn5hT1UHJ3t7oKtnei70pI21wBxLsO', NULL, FALSE, LOCALTIMESTAMP - INTERVAL '200' DAY, TRUE),
(2, 'Laura Gómez',    'mxZcnpF3dAI1s2ey64jmKuvzfbua5MDmxJfRSZoxOs2zrzaQobzYy1AzRvFs1+zsv5o/Q8weCwo=', 'Sf30em+lkvl0juaAOIhA4wN4GgcNSC+u8sNS5eAo4Bo=', '3001112233', 'ANFITRION', 'ACTIVO',      '$2a$10$odaYIXaqmyE0x2QXWJIj6eDMn5hT1UHJ3t7oKtnei70pI21wBxLsO', NULL, FALSE, LOCALTIMESTAMP - INTERVAL '180' DAY, TRUE),
(3, 'Carlos Ruiz',    'jHwjm8Z87nGxTFWA7M417cWqx5kHQONnks0TOfuVCmkobiZD2rzYbfkDXSA3/p7boQO65ipmnug=', 'dPXyRMN5DOLY+KnhBMCJGv9EOaxfa71sSG8vAvNkCmg=', '3002223344', 'ANFITRION', 'ACTIVO',      '$2a$10$odaYIXaqmyE0x2QXWJIj6eDMn5hT1UHJ3t7oKtnei70pI21wBxLsO', NULL, FALSE, LOCALTIMESTAMP - INTERVAL '170' DAY, TRUE),
(4, 'Ana Torres',     '05rFL3qHZgN7tdNxzbOliN8KdKddmD6zuCWI4ZZ0EpYvRI8l3yXAwbW86rP35n1N3aSKXn9hFA==',  'U1b0x0xAMukL9UWGIbjoOIuJfBMhbbZGP5CgFmHXxbE=',  '3003334455', 'HUESPED',   'ACTIVO',      '$2a$10$odaYIXaqmyE0x2QXWJIj6eDMn5hT1UHJ3t7oKtnei70pI21wBxLsO', NULL, FALSE, LOCALTIMESTAMP - INTERVAL '120' DAY, TRUE),
(5, 'Diego Mora',     'XRu5bA/pqFDPjzIiTwoxqhNA1AcMrbGn0VniNx6Tx0SgzNx1dPvGQxJkY3sWy8rqHHvUghyhew==',  'nHkvjC/HP+TAl3Bedpkos7qhLyetd8Z4bY4S76KfseE=',  '3004445566', 'HUESPED',   'ACTIVO',      '$2a$10$odaYIXaqmyE0x2QXWJIj6eDMn5hT1UHJ3t7oKtnei70pI21wBxLsO', NULL, FALSE, LOCALTIMESTAMP - INTERVAL '100' DAY, TRUE),
(6, 'Sofía Pérez',    't6GM20jraAKYmVSo5u6Qf7cJ8q+8kbRoeCec8carDPZXk2Q59yFkB7MsHhvk0+jV2FPxWX/MY7M=', 'gmjOcWxzliHBE3AOT47B3ZiE/iZcdIUPawWDCiv3e0o=', NULL,         'HUESPED',   'RESTRINGIDO', '$2a$10$odaYIXaqmyE0x2QXWJIj6eDMn5hT1UHJ3t7oKtnei70pI21wBxLsO', 'Incumplimiento de las normas de la plataforma.', FALSE, LOCALTIMESTAMP - INTERVAL '90' DAY, TRUE),
-- Correo sin verificar: sirve para probar el rechazo del inicio de sesión.
(7, 'Pedro Díaz',     'vSeGg8pqiPTE76FSldVYXtp3TKHeUyO7QqfyVTAvll622n8MxbCrPyNK2CK1bpZp945XGopo+Q==',  'bR90VzUiXbGv1v0zRnjeT/nGDJUg5XY6dOaILvBzWuY=',  NULL,         'HUESPED',   'ACTIVO',      '$2a$10$odaYIXaqmyE0x2QXWJIj6eDMn5hT1UHJ3t7oKtnei70pI21wBxLsO', NULL, FALSE, LOCALTIMESTAMP - INTERVAL '5' DAY, FALSE);

-- ---- Políticas: los mismos textos de PoliticasInicializador, para que no cree otras ----
INSERT INTO politica_cancelacion (id_politica, nombre, condiciones, dias_reembolso_total, dias_reembolso_parcial, porcentaje_parcial) VALUES
(1, 'FLEXIBLE', 'Devolución del 100 % si cancelas con al menos 1 día de anticipación.', 1, NULL, NULL),
(2, 'MODERADA', 'Devolución del 100 % con 5 o más días de anticipación y del 50 % con 1 a 4 días.', 5, 1, 50),
(3, 'ESTRICTA', 'Devolución del 50 % con 7 o más días de anticipación; después no hay devolución.', NULL, 7, 50);

INSERT INTO configuracion_tarifa (id_configuracion, porcentaje_limpieza, porcentaje_servicio, id_usuario, fecha) VALUES
(1, 5.00, 10.00, 1, LOCALTIMESTAMP - INTERVAL '150' DAY);

-- ---- Alojamientos: uno por cada estado posible y varias ciudades ----------------
INSERT INTO alojamiento (id_alojamiento, titulo, descripcion, ubicacion, ciudad, latitud, longitud, tipo, capacidad,
                         habitaciones, camas, banos, hora_entrada, hora_salida, precio_noche, reglas, estado,
                         id_anfitrion, id_politica, fecha_creacion, fecha_actualizacion) VALUES
(1, 'Apartamento moderno en Chapinero', 'Apartamento luminoso cerca de restaurantes y transporte.', 'Calle 63 # 9-20', 'Bogotá', 4.6486, -74.0628, 'Apartamento', 3, 1, 2, 1, TIME '15:00', TIME '11:00', 180000.00, 'No fumar. Sin fiestas.', 'ACTIVA', 2, 1, LOCALTIMESTAMP - INTERVAL '150' DAY, LOCALTIMESTAMP - INTERVAL '20' DAY),
(2, 'Casa con vista en El Poblado', 'Casa amplia con terraza y vista a la ciudad.', 'Carrera 37 # 8-50', 'Medellín', 6.2086, -75.5659, 'Casa', 6, 3, 4, 2, TIME '14:00', TIME '12:00', 350000.00, 'Se admiten mascotas pequeñas.', 'ACTIVA', 2, 2, LOCALTIMESTAMP - INTERVAL '140' DAY, LOCALTIMESTAMP - INTERVAL '30' DAY),
(3, 'Apartamento frente al mar', 'A pasos de la playa, con balcón y piscina.', 'Avenida San Martín # 5-10', 'Cartagena', 10.3997, -75.5144, 'Apartamento', 4, 2, 2, 2, TIME '15:00', TIME '11:00', 420000.00, 'No fumar. Silencio desde las 10 p. m.', 'ACTIVA', 3, 3, LOCALTIMESTAMP - INTERVAL '130' DAY, LOCALTIMESTAMP - INTERVAL '10' DAY),
(4, 'Cabaña en Pance', 'Cabaña rodeada de naturaleza.', 'Vía Pance km 12', 'Cali', 3.3350, -76.5900, 'Cabaña', 5, 2, 3, 1, TIME '13:00', TIME '11:00', 260000.00, 'Respetar el entorno natural.', 'PAUSADA', 3, 1, LOCALTIMESTAMP - INTERVAL '110' DAY, LOCALTIMESTAMP - INTERVAL '15' DAY),
-- Borrador incompleto: el modelo permite estos NULL hasta que se publique.
(5, 'Borrador en Santa Marta', NULL, NULL, 'Santa Marta', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'BORRADOR', 2, NULL, LOCALTIMESTAMP - INTERVAL '3' DAY, LOCALTIMESTAMP - INTERVAL '3' DAY),
(6, 'Estudio en Usaquén', 'Estudio acogedor para estancias cortas.', 'Carrera 7 # 120-30', 'Bogotá', 4.6950, -74.0310, 'Estudio', 2, 0, 1, 1, TIME '15:00', TIME '10:00', 140000.00, 'No fumar.', 'PENDIENTE_REVISION', 3, 1, LOCALTIMESTAMP - INTERVAL '8' DAY, LOCALTIMESTAMP - INTERVAL '2' DAY),
(7, 'Finca cafetera en Manizales', 'Finca tradicional con recorrido por el cafetal.', 'Vereda La Linda', 'Manizales', 5.0689, -75.5174, 'Finca', 8, 4, 6, 3, TIME '14:00', TIME '12:00', 200000.00, 'Respetar los horarios de la finca.', 'ACTIVA', 3, 2, LOCALTIMESTAMP - INTERVAL '100' DAY, LOCALTIMESTAMP - INTERVAL '4' DAY);

-- Minúsculas: la búsqueda por amenidad compara en minúsculas.
INSERT INTO alojamiento_amenidad (id_alojamiento, amenidad) VALUES
(1, 'wifi'), (1, 'cocina'), (1, 'lavadora'),
(2, 'wifi'), (2, 'parqueadero'), (2, 'terraza'), (2, 'mascotas'),
(3, 'wifi'), (3, 'piscina'), (3, 'aire acondicionado'),
(4, 'chimenea'), (4, 'parqueadero'),
(6, 'wifi'),
(7, 'wifi'), (7, 'parqueadero'), (7, 'cocina');

INSERT INTO servicio_adicional (id_servicio, nombre, costo, id_alojamiento) VALUES
(1, 'Desayuno', 30000.00, 1),
(2, 'Traslado desde el aeropuerto', 80000.00, 2),
(3, 'Chef privado', 200000.00, 2),
(4, 'Salida tardía', 50000.00, 3);

INSERT INTO periodo_disponibilidad (id_periodo, id_alojamiento, fecha_inicio, fecha_fin) VALUES
(1, 1, CURRENT_DATE - 90, CURRENT_DATE + 180),
(2, 2, CURRENT_DATE - 90, CURRENT_DATE + 180),
(3, 3, CURRENT_DATE - 60, CURRENT_DATE + 180),
(4, 4, CURRENT_DATE + 10, CURRENT_DATE + 100),
(5, 6, CURRENT_DATE + 15, CURRENT_DATE + 120),
(6, 7, CURRENT_DATE - 30, CURRENT_DATE + 120);

INSERT INTO historial_estado_alojamiento (id_alojamiento, estado_anterior, estado_nuevo, motivo, id_usuario, fecha) VALUES
(1, NULL, 'BORRADOR', NULL, 2, LOCALTIMESTAMP - INTERVAL '150' DAY),
(1, 'BORRADOR', 'PENDIENTE_REVISION', NULL, 2, LOCALTIMESTAMP - INTERVAL '149' DAY),
(1, 'PENDIENTE_REVISION', 'ACTIVA', NULL, 1, LOCALTIMESTAMP - INTERVAL '148' DAY),
(2, NULL, 'BORRADOR', NULL, 2, LOCALTIMESTAMP - INTERVAL '140' DAY),
(2, 'BORRADOR', 'ACTIVA', NULL, 1, LOCALTIMESTAMP - INTERVAL '138' DAY),
(3, NULL, 'BORRADOR', NULL, 3, LOCALTIMESTAMP - INTERVAL '130' DAY),
(3, 'BORRADOR', 'ACTIVA', NULL, 1, LOCALTIMESTAMP - INTERVAL '128' DAY),
(4, NULL, 'BORRADOR', NULL, 3, LOCALTIMESTAMP - INTERVAL '110' DAY),
(4, 'BORRADOR', 'ACTIVA', NULL, 1, LOCALTIMESTAMP - INTERVAL '108' DAY),
(4, 'ACTIVA', 'PAUSADA', NULL, 3, LOCALTIMESTAMP - INTERVAL '15' DAY),
(5, NULL, 'BORRADOR', NULL, 2, LOCALTIMESTAMP - INTERVAL '3' DAY),
(6, NULL, 'BORRADOR', NULL, 3, LOCALTIMESTAMP - INTERVAL '8' DAY),
(6, 'BORRADOR', 'PENDIENTE_REVISION', NULL, 3, LOCALTIMESTAMP - INTERVAL '2' DAY),
(7, NULL, 'BORRADOR', NULL, 3, LOCALTIMESTAMP - INTERVAL '100' DAY),
(7, 'BORRADOR', 'ACTIVA', NULL, 1, LOCALTIMESTAMP - INTERVAL '98' DAY),
(7, 'ACTIVA', 'BLOQUEADA', 'Bloqueada: reporte de información incorrecta (1 reservas futuras en revisión)', 1, LOCALTIMESTAMP - INTERVAL '4' DAY);

-- ---- Cotizaciones ------------------------------------------------------------
-- Limpieza 5 % y servicio 10 % sobre el valor base, como CotizacionService.calcular.
-- id_reserva se completa después: cotizacion y reserva se referencian entre sí.
INSERT INTO cotizacion (id_cotizacion, fecha_entrada, fecha_salida, huespedes, noches, precio_noche, precio_base, descuento,
                        porcentaje_limpieza, valor_limpieza, porcentaje_servicio, valor_servicio, valor_adicionales, total,
                        desglose, id_usuario, id_alojamiento, fecha_creacion, fecha_expiracion) VALUES
(1, CURRENT_DATE - 30, CURRENT_DATE - 27, 2, 3, 180000.00,  540000.00, 0, 5.00, 27000.00, 10.00,  54000.00,     0.00,  621000.00, '[]', 4, 1, LOCALTIMESTAMP - INTERVAL '45' DAY, LOCALTIMESTAMP - INTERVAL '45' DAY + INTERVAL '30' MINUTE),
(2, CURRENT_DATE - 20, CURRENT_DATE - 15, 4, 5, 350000.00, 1750000.00, 0, 5.00, 87500.00, 10.00, 175000.00, 80000.00, 2092500.00, '[{"idServicio":2,"nombre":"Traslado desde el aeropuerto","costo":80000.00,"idAlojamiento":2}]', 5, 2, LOCALTIMESTAMP - INTERVAL '35' DAY, LOCALTIMESTAMP - INTERVAL '35' DAY + INTERVAL '30' MINUTE),
(3, CURRENT_DATE + 30, CURRENT_DATE + 34, 3, 4, 420000.00, 1680000.00, 0, 5.00, 84000.00, 10.00, 168000.00,     0.00, 1932000.00, '[]', 4, 3, LOCALTIMESTAMP - INTERVAL '6' DAY, LOCALTIMESTAMP - INTERVAL '6' DAY + INTERVAL '30' MINUTE),
(4, CURRENT_DATE + 10, CURRENT_DATE + 12, 2, 2, 180000.00,  360000.00, 0, 5.00, 18000.00, 10.00,  36000.00,     0.00,  414000.00, '[]', 5, 1, LOCALTIMESTAMP - INTERVAL '12' DAY, LOCALTIMESTAMP - INTERVAL '12' DAY + INTERVAL '30' MINUTE),
(5, CURRENT_DATE + 20, CURRENT_DATE + 23, 2, 3, 200000.00,  600000.00, 0, 5.00, 30000.00, 10.00,  60000.00,     0.00,  690000.00, '[]', 4, 7, LOCALTIMESTAMP - INTERVAL '9' DAY, LOCALTIMESTAMP - INTERVAL '9' DAY + INTERVAL '30' MINUTE),
(6, CURRENT_DATE + 40, CURRENT_DATE + 42, 2, 2, 350000.00,  700000.00, 0, 5.00, 35000.00, 10.00,  70000.00,     0.00,  805000.00, '[]', 5, 2, LOCALTIMESTAMP - INTERVAL '2' DAY, LOCALTIMESTAMP - INTERVAL '2' DAY + INTERVAL '30' MINUTE),
(7, CURRENT_DATE + 50, CURRENT_DATE + 52, 1, 2, 180000.00,  360000.00, 0, 5.00, 18000.00, 10.00,  36000.00,     0.00,  414000.00, '[]', 4, 1, LOCALTIMESTAMP - INTERVAL '5' MINUTE, LOCALTIMESTAMP + INTERVAL '25' MINUTE),
-- 8: sin usar, se puede reservar durante 7 días (POST /reservas con idCotizacion 8 como Diego).
(8, CURRENT_DATE + 60, CURRENT_DATE + 63, 2, 3, 420000.00, 1260000.00, 0, 5.00, 63000.00, 10.00, 126000.00,     0.00, 1449000.00, '[]', 5, 3, LOCALTIMESTAMP - INTERVAL '1' MINUTE, LOCALTIMESTAMP + INTERVAL '7' DAY),
-- 9: vencida, sirve para probar el rechazo.
(9, CURRENT_DATE + 70, CURRENT_DATE + 71, 1, 1, 180000.00,  180000.00, 0, 5.00,  9000.00, 10.00,  18000.00,     0.00,  207000.00, '[]', 4, 1, LOCALTIMESTAMP - INTERVAL '2' DAY, LOCALTIMESTAMP - INTERVAL '2' DAY + INTERVAL '30' MINUTE);

-- ---- Reservas: una por cada situación del negocio -------------------------------
INSERT INTO reserva (id_reserva, fecha_entrada, fecha_salida, noches, huespedes, estado, total, desglose,
                     id_huesped, id_alojamiento, id_cotizacion, fecha_creacion) VALUES
-- 1 y 2: estancias completadas y con reseña.
(1, CURRENT_DATE - 30, CURRENT_DATE - 27, 3, 2, 'CONFIRMADA',  621000.00, '{"idCotizacion":1,"noches":3,"precioBase":540000.00,"valorLimpieza":27000.00,"valorServicio":54000.00,"valorAdicionales":0.00,"total":621000.00}', 4, 1, 1, LOCALTIMESTAMP - INTERVAL '45' DAY),
(2, CURRENT_DATE - 20, CURRENT_DATE - 15, 5, 4, 'CONFIRMADA', 2092500.00, '{"idCotizacion":2,"noches":5,"precioBase":1750000.00,"valorLimpieza":87500.00,"valorServicio":175000.00,"valorAdicionales":80000.00,"total":2092500.00}', 5, 2, 2, LOCALTIMESTAMP - INTERVAL '35' DAY),
-- 3: futura y cancelable.
(3, CURRENT_DATE + 30, CURRENT_DATE + 34, 4, 3, 'CONFIRMADA', 1932000.00, '{"idCotizacion":3,"noches":4,"precioBase":1680000.00,"valorLimpieza":84000.00,"valorServicio":168000.00,"valorAdicionales":0.00,"total":1932000.00}', 4, 3, 3, LOCALTIMESTAMP - INTERVAL '6' DAY),
-- 4: cancelada con devolución del 100 % (FLEXIBLE).
(4, CURRENT_DATE + 10, CURRENT_DATE + 12, 2, 2, 'CANCELADA',   414000.00, '{"idCotizacion":4,"noches":2,"precioBase":360000.00,"valorLimpieza":18000.00,"valorServicio":36000.00,"valorAdicionales":0.00,"total":414000.00}', 5, 1, 4, LOCALTIMESTAMP - INTERVAL '12' DAY),
-- 5: en revisión porque se bloqueó el alojamiento.
(5, CURRENT_DATE + 20, CURRENT_DATE + 23, 3, 2, 'EN_REVISION', 690000.00, '{"idCotizacion":5,"noches":3,"precioBase":600000.00,"valorLimpieza":30000.00,"valorServicio":60000.00,"valorAdicionales":0.00,"total":690000.00}', 4, 7, 5, LOCALTIMESTAMP - INTERVAL '9' DAY),
-- 6: pago rechazado; la cotización queda libre (id_reserva NULL), como en ReservaService.cerrar.
(6, CURRENT_DATE + 40, CURRENT_DATE + 42, 2, 2, 'CANCELADA',   805000.00, '{"idCotizacion":6,"noches":2,"precioBase":700000.00,"valorLimpieza":35000.00,"valorServicio":70000.00,"valorAdicionales":0.00,"total":805000.00}', 5, 2, 6, LOCALTIMESTAMP - INTERVAL '2' DAY),
-- 7: pendiente de pago; el proceso programado del backend la cancelará a los 15 minutos.
(7, CURRENT_DATE + 50, CURRENT_DATE + 52, 2, 1, 'PENDIENTE_PAGO', 414000.00, '{"idCotizacion":7,"noches":2,"precioBase":360000.00,"valorLimpieza":18000.00,"valorServicio":36000.00,"valorAdicionales":0.00,"total":414000.00}', 4, 1, 7, LOCALTIMESTAMP - INTERVAL '4' MINUTE);

UPDATE cotizacion SET id_reserva = id_cotizacion WHERE id_cotizacion IN (1, 2, 3, 4, 5, 7);

INSERT INTO pago (id_pago, referencia, proveedor, valor, estado, detalle, fecha, metodo, valor_reembolsado, referencia_reembolso, id_reserva) VALUES
(1, 'sim_pi_0a000000-0000-4000-8000-000000000001', 'SIMULADO',  621000.00, 'APROBADO',    'Pago de prueba aprobado.', LOCALTIMESTAMP - INTERVAL '45' DAY, 'pm_card_visa', NULL, NULL, 1),
(2, 'sim_pi_0a000000-0000-4000-8000-000000000002', 'SIMULADO', 2092500.00, 'APROBADO',    'Pago de prueba aprobado.', LOCALTIMESTAMP - INTERVAL '35' DAY, 'pm_card_mastercard', NULL, NULL, 2),
(3, 'sim_pi_0a000000-0000-4000-8000-000000000003', 'SIMULADO', 1932000.00, 'APROBADO',    'Pago de prueba aprobado.', LOCALTIMESTAMP - INTERVAL '6' DAY,  'pm_card_visa', NULL, NULL, 3),
(4, 'sim_pi_0a000000-0000-4000-8000-000000000004', 'SIMULADO',  414000.00, 'REEMBOLSADO', 'Devolución simulada de 414000.00.', LOCALTIMESTAMP - INTERVAL '12' DAY, 'pm_card_visa', 414000.00, 'sim_re_0b000000-0000-4000-8000-000000000004', 4),
(5, 'sim_pi_0a000000-0000-4000-8000-000000000005', 'SIMULADO',  690000.00, 'APROBADO',    'Pago de prueba aprobado.', LOCALTIMESTAMP - INTERVAL '9' DAY,  'pm_card_visa', NULL, NULL, 5),
(6, 'sim_pi_0a000000-0000-4000-8000-000000000006', 'SIMULADO',  805000.00, 'RECHAZADO',   'La tarjeta fue rechazada.', LOCALTIMESTAMP - INTERVAL '2' DAY, 'pm_card_chargeDeclined', NULL, NULL, 6);

INSERT INTO cancelacion (id_cancelacion, motivo, politica, porcentaje_reembolso, valor_reembolso, estado, estado_final_reserva, fecha, id_usuario, id_reserva) VALUES
(1, 'Cambio de planes de viaje.', 'FLEXIBLE', 100, 414000.00, 'COMPLETADA', 'CANCELADA', LOCALTIMESTAMP - INTERVAL '10' DAY, 5, 4);

INSERT INTO resena (id_resena, calificacion, comentario, estado, fecha, id_usuario, id_reserva, id_alojamiento) VALUES
(1, 5, 'Excelente ubicación y muy limpio.', 'PUBLICADA', LOCALTIMESTAMP - INTERVAL '25' DAY, 4, 1, 1),
(2, 4, 'Muy buena casa, la vista es increíble.', 'PUBLICADA', LOCALTIMESTAMP - INTERVAL '14' DAY, 5, 2, 2);

INSERT INTO favorito (id_favorito, fecha_agregado, id_usuario, id_alojamiento) VALUES
(1, LOCALTIMESTAMP - INTERVAL '40' DAY, 4, 2),
(2, LOCALTIMESTAMP - INTERVAL '10' DAY, 4, 3),
(3, LOCALTIMESTAMP - INTERVAL '20' DAY, 5, 1);

-- ---- Mensajería y notificaciones ---------------------------------------------
INSERT INTO conversacion (id_conversacion, fecha_creacion, estado, id_usuario, id_anfitrion, id_alojamiento) VALUES
(1, LOCALTIMESTAMP - INTERVAL '46' DAY, 'ACTIVA',  4, 2, 1),
(2, LOCALTIMESTAMP - INTERVAL '36' DAY, 'ACTIVA',  5, 2, 2),
(3, LOCALTIMESTAMP - INTERVAL '7' DAY,  'CERRADA', 4, 3, 3);

INSERT INTO mensaje (id_mensaje, contenido, fecha_envio, id_conversacion, id_usuario) VALUES
(1, 'Hola, ¿el apartamento tiene lavadora?', LOCALTIMESTAMP - INTERVAL '46' DAY, 1, 4),
(2, 'Sí, y también cocina completa.', LOCALTIMESTAMP - INTERVAL '45' DAY, 1, 2),
(3, '¿Se admiten mascotas pequeñas?', LOCALTIMESTAMP - INTERVAL '36' DAY, 2, 5),
(4, 'Sí, sin problema.', LOCALTIMESTAMP - INTERVAL '36' DAY, 2, 2),
(5, '¿Hay parqueadero cerca?', LOCALTIMESTAMP - INTERVAL '7' DAY, 3, 4);

INSERT INTO notificacion (id_notificacion, tipo, mensaje, fecha, leida, id_usuario) VALUES
(1, 'RESERVA_CONFIRMADA', 'Tu reserva 3 está confirmada.', LOCALTIMESTAMP - INTERVAL '6' DAY, TRUE, 4),
(2, 'RESERVA_CONFIRMADA', 'Tienes una reserva confirmada en Apartamento frente al mar.', LOCALTIMESTAMP - INTERVAL '6' DAY, FALSE, 3),
(3, 'RESERVA_CANCELADA', 'La reserva 4 fue cancelada por el huésped.', LOCALTIMESTAMP - INTERVAL '10' DAY, FALSE, 2),
(4, 'RESERVA_EN_REVISION', 'Tu reserva 5 quedó en revisión porque el alojamiento fue bloqueado por un administrador.', LOCALTIMESTAMP - INTERVAL '4' DAY, FALSE, 4),
(5, 'ALOJAMIENTO_BLOQUEADO', 'Tu alojamiento "Finca cafetera en Manizales" fue bloqueado.', LOCALTIMESTAMP - INTERVAL '4' DAY, FALSE, 3),
(6, 'RESENA_RECIBIDA', 'Recibiste una reseña de 5 estrellas en "Apartamento moderno en Chapinero".', LOCALTIMESTAMP - INTERVAL '25' DAY, TRUE, 2),
(7, 'PAGO_NO_CONFIRMADO', 'El pago de tu reserva no se confirmó y la reserva no quedó registrada como pagada.', LOCALTIMESTAMP - INTERVAL '2' DAY, FALSE, 5),
(8, 'CUENTA_RESTRINGIDA', 'Tu cuenta fue restringida.', LOCALTIMESTAMP - INTERVAL '30' DAY, FALSE, 6);

-- ---- Auditoría, exportaciones e idempotencia ---------------------------------
INSERT INTO evento_auditoria (id_evento, accion, fecha, detalle, id_usuario, entidad, id_entidad, resultado) VALUES
(1, 'REGISTRO_USUARIO', LOCALTIMESTAMP - INTERVAL '120' DAY, 'Rol HUESPED', 4, 'USUARIO', 4, 'EXITOSO'),
(2, 'LOGIN', LOCALTIMESTAMP - INTERVAL '6' DAY, NULL, 4, 'USUARIO', 4, 'EXITOSO'),
(3, 'LOGIN', LOCALTIMESTAMP - INTERVAL '5' DAY, 'Credenciales inválidas', NULL, 'USUARIO', NULL, 'RECHAZADO'),
(4, 'RESERVA_CREADA', LOCALTIMESTAMP - INTERVAL '6' DAY, 'Total 1932000.00', 4, 'RESERVA', 3, 'EXITOSO'),
(5, 'PAGO', LOCALTIMESTAMP - INTERVAL '2' DAY, 'La tarjeta fue rechazada.', 5, 'RESERVA', 6, 'RECHAZADO'),
(6, 'CANCELACION', LOCALTIMESTAMP - INTERVAL '10' DAY, 'FLEXIBLE 100 %, devolución 414000.00', 5, 'RESERVA', 4, 'EXITOSO'),
(7, 'ALOJAMIENTO_CAMBIO_ESTADO', LOCALTIMESTAMP - INTERVAL '4' DAY, 'ACTIVA -> BLOQUEADA', 1, 'ALOJAMIENTO', 7, 'EXITOSO'),
(8, 'USUARIO_RESTRINGIDO', LOCALTIMESTAMP - INTERVAL '30' DAY, 'Incumplimiento de las normas de la plataforma.', 1, 'USUARIO', 6, 'EXITOSO');

INSERT INTO registro_exportacion (id_registro, tipo_reporte, formato, cantidad_registros, id_usuario, fecha) VALUES
(1, 'reservas', 'pdf', 7, 1, LOCALTIMESTAMP - INTERVAL '1' DAY);

INSERT INTO registro_idempotencia (id_registro, clave, id_usuario, operacion, huella_peticion, estado, codigo_respuesta, cuerpo_respuesta, fecha_creacion) VALUES
(1, 'semilla-reserva-3', 4, 'CREAR_RESERVA', '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08', 'COMPLETADA', 201, '{"idReserva":3,"estado":"CONFIRMADA"}', LOCALTIMESTAMP - INTERVAL '6' DAY);

-- El alojamiento 7 entró ACTIVA para poder cotizarlo (trg_validar_cotizacion); ahora se bloquea.
UPDATE alojamiento SET estado = 'BLOQUEADA' WHERE id_alojamiento = 7;

-- ---- Los INSERT usaron ids explícitos: las secuencias continúan desde el último ----
SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'), (SELECT MAX(id_usuario) FROM usuario));
SELECT setval(pg_get_serial_sequence('politica_cancelacion', 'id_politica'), (SELECT MAX(id_politica) FROM politica_cancelacion));
SELECT setval(pg_get_serial_sequence('configuracion_tarifa', 'id_configuracion'), (SELECT MAX(id_configuracion) FROM configuracion_tarifa));
SELECT setval(pg_get_serial_sequence('alojamiento', 'id_alojamiento'), (SELECT MAX(id_alojamiento) FROM alojamiento));
SELECT setval(pg_get_serial_sequence('servicio_adicional', 'id_servicio'), (SELECT MAX(id_servicio) FROM servicio_adicional));
SELECT setval(pg_get_serial_sequence('periodo_disponibilidad', 'id_periodo'), (SELECT MAX(id_periodo) FROM periodo_disponibilidad));
SELECT setval(pg_get_serial_sequence('cotizacion', 'id_cotizacion'), (SELECT MAX(id_cotizacion) FROM cotizacion));
SELECT setval(pg_get_serial_sequence('reserva', 'id_reserva'), (SELECT MAX(id_reserva) FROM reserva));
SELECT setval(pg_get_serial_sequence('pago', 'id_pago'), (SELECT MAX(id_pago) FROM pago));
SELECT setval(pg_get_serial_sequence('cancelacion', 'id_cancelacion'), (SELECT MAX(id_cancelacion) FROM cancelacion));
SELECT setval(pg_get_serial_sequence('resena', 'id_resena'), (SELECT MAX(id_resena) FROM resena));
SELECT setval(pg_get_serial_sequence('favorito', 'id_favorito'), (SELECT MAX(id_favorito) FROM favorito));
SELECT setval(pg_get_serial_sequence('conversacion', 'id_conversacion'), (SELECT MAX(id_conversacion) FROM conversacion));
SELECT setval(pg_get_serial_sequence('mensaje', 'id_mensaje'), (SELECT MAX(id_mensaje) FROM mensaje));
SELECT setval(pg_get_serial_sequence('notificacion', 'id_notificacion'), (SELECT MAX(id_notificacion) FROM notificacion));
SELECT setval(pg_get_serial_sequence('evento_auditoria', 'id_evento'), (SELECT MAX(id_evento) FROM evento_auditoria));
SELECT setval(pg_get_serial_sequence('registro_exportacion', 'id_registro'), (SELECT MAX(id_registro) FROM registro_exportacion));
SELECT setval(pg_get_serial_sequence('registro_idempotencia', 'id_registro'), (SELECT MAX(id_registro) FROM registro_idempotencia));

COMMIT;
