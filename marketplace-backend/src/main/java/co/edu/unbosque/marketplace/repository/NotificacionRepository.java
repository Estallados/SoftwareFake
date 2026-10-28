package co.edu.unbosque.marketplace.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.Notificacion;

/**
 * Repositorio de {@link Notificacion}.
 *
 * @see "RF-003"
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

	public Page<Notificacion> findByIdUsuario(Long idUsuario, Pageable pageable);

	public Optional<Notificacion> findByIdNotificacionAndIdUsuario(Long idNotificacion, Long idUsuario);

	/** Notificaciones sin leer de un usuario. */
	public long countByIdUsuarioAndLeidaFalse(Long idUsuario);

	/**
	 * Marca como leídas todas las notificaciones sin leer de un usuario; devuelve
	 * cuántas cambiaron.
	 */
	@Modifying
	@Query("update Notificacion n set n.leida = true where n.idUsuario = :idUsuario and n.leida = false")
	public int marcarTodasLeidas(@Param("idUsuario") Long idUsuario);
}
