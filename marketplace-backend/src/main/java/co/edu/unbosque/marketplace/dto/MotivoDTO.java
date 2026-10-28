package co.edu.unbosque.marketplace.dto;

/**
 * Motivo obligatorio de una acción (rechazo, bloqueo, restricción, cancelación,
 * moderación).
 *
 * @see "RF-002, RF-013, RF-014, RF-016, RF-027"
 */
public class MotivoDTO {
	private String motivo;

	public MotivoDTO() {
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
}
