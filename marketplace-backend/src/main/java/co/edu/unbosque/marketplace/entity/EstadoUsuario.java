package co.edu.unbosque.marketplace.entity;

/**
 * Estados de la cuenta de un usuario.
 */
public enum EstadoUsuario {

	/** La cuenta funciona con normalidad. */
	ACTIVO,

	/** El dueño desactivó su cuenta; no puede iniciar sesión hasta reactivarla. */
	INACTIVO,

	/** Un administrador restringió la cuenta; sus tokens dejan de funcionar. */
	RESTRINGIDO
}
