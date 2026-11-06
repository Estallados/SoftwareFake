package co.edu.unbosque.marketplace.entity;

/**
 * Resultado de una operación auditada. En la auditoría también se guardan las
 * operaciones rechazadas y las que fallaron, no solo las exitosas.
 */
public enum ResultadoAuditoria {

	/** La operación se realizó. */
	EXITOSO,

	/** La operación se rechazó por una regla de negocio o de permisos. */
	RECHAZADO,

	/** La operación falló por un error del sistema. */
	ERROR
}
