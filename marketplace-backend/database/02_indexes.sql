-- =============================================================================
-- Marketplace - 02_indexes.sql
-- Solo índices que atienden una consulta real del backend. No se repiten los que
-- PostgreSQL ya crea por PRIMARY KEY, UNIQUE o EXCLUDE:
--   usuario.correo_hash          -> findByCorreoHash (login y registro)
--   favorito (usuario, aloj.)    -> findByIdUsuario, findByIdUsuarioAndIdAlojamiento
--   conversacion (usuario, aloj.)-> findByIdUsuarioAndIdAlojamiento
--   registro_idempotencia        -> findByIdUsuarioAndOperacionAndClave
--   cancelacion.id_reserva, resena.id_reserva, cotizacion.id_reserva
--   periodo_disponibilidad y reserva: el índice GiST de EXCLUDE ayuda en cruces
-- =============================================================================
SET client_encoding = 'UTF8';

-- ---- alojamiento ------------------------------------------------------------
-- Catálogo público: findByEstado y AlojamientoEspecificacion.conEstado, ordenado por precio o fecha.
CREATE INDEX idx_alojamiento_estado_precio ON alojamiento (estado, precio_noche);
CREATE INDEX idx_alojamiento_estado_fecha ON alojamiento (estado, fecha_creacion DESC, id_alojamiento DESC);
-- enCiudad y deTipo comparan con lower(columna), así que el índice debe ser por expresión.
CREATE INDEX idx_alojamiento_ciudad ON alojamiento (LOWER(ciudad), estado);
CREATE INDEX idx_alojamiento_tipo ON alojamiento (LOWER(tipo), estado);
-- conCapacidadPara: solo publicaciones activas, que son las que se buscan.
CREATE INDEX idx_alojamiento_capacidad ON alojamiento (capacidad) WHERE estado = 'ACTIVA';
-- findByIdAnfitrion y las subconsultas de buscarDeAnfitrion (reservas y reseñas).
CREATE INDEX idx_alojamiento_anfitrion ON alojamiento (id_anfitrion, fecha_creacion DESC);
-- FK; contarPorTipo y demás agregados recorren la tabla, no necesitan índice propio.
CREATE INDEX idx_alojamiento_politica ON alojamiento (id_politica);

-- conAmenidades: busca alojamientos que tengan una amenidad (la PK empieza por el alojamiento).
CREATE INDEX idx_amenidad_nombre ON alojamiento_amenidad (amenidad, id_alojamiento);

-- findByIdAlojamientoOrderByIdServicio.
CREATE INDEX idx_servicio_alojamiento ON servicio_adicional (id_alojamiento, id_servicio);
-- findByIdAlojamientoOrderByFechaInicio, contiene y libreEntre.
CREATE INDEX idx_periodo_alojamiento ON periodo_disponibilidad (id_alojamiento, fecha_inicio, fecha_fin);
-- findByIdAlojamientoOrderByFechaAscIdHistorialAsc.
CREATE INDEX idx_historial_alojamiento ON historial_estado_alojamiento (id_alojamiento, fecha, id_historial);
-- FK hacia usuario.
CREATE INDEX idx_historial_usuario ON historial_estado_alojamiento (id_usuario);

-- ---- configuracion_tarifa ---------------------------------------------------
-- findTopByOrderByFechaDescIdConfiguracionDesc: se consulta en cada cotización.
CREATE INDEX idx_configuracion_vigente ON configuracion_tarifa (fecha DESC, id_configuracion DESC);
CREATE INDEX idx_configuracion_usuario ON configuracion_tarifa (id_usuario);

-- ---- cotizacion -------------------------------------------------------------
-- Cotizaciones de un huésped y claves foráneas.
CREATE INDEX idx_cotizacion_usuario ON cotizacion (id_usuario, fecha_creacion DESC);
CREATE INDEX idx_cotizacion_alojamiento ON cotizacion (id_alojamiento);

