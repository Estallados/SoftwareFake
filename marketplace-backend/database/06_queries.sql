-- =============================================================================
-- Marketplace - 06_queries.sql
-- Consultas equivalentes a las del backend. Los valores fijos corresponden a
-- 05_seed_data.sql: usuario 4 = Ana (huésped), 2 = Laura (anfitriona).
-- correo se muestra cifrado: solo el backend tiene la clave para descifrarlo.
-- =============================================================================
SET client_encoding = 'UTF8';

-- ---- 1. Todos los usuarios --------------------------------------------------
SELECT id_usuario, nombre, telefono, rol, estado, correo_verificado, fecha_registro
  FROM usuario ORDER BY id_usuario;

-- ---- 2. Usuarios por rol y estado -------------------------------------------
SELECT id_usuario, nombre, fecha_registro
  FROM usuario WHERE rol = 'HUESPED' AND estado = 'ACTIVO' ORDER BY nombre;

-- ---- 3. Alojamientos activos (findByEstado) ---------------------------------
SELECT id_alojamiento, titulo, ciudad, tipo, capacidad, precio_noche
  FROM alojamiento WHERE estado = 'ACTIVA' ORDER BY fecha_creacion DESC;

-- ---- 4. Por ciudad (AlojamientoEspecificacion.enCiudad) ---------------------
SELECT id_alojamiento, titulo, precio_noche
  FROM alojamiento WHERE LOWER(ciudad) = LOWER('Bogotá') AND estado = 'ACTIVA';

-- ---- 5. Por rango de precios (precioDesde, precioHasta) ---------------------
SELECT id_alojamiento, titulo, ciudad, precio_noche
  FROM alojamiento WHERE estado = 'ACTIVA' AND precio_noche BETWEEN 150000 AND 400000
 ORDER BY precio_noche;

-- ---- 6. Por capacidad (conCapacidadPara) ------------------------------------
SELECT id_alojamiento, titulo, capacidad
  FROM alojamiento WHERE estado = 'ACTIVA' AND capacidad >= 4 ORDER BY capacidad;

-- ---- 7. Alojamientos de un anfitrión (findByIdAnfitrion) --------------------
SELECT id_alojamiento, titulo, estado, fecha_actualizacion
  FROM alojamiento WHERE id_anfitrion = 2 ORDER BY fecha_creacion DESC;

-- ---- 8. Cotizaciones de un huésped, con su vigencia -------------------------
SELECT c.id_cotizacion, a.titulo, c.fecha_entrada, c.fecha_salida, c.total,
       CASE WHEN c.id_reserva IS NOT NULL THEN 'USADA'
            WHEN c.fecha_expiracion < LOCALTIMESTAMP THEN 'VENCIDA'
            ELSE 'VIGENTE' END AS situacion
  FROM cotizacion c JOIN alojamiento a ON a.id_alojamiento = c.id_alojamiento
 WHERE c.id_usuario = 4 ORDER BY c.fecha_creacion DESC;

-- ---- 9. Reservas de un huésped (findByIdHuesped) ----------------------------
SELECT r.id_reserva, a.titulo, r.fecha_entrada, r.fecha_salida, r.estado, r.total
  FROM reserva r JOIN alojamiento a ON a.id_alojamiento = r.id_alojamiento
 WHERE r.id_huesped = 4 ORDER BY r.fecha_creacion DESC;

-- ---- 10. Reservas de un anfitrión (buscarDeAnfitrion) -----------------------
SELECT r.id_reserva, r.fecha_entrada, r.fecha_salida, r.estado, r.total
  FROM reserva r
 WHERE r.id_alojamiento IN (SELECT a.id_alojamiento FROM alojamiento a WHERE a.id_anfitrion = 2)
 ORDER BY r.fecha_entrada;

-- ---- 11. Reservas por estado (listarResumenAdmin) ---------------------------
SELECT r.id_reserva, h.nombre AS huesped, a.titulo, a.ciudad, an.nombre AS anfitrion, r.total
  FROM reserva r
  LEFT JOIN usuario h ON h.id_usuario = r.id_huesped
  LEFT JOIN alojamiento a ON a.id_alojamiento = r.id_alojamiento
  LEFT JOIN usuario an ON an.id_usuario = a.id_anfitrion
 WHERE r.estado = 'CONFIRMADA';

