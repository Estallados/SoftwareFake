package co.edu.unbosque.marketplace.entity;

/**
 * Estado de una petición registrada con Idempotency-Key.
 *
 * @see "RNF-013, RNF-014"
 */
public enum EstadoIdempotencia {
    EN_PROCESO,
    COMPLETADA
}
