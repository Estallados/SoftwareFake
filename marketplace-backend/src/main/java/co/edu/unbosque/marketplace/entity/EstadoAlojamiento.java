package co.edu.unbosque.marketplace.entity;

/**
 * Estados por los que pasa la publicación de un alojamiento. Solo se permiten
 * estos cinco, y cada cambio queda guardado en
 * {@link HistorialEstadoAlojamiento}.
 */
public enum EstadoAlojamiento {

	/** Recién creado o incompleto; solo lo ve su anfitrión. */
	BORRADOR,

	/** El anfitrión lo envió a revisión y espera la aprobación del administrador. */
	PENDIENTE_REVISION,

	/** Aprobado y publicado: aparece en las búsquedas y se puede reservar. */
	ACTIVA,

	/** El anfitrión lo ocultó temporalmente; no recibe reservas nuevas. */
	PAUSADA,

	/** Un administrador lo bloqueó; el anfitrión no puede reactivarlo. */
	BLOQUEADA
}
