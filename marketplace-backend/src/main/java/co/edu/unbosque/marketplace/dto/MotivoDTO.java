package co.edu.unbosque.marketplace.dto;

/**
 * Motivo obligatorio de una acción (rechazo, bloqueo, restricción, cancelación,
 * moderación).
 */
public class MotivoDTO {

	/** Explicación de la acción; queda en la auditoría y se le notifica al afectado. */
	private String motivo;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public MotivoDTO() {
	}

	/**
	 * Devuelve la explicación de la acción; queda en la auditoría y se le notifica al afectado.
	 *
	 * @return explicación de la acción; queda en la auditoría y se le notifica al afectado
	 */
	public String getMotivo() {
		return motivo;
	}

	/**
	 * Asigna la explicación de la acción; queda en la auditoría y se le notifica al afectado.
	 *
	 * @param motivo explicación de la acción; queda en la auditoría y se le notifica al afectado
	 */
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
}
