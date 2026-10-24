package co.edu.unbosque.marketplace.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import co.edu.unbosque.marketplace.entity.HistorialEstadoAlojamiento;

/**
 * Repositorio de solo inserción para {@link HistorialEstadoAlojamiento}.
 *
 * @see "RF-027, RNF-026"
 */
public interface HistorialEstadoAlojamientoRepository extends Repository<HistorialEstadoAlojamiento, Long> {

    public HistorialEstadoAlojamiento save(HistorialEstadoAlojamiento historial);

    public List<HistorialEstadoAlojamiento> findByIdAlojamientoOrderByFechaAscIdHistorialAsc(Long idAlojamiento);
}
