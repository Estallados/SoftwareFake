package co.edu.unbosque.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.RegistroIdempotencia;

/**
 * Acceso a los registros de idempotencia, que guardan las peticiones hechas
 * con la cabecera {@code Idempotency-Key} y su respuesta.
 */
public interface RegistroIdempotenciaRepository extends JpaRepository<RegistroIdempotencia, Long> {

	/**
	 * Busca la petición registrada con una clave para un usuario y operación.
	 *
	 * @param idUsuario Usuario que hizo la petición.
	 * @param operacion Operación protegida, por ejemplo CREAR_RESERVA.
	 * @param clave     Valor de la cabecera Idempotency-Key.
	 * @return El registro si la clave ya se usó.
	 */
	public Optional<RegistroIdempotencia> findByIdUsuarioAndOperacionAndClave(Long idUsuario, String operacion,
			String clave);
}
