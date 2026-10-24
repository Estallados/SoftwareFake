-- Ejecutar UNA vez en la base PostgreSQL de desarrollo antes de arrancar la versión con los módulos 2 a 9.
--
-- Motivo: los estados de alojamiento, reserva y pago cambiaron (CA-104) y Hibernate creó en su momento
-- restricciones CHECK con los valores viejos; spring.jpa.hibernate.ddl-auto=update no las actualiza.
-- Estas tablas no tenían datos reales, así que se borran y Hibernate las vuelve a crear al arrancar.
--
-- Se CONSERVA la tabla usuario (las cuentas registradas siguen funcionando).
-- Se pierde el historial de evento_auditoria generado en las pruebas de desarrollo.

DROP TABLE IF EXISTS
    alojamiento_amenidad,
    alojamiento,
    imagen,
    servicio_adicional,
    politica_cancelacion,
    periodo_disponibilidad,
    historial_estado_alojamiento,
    configuracion_tarifa,
    cotizacion,
    reserva,
    pago,
    cancelacion,
    notificacion,
    resena,
    favorito,
    conversacion,
    mensaje,
    evento_auditoria,
    registro_idempotencia,
    token_recuperacion,
    registro_exportacion
CASCADE;
