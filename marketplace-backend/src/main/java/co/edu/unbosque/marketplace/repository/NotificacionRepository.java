package co.edu.unbosque.marketplace.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Notificacion;

/**
 * Repositorio de {@link Notificacion}.
 *
 * @see "RF-003"
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    public Page<Notificacion> findByIdUsuario(Long idUsuario, Pageable pageable);

    public Optional<Notificacion> findByIdNotificacionAndIdUsuario(Long idNotificacion, Long idUsuario);
}