-- ---- 12. Disponibilidad entre dos fechas (libreEntre) -----------------------
-- Ofrecido en todo el rango y sin reservas que lo ocupen. Una pendiente solo
-- ocupa durante sus primeros 15 minutos (CalendarioService.MINUTOS_PENDIENTE).
SELECT a.id_alojamiento, a.titulo
  FROM alojamiento a
 WHERE a.estado = 'ACTIVA'
   AND EXISTS (SELECT 1 FROM periodo_disponibilidad p
                WHERE p.id_alojamiento = a.id_alojamiento
                  AND p.fecha_inicio <= CURRENT_DATE + 30 AND p.fecha_fin >= CURRENT_DATE + 35)
   AND NOT EXISTS (SELECT 1 FROM reserva r
                    WHERE r.id_alojamiento = a.id_alojamiento
                      AND r.fecha_entrada < CURRENT_DATE + 35 AND r.fecha_salida > CURRENT_DATE + 30
                      AND (r.estado IN ('CONFIRMADA', 'EN_REVISION')
                           OR (r.estado = 'PENDIENTE_PAGO'
                               AND r.fecha_creacion >= LOCALTIMESTAMP - INTERVAL '15' MINUTE)));

-- ---- 13. Pagos de una reserva, el más reciente primero ----------------------
SELECT id_pago, proveedor, metodo, valor, estado, valor_reembolsado, fecha
  FROM pago WHERE id_reserva = 4 ORDER BY fecha DESC, id_pago DESC;

-- ---- 14. Cancelaciones y devoluciones ---------------------------------------
SELECT c.id_cancelacion, c.id_reserva, u.nombre AS cancelada_por, c.motivo, c.politica,
       c.porcentaje_reembolso, c.valor_reembolso, p.estado AS estado_pago, p.referencia_reembolso
  FROM cancelacion c
  JOIN usuario u ON u.id_usuario = c.id_usuario
  LEFT JOIN pago p ON p.id_reserva = c.id_reserva
 ORDER BY c.fecha DESC;

-- ---- 15. Conversaciones de un usuario (buscarDeParticipante) ----------------
SELECT c.id_conversacion, a.titulo, c.estado, c.fecha_creacion
  FROM conversacion c JOIN alojamiento a ON a.id_alojamiento = c.id_alojamiento
 WHERE c.id_usuario = 4 OR c.id_anfitrion = 4;

-- ---- 16. Mensajes de una conversación (findByIdConversacion) ----------------
SELECT m.id_mensaje, u.nombre, m.contenido, m.fecha_envio
  FROM mensaje m JOIN usuario u ON u.id_usuario = m.id_usuario
 WHERE m.id_conversacion = 1 ORDER BY m.fecha_envio, m.id_mensaje;

-- ---- 17. Notificaciones pendientes (countByIdUsuarioAndLeidaFalse) ----------
SELECT id_notificacion, tipo, mensaje, fecha
  FROM notificacion WHERE id_usuario = 4 AND leida = FALSE ORDER BY fecha DESC;

-- ---- 18. Reseñas visibles de un alojamiento ---------------------------------
SELECT r.calificacion, r.comentario, u.nombre, r.fecha
  FROM resena r JOIN usuario u ON u.id_usuario = r.id_usuario
 WHERE r.id_alojamiento = 1 AND r.estado = 'PUBLICADA' ORDER BY r.fecha DESC;

-- ---- 19. Favoritos de un usuario (findByIdUsuario) --------------------------
SELECT f.fecha_agregado, a.id_alojamiento, a.titulo, a.ciudad, a.precio_noche
  FROM favorito f JOIN alojamiento a ON a.id_alojamiento = f.id_alojamiento
 WHERE f.id_usuario = 4 ORDER BY f.fecha_agregado DESC;

-- ---- 20. Auditoría con filtros (AuditoriaRepository.buscar) -----------------
SELECT e.id_evento, e.fecha, e.accion, u.nombre AS actor, e.entidad, e.id_entidad, e.resultado, e.detalle
  FROM evento_auditoria e LEFT JOIN usuario u ON u.id_usuario = e.id_usuario
 WHERE e.fecha >= LOCALTIMESTAMP - INTERVAL '30' DAY
 ORDER BY e.fecha DESC, e.id_evento DESC;

-- =============================================================================
-- Consultas elaboradas
-- =============================================================================