-- ---- reserva ----------------------------------------------------------------
-- existeCruce, buscarOcupadas, libreEntre y findByIdAlojamientoAndFechaEntradaAfterAndEstadoIn.
CREATE INDEX idx_reserva_alojamiento_fechas ON reserva (id_alojamiento, fecha_entrada, fecha_salida, estado);
-- findByIdHuesped: "mis reservas".
CREATE INDEX idx_reserva_huesped ON reserva (id_huesped, fecha_creacion DESC);
-- listarResumenAdmin por estado y countByEstadoAndFechaSalidaBefore.
CREATE INDEX idx_reserva_estado_salida ON reserva (estado, fecha_salida);
-- cancelarPendientesVencidas corre cada minuto; hay muy pocas pendientes, el índice parcial es mínimo.
CREATE INDEX idx_reserva_pendientes ON reserva (fecha_creacion) WHERE estado = 'PENDIENTE_PAGO';
-- FK hacia cotizacion.
CREATE INDEX idx_reserva_cotizacion ON reserva (id_cotizacion);

-- ---- pago, cancelacion, resena ----------------------------------------------
-- findTopByIdReservaOrderByFechaDescIdPagoDesc: se consulta al mostrar cada reserva.
CREATE INDEX idx_pago_reserva ON pago (id_reserva, fecha DESC, id_pago DESC);
-- sumarValores (ingresos por estado en el dashboard y los reportes).
CREATE INDEX idx_pago_estado ON pago (estado);
-- FK hacia usuario.
CREATE INDEX idx_cancelacion_usuario ON cancelacion (id_usuario);
-- findByIdAlojamientoAndEstado, promedio y countByIdAlojamientoAndEstado.
CREATE INDEX idx_resena_alojamiento_estado ON resena (id_alojamiento, estado, fecha DESC);
-- findByIdUsuario y parte de la FK compuesta.
CREATE INDEX idx_resena_usuario ON resena (id_usuario);
-- findByEstado: moderación.
CREATE INDEX idx_resena_estado ON resena (estado, fecha DESC);
-- FK hacia alojamiento; también responde "quién guardó este alojamiento".
CREATE INDEX idx_favorito_alojamiento ON favorito (id_alojamiento);

-- ---- mensajería y notificaciones --------------------------------------------
-- buscarDeParticipante consulta por huésped O anfitrión; el huésped lo cubre uq_conversacion.
CREATE INDEX idx_conversacion_anfitrion ON conversacion (id_anfitrion);
CREATE INDEX idx_conversacion_alojamiento ON conversacion (id_alojamiento, id_anfitrion);
-- findByIdConversacion en orden de envío.
CREATE INDEX idx_mensaje_conversacion ON mensaje (id_conversacion, fecha_envio, id_mensaje);
-- FK hacia usuario.
CREATE INDEX idx_mensaje_usuario ON mensaje (id_usuario);
-- findByIdUsuario: bandeja de notificaciones.
CREATE INDEX idx_notificacion_usuario ON notificacion (id_usuario, fecha DESC);
-- countByIdUsuarioAndLeidaFalse y marcarTodasLeidas: contador de no leídas.
CREATE INDEX idx_notificacion_pendientes ON notificacion (id_usuario) WHERE leida = FALSE;

-- ---- auditoría y exportaciones ----------------------------------------------
-- AuditoriaRepository.findAll y buscar por rango de fechas.
CREATE INDEX idx_evento_fecha ON evento_auditoria (fecha DESC, id_evento DESC);
-- findByAccion y buscar con filtro de acción.
CREATE INDEX idx_evento_accion ON evento_auditoria (accion, fecha DESC);
-- buscar con filtro de actor (solo cuando se conoce).
CREATE INDEX idx_evento_usuario ON evento_auditoria (id_usuario, fecha DESC) WHERE id_usuario IS NOT NULL;
-- FK hacia usuario.
CREATE INDEX idx_exportacion_usuario ON registro_exportacion (id_usuario);
-- sp_purgar_idempotencia: borra los registros antiguos.
CREATE INDEX idx_idempotencia_fecha ON registro_idempotencia (fecha_creacion);
