package co.edu.unbosque.marketplace.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import co.edu.unbosque.marketplace.entity.HistorialEstadoAlojamiento;

/**
 * Acceso de solo inserción al historial de estados de los alojamientos.
 * Extiende {@link Repository} y no {@code JpaRepository} a propósito: así no
 * existen métodos para modificar ni borrar registros.
 */
public interface HistorialEstadoAlojamientoRepository extends Repository<HistorialEstadoAlojamiento, Long> {

	/**
	 * Guarda un cambio de estado nuevo.
	 *
	 * @param historial registro a guardar
	 * @return el registro guardado, con su id
	 */
	public HistorialEstadoAlojamiento save(HistorialEstadoAlojamiento historial);

	/**
	 * Historial completo de un alojamiento, del cambio más antiguo al más
	 * reciente.
	 *
	 * @param idAlojamiento alojamiento a consultar
	 * @return lista de cambios de estado
	 */
	public List<HistorialEstadoAlojamiento> findByIdAlojamientoOrderByFechaAscIdHistorialAsc(Long idAlojamiento);
}
