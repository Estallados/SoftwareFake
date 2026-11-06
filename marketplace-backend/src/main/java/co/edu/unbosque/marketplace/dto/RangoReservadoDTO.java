package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

/**
 * Rango de fechas ocupado por una reserva activa (llegada inclusive, salida
 * exclusiva).
 */
public class RangoReservadoDTO {

	/** Día de llegada (ocupa noche). */
	private LocalDate fechaEntrada;

	/** Día de salida (no ocupa noche). */
	private LocalDate fechaSalida;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public RangoReservadoDTO() {
	}

	/**
	 * Devuelve el día de llegada (ocupa noche).
	 *
	 * @return día de llegada (ocupa noche)
	 */
	public LocalDate getFechaEntrada() {
		return fechaEntrada;
	}

	/**
	 * Asigna el día de llegada (ocupa noche).
	 *
	 * @param fechaEntrada día de llegada (ocupa noche)
	 */
	public void setFechaEntrada(LocalDate fechaEntrada) {
		this.fechaEntrada = fechaEntrada;
	}

	/**
	 * Devuelve el día de salida (no ocupa noche).
	 *
	 * @return día de salida (no ocupa noche)
	 */
	public LocalDate getFechaSalida() {
		return fechaSalida;
	}

	/**
	 * Asigna el día de salida (no ocupa noche).
	 *
	 * @param fechaSalida día de salida (no ocupa noche)
	 */
	public void setFechaSalida(LocalDate fechaSalida) {
		this.fechaSalida = fechaSalida;
	}
}
