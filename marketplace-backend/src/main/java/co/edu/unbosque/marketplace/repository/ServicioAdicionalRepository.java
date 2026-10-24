package co.edu.unbosque.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.ServicioAdicional;

/**
 * Repositorio de {@link ServicioAdicional}.
 *
 * @see "RF-018, CA-185"
 */
public interface ServicioAdicionalRepository extends JpaRepository<ServicioAdicional, Long> {

    public List<ServicioAdicional> findByIdAlojamientoOrderByIdServicio(Long idAlojamiento);

    public Optional<ServicioAdicional> findByIdServicioAndIdAlojamiento(Long idServicio, Long idAlojamiento);
}
