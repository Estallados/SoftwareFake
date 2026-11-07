package co.edu.unbosque.marketplace.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.marketplace.entity.CodigoVerificacion;

/**
 * Almacén en memoria de los códigos de verificación de correo; no usa la base
 * de datos.
 * <p>
 * Guarda un solo código por usuario, porque cada código nuevo invalida el
 * anterior. Los códigos se pierden al reiniciar la aplicación (el usuario pide
 * otro) y no se comparten entre instancias.
 * </p>
 */
@Repository
public class CodigoVerificacionRepository {

	/** Cada cuánto se limpian los códigos usados o vencidos (5 minutos). */
	private static final long INTERVALO_LIMPIEZA_MS = 300_000;

	/** Código vigente de cada usuario. */
	private final Map<Long, CodigoVerificacion> codigos = new ConcurrentHashMap<>();

	/**
	 * Constructor de CodigoVerificacionRepository.
	 */
	public CodigoVerificacionRepository() {
	}
	
	/**
	 * Guarda el código como el único del usuario.
	 *
	 * @param codigo Código a guardar.
	 * @return El mismo código.
	 */
	public CodigoVerificacion save(CodigoVerificacion codigo) {
		codigos.put(codigo.getIdUsuario(), codigo);
		return codigo;
	}

	/**
	 * Último código sin usar del usuario (el único vigente).
	 *
	 * @param idUsuario Usuario dueño del código.
	 * @return El código más reciente sin usar, si existe.
	 */
	public Optional<CodigoVerificacion> findTopByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(Long idUsuario) {
		return Optional.ofNullable(codigos.get(idUsuario)).filter(codigo -> !Boolean.TRUE.equals(codigo.getUsado()));
	}

	/**
	 * Códigos que el usuario aún no ha usado; se invalidan al pedir uno nuevo.
	 *
	 * @param idUsuario Usuario dueño de los códigos.
	 * @return Códigos sin usar.
	 */
	public List<CodigoVerificacion> findByIdUsuarioAndUsadoFalse(Long idUsuario) {
		return findTopByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(idUsuario).stream().toList();
	}

	/**
	 * Devuelve Todos los códigos guardados.
	 *
	 * @return Todos los códigos guardados.
	 */
	public List<CodigoVerificacion> findAll() {
		return new ArrayList<>(codigos.values());
	}

	/**
	 * Devuelve Cantidad de códigos guardados.
	 *
	 * @return Cantidad de códigos guardados.
	 */
	public long count() {
		return codigos.size();
	}

	/** Borra todos los códigos. */
	public void deleteAll() {
		codigos.clear();
	}

	/** Quita de la memoria los códigos ya usados o vencidos. */
	@Scheduled(fixedDelay = INTERVALO_LIMPIEZA_MS)
	public void limpiar() {
		LocalDateTime ahora = LocalDateTime.now();
		codigos.values().removeIf(
				codigo -> Boolean.TRUE.equals(codigo.getUsado()) || codigo.getFechaExpiracion().isBefore(ahora));
	}
}
