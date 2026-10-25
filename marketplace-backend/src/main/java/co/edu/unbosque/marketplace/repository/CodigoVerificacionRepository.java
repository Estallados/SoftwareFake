package co.edu.unbosque.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.CodigoVerificacion;

/**
 * Repositorio de {@link CodigoVerificacion}.
 *
 * @see "RF-001"
 */
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    /**
     * Último código sin usar del usuario (el único vigente).
     *
     * @param idUsuario Usuario dueño del código.
     * @return El código más reciente sin usar, si existe.
     */
    public Optional<CodigoVerificacion> findTopByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(Long idUsuario);

    /**
     * Códigos que el usuario aún no ha usado; se invalidan al pedir uno nuevo.
     *
     * @param idUsuario Usuario dueño de los códigos.
     * @return Códigos sin usar.
     */
    public List<CodigoVerificacion> findByIdUsuarioAndUsadoFalse(Long idUsuario);
}
