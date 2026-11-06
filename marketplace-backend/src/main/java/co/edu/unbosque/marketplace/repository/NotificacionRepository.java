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
 * Acceso a las notificaciones de los usuarios.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

	/**
	 * Notificaciones de un usuario, paginadas.
	 *
	 * @param idUsuario usuario que las recibe
	 * @param pageable página y orden
	 * @return página de notificaciones
	 */
	public Page<Notificacion> findByIdUsuario(Long idUsuario, Pageable pageable);

	/**
	 * Busca una notificación verificando que sea del usuario.
	 *
	 * @param idNotificacion notificación a buscar
	 * @param idUsuario usuario al que debe pertenecer
	 * @return la notificación, si existe y es del usuario
	 */
	public Optional<Notificacion> findByIdNotificacionAndIdUsuario(Long idNotificacion, Long idUsuario);

	/**
	 * Cuenta las notificaciones sin leer de un usuario (el número de la
	 * campana).
	 *
	 * @param idUsuario usuario
	 * @return cantidad de notificaciones sin leer
	 */
	public long countByIdUsuarioAndLeidaFalse(Long idUsuario);

	/**
	 * Marca como leídas todas las notificaciones sin leer de un usuario.
	 *
	 * @param idUsuario usuario
	 * @return cantidad de notificaciones que cambiaron
	 */
	@Modifying
	@Query("update Notificacion n set n.leida = true where n.idUsuario = :idUsuario and n.leida = false")
	public int marcarTodasLeidas(@Param("idUsuario") Long idUsuario);
}
