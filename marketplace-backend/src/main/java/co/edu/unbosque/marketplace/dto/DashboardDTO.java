package co.edu.unbosque.marketplace.dto;

import java.util.List;

/**
 * Indicadores del dashboard administrativo, ya calculados con consultas
 * agregadas (sin listas de entidades).
 *
 * @param totalUsuarios cantidad de usuarios registrados
 * @param alojamientos  total de alojamientos y cantidad por estado
 * @param reservas      total de reservas y cantidad por estado
 * @param ciudadesTop   ciudades con más reservas, de mayor a menor
 */
public record DashboardDTO(long totalUsuarios, ConteoPorEstadoDTO alojamientos, ConteoPorEstadoDTO reservas,
		List<DemandaCiudadDTO> ciudadesTop) {
}
