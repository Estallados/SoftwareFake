package co.edu.unbosque.marketplace.entity;

/**
 * Resultado de una operación auditada. Se guardan también las rechazadas.
 *
 * @see "RNF-025"
 */
public enum ResultadoAuditoria {
    EXITOSO,
    RECHAZADO,
    ERROR
}
