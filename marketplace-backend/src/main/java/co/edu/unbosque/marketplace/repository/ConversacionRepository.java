package co.edu.unbosque.marketplace.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.Conversacion;

/**
 * Repositorio de {@link Conversacion}.
 *
 * @see "RF-029"
 */
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

	public Optional<Conversacion> findByIdUsuarioAndIdAlojamiento(Long idUsuario, Long idAlojamiento);

	/**
	 * Conversaciones en las que participa el usuario, como huésped o como
	 * anfitrión.
	 */
	@Query("select c from Conversacion c where c.idUsuario = :id or c.idAnfitrion = :id")
	public Page<Conversacion> buscarDeParticipante(@Param("id") Long idUsuario, Pageable pageable);
}
