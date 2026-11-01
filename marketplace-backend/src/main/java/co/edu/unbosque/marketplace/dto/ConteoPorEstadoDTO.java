package co.edu.unbosque.marketplace.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Total y cantidad por estado (dashboard). {@code porEstado} trae todos los
 * estados, con 0 si no hay. {@code confirmadasFinalizadas} solo aplica a
 * reservas; si es nulo no se envía.
 *
 * @see "RF-005"
 */
public record ConteoPorEstadoDTO(long total, Map<String, Long> porEstado,
		@JsonInclude(JsonInclude.Include.NON_NULL) Long confirmadasFinalizadas) {
}
