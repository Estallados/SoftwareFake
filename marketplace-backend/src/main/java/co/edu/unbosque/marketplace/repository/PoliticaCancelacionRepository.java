package co.edu.unbosque.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.PoliticaCancelacion;

/**
 * Repositorio de {@link PoliticaCancelacion}.
 *
 * @see "RF-002"
 */
public interface PoliticaCancelacionRepository extends JpaRepository<PoliticaCancelacion, Long> {

    public Optional<PoliticaCancelacion> findByNombre(String nombre);
}
