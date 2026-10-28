package co.edu.unbosque.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.TokenRecuperacion;

/**
 * Repositorio de {@link TokenRecuperacion}.
 *
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

	/**
	 * Busca un token por su hash.
	 *
	 * @param tokenHash Hash del token recibido.
	 * @return El token si existe.
	 */
	public Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

	/**
	 * Tokens que el usuario aún no ha usado; se invalidan al pedir uno nuevo.
	 *
	 * @param idUsuario Usuario dueño de los tokens.
	 * @return Tokens sin usar.
	 */
	public List<TokenRecuperacion> findByIdUsuarioAndUsadoFalse(Long idUsuario);
}
