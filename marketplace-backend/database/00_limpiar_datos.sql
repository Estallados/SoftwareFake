-- =============================================================================
-- Marketplace - 00_limpiar_datos.sql
-- Borra TODOS los datos (no el esquema) y reinicia las secuencias, para volver a
-- poblar con 05_seed_data.sql. Todo o nada: corre en una sola transacción.
--
-- evento_auditoria e historial_estado_alojamiento son inmutables (03_triggers.sql):
-- sus triggers se desactivan solo dentro de esta transacción y se reactivan antes
-- del COMMIT. Requiere ser dueño de las tablas.
--
-- Para volver a poblar, carga 05_seed_data.sql en modo carga (03b_triggers_validacion.sql):
--   $env:PGOPTIONS = '-c marketplace.carga_datos=on'
--
-- NUNCA lo ejecutes en producción.
-- =============================================================================
SET client_encoding = 'UTF8';
BEGIN;

ALTER TABLE evento_auditoria DISABLE TRIGGER USER;
ALTER TABLE historial_estado_alojamiento DISABLE TRIGGER USER;

TRUNCATE TABLE
    registro_idempotencia,
    registro_exportacion,
    evento_auditoria,
    notificacion,
    mensaje,
    conversacion,
    favorito,
    resena,
    cancelacion,
    pago,
    reserva,
    cotizacion,
    historial_estado_alojamiento,
    periodo_disponibilidad,
    servicio_adicional,
    alojamiento_amenidad,
    alojamiento,
    configuracion_tarifa,
    politica_cancelacion,
    usuario
RESTART IDENTITY CASCADE;

ALTER TABLE evento_auditoria ENABLE TRIGGER USER;
ALTER TABLE historial_estado_alojamiento ENABLE TRIGGER USER;

COMMIT;
