package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Fila resumida del listado de reservas del administrador: sin desglose, pago
 * ni cancelación. Se arma directamente en la consulta (una sola por página).
 * {@code completada} sigue la regla de {@code ReservaService.esCompletada}.
 *
 * @param idReserva         código de la reserva
 * @param nombreHuesped     nombre del huésped
 * @param tituloAlojamiento título del alojamiento
 * @param ciudad            ciudad del alojamiento
 * @param nombreAnfitrion   nombre del anfitrión
 * @param fechaEntrada      día de llegada
 * @param fechaSalida       día de salida
 * @param noches            cantidad de noches
 * @param total             valor total
 * @param estado            estado de la reserva
 * @param completada        {@code true} si está confirmada y la salida ya pasó
 */
@SuppressWarnings("java:S107") // Proyección JPQL: un componente por columna.
public record ReservaAdminDTO(Long idReserva, String nombreHuesped, String tituloAlojamiento, String ciudad,
		String nombreAnfitrion, LocalDate fechaEntrada, LocalDate fechaSalida, Integer noches, BigDecimal total,
		EstadoReserva estado, Boolean completada) {
}
