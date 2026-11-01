package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Fila resumida del listado de reservas del administrador: sin desglose, pago
 * ni cancelación. Se arma directamente en la consulta (una sola por página).
 * {@code completada} sigue la regla de {@code ReservaService.esCompletada}.
 *
 * @see "RF-016"
 */
@SuppressWarnings("java:S107") // Proyección JPQL: un componente por columna.
public record ReservaAdminDTO(Long idReserva, String nombreHuesped, String tituloAlojamiento, String ciudad,
		String nombreAnfitrion, LocalDate fechaEntrada, LocalDate fechaSalida, Integer noches, BigDecimal total,
		EstadoReserva estado, Boolean completada) {
}