-- ---- 21. Catálogo paginado con calificación (LEFT JOIN, GROUP BY, paginación) ----
-- Mismo orden que AlojamientoEspecificacion.ordenadoPorResenas. Página 1 de 2 por página.
SELECT a.id_alojamiento, a.titulo, a.ciudad, a.precio_noche,
       COALESCE(ROUND(AVG(r.calificacion), 1), 0) AS promedio, COUNT(r.id_resena) AS resenas
  FROM alojamiento a
  LEFT JOIN resena r ON r.id_alojamiento = a.id_alojamiento AND r.estado = 'PUBLICADA'
 WHERE a.estado = 'ACTIVA'
 GROUP BY a.id_alojamiento, a.titulo, a.ciudad, a.precio_noche, a.fecha_creacion
 ORDER BY promedio DESC, resenas DESC, a.fecha_creacion DESC, a.id_alojamiento DESC
 LIMIT 2 OFFSET 0;

-- ---- 22. Reservas por estado (contarPorEstado) ------------------------------
SELECT estado, COUNT(*) AS cantidad, SUM(total) AS valor
  FROM reserva GROUP BY estado ORDER BY cantidad DESC;

-- ---- 23. Demanda por ciudad (contarPorCiudad) -------------------------------
SELECT a.ciudad, COUNT(r.id_reserva) AS reservas
  FROM reserva r JOIN alojamiento a ON a.id_alojamiento = r.id_alojamiento
 WHERE r.estado <> 'CANCELADA'
 GROUP BY a.ciudad ORDER BY reservas DESC, a.ciudad;

-- ---- 24. Anfitriones con más de una publicación activa (HAVING) -------------
SELECT u.id_usuario, u.nombre, COUNT(*) AS activos
  FROM usuario u JOIN alojamiento a ON a.id_anfitrion = u.id_usuario
 WHERE a.estado = 'ACTIVA'
 GROUP BY u.id_usuario, u.nombre
HAVING COUNT(*) > 1
 ORDER BY activos DESC;

-- ---- 25. Ingresos: cobrado, devuelto y neto por alojamiento -----------------
-- Un pago REEMBOLSADO se cobró y luego se devolvió (parcial o totalmente).
SELECT a.id_alojamiento, a.titulo,
       SUM(CASE WHEN p.estado IN ('APROBADO', 'REEMBOLSADO') THEN p.valor ELSE 0 END) AS cobrado,
       SUM(COALESCE(p.valor_reembolsado, 0)) AS devuelto,
       SUM(CASE WHEN p.estado IN ('APROBADO', 'REEMBOLSADO') THEN p.valor ELSE 0 END)
           - SUM(COALESCE(p.valor_reembolsado, 0)) AS neto
  FROM alojamiento a
  JOIN reserva r ON r.id_alojamiento = a.id_alojamiento
  JOIN pago p ON p.id_reserva = r.id_reserva
 GROUP BY a.id_alojamiento, a.titulo
 ORDER BY neto DESC;

-- ---- 26. Alojamientos más caros que el promedio de su ciudad (subconsulta correlacionada) ----
SELECT a.id_alojamiento, a.titulo, a.ciudad, a.precio_noche
  FROM alojamiento a
 WHERE a.precio_noche > (SELECT AVG(b.precio_noche) FROM alojamiento b WHERE b.ciudad = a.ciudad AND b.estado = 'ACTIVA')
   AND a.estado = 'ACTIVA';

-- ---- 27. Estancias completadas sin reseña: a quién pedirle una --------------
-- Es la condición de ReservaService.esCompletada.
SELECT r.id_reserva, u.nombre AS huesped, a.titulo, r.fecha_salida
  FROM reserva r
  JOIN usuario u ON u.id_usuario = r.id_huesped
  JOIN alojamiento a ON a.id_alojamiento = r.id_alojamiento
  LEFT JOIN resena re ON re.id_reserva = r.id_reserva
 WHERE r.estado = 'CONFIRMADA' AND r.fecha_salida <= CURRENT_DATE AND re.id_resena IS NULL;

-- ---- 28. Último pago de cada reserva (findTopByIdReserva..., subconsulta) ---
SELECT r.id_reserva, r.estado, p.estado AS estado_pago, p.valor
  FROM reserva r
  LEFT JOIN pago p ON p.id_pago = (SELECT MAX(p2.id_pago) FROM pago p2 WHERE p2.id_reserva = r.id_reserva)
 ORDER BY r.id_reserva;

-- ---- 29. Control de integridad: reservas pagadas sin pago aprobado ----------
-- Debe devolver cero filas.
SELECT r.id_reserva, r.estado
  FROM reserva r
 WHERE r.estado IN ('CONFIRMADA', 'EN_REVISION')
   AND NOT EXISTS (SELECT 1 FROM pago p WHERE p.id_reserva = r.id_reserva AND p.estado = 'APROBADO');
