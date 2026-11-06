package co.edu.unbosque.marketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Mensaje;

/**
 * Acceso a los mensajes de las conversaciones.
 */
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

	/**
	 * Mensajes de una conversación, paginados.
	 *
	 * @param idConversacion conversación a consultar
	 * @param pageable página y orden
	 * @return página de mensajes
	 */
	public Page<Mensaje> findByIdConversacion(Long idConversacion, Pageable pageable);
}
