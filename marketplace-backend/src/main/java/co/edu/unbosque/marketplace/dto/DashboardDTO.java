package co.edu.unbosque.marketplace.dto;

import java.util.List;

/**
 * Indicadores del dashboard administrativo, ya calculados con consultas
 * agregadas (sin listas de entidades).
 *
 * @see "RF-005"
 */
public record DashboardDTO(long totalUsuarios, ConteoPorEstadoDTO alojamientos, ConteoPorEstadoDTO reservas,
		List<DemandaCiudadDTO> ciudadesTop) {
}
