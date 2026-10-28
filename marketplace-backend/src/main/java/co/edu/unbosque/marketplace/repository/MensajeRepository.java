package co.edu.unbosque.marketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Mensaje;

/**
 * Repositorio de {@link Mensaje}.
 *
 * @see "RF-029"
 */
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

	public Page<Mensaje> findByIdConversacion(Long idConversacion, Pageable pageable);
}
