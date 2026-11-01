package co.edu.unbosque.marketplace.dto;

/**
 * Cantidad de reservas de una ciudad.
 *
 * @see "RF-005, CA-026"
 */
public record DemandaCiudadDTO(String ciudad, Long totalReservas) {
}
