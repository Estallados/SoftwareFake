package co.edu.unbosque.marketplace.exception;

import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejador global de errores técnicos. Las reglas de negocio se resuelven con
 * los códigos int de los services; aquí llega lo que los controllers no
 * controlan (JSON mal formado, parámetros faltantes, permisos, errores
 * inesperados) y siempre se responde con el mismo formato {@code {"message":
 * "..."}}.
 * <p>
 * Los accesos denegados y los errores inesperados quedan en la auditoría.
 * </p>
 *
 * @see "RNF-004, RNF-024"
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

	private final AuditoriaService auditoriaService;

	public ManejadorGlobalExcepciones(AuditoriaService auditoriaService) {
		this.auditoriaService = auditoriaService;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> cuerpoInvalido() {
		return mensaje(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido.");
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<Map<String, String>> cabeceraFaltante(MissingRequestHeaderException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "Falta la cabecera " + e.getHeaderName() + ".");
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, String>> parametroFaltante(MissingServletRequestParameterException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "Falta el parámetro " + e.getParameterName() + ".");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, String>> tipoInvalido(MethodArgumentTypeMismatchException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "El valor de " + e.getName() + " no es válido.");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
		return mensaje(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " no permitido en esta ruta.");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<Map<String, String>> tipoContenidoNoSoportado(HttpMediaTypeNotSupportedException e) {
		String esperado = e.getSupportedMediaTypes().stream().anyMatch(MediaType.MULTIPART_FORM_DATA::includes)
				? "multipart/form-data"
				: "application/json";
		return mensaje(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo debe enviarse como " + esperado + ".");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> archivoMuyGrande() {
		return mensaje(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño permitido (5 MB por imagen).");
	}

	@ExceptionHandler(AlmacenamientoNoDisponibleException.class)
	public ResponseEntity<Map<String, String>> almacenamientoNoDisponible(AlmacenamientoNoDisponibleException e) {
		return mensaje(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<Map<String, String>> parteFaltante(MissingServletRequestPartException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "Falta el campo " + e.getRequestPartName() + " con los archivos.");
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<Map<String, String>> multipartInvalido() {
		return mensaje(HttpStatus.BAD_REQUEST, "Los archivos deben enviarse como multipart/form-data.");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> rutaNoEncontrada() {
		return mensaje(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.");
	}

	/**
	 * El cliente pidió un formato que la API no entrega (Accept distinto de JSON).
	 * Va sin cuerpo: un JSON tampoco lo aceptaría.
	 */
	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<Void> formatoNoAceptable() {
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}

	/**
	 * El cliente cerró la conexión antes de recibir la respuesta (cambió de página,
	 * canceló la petición). No hay a quién responder y no es un error del servidor.
	 */
	@ExceptionHandler(AsyncRequestNotUsableException.class)
	public void clienteDesconectado(AsyncRequestNotUsableException e, HttpServletRequest request) {
		LOG.debug("El cliente cerró la conexión en {} {}: {}", request.getMethod(), request.getRequestURI(),
				e.getMessage());
	}

	/**
	 * Dos peticiones simultáneas chocaron con un índice único (por ejemplo, un doble
	 * clic). Otras violaciones de integridad son errores del servidor.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> conflictoDeDatos(DataIntegrityViolationException e,
			HttpServletRequest request) {
		if (!esDuplicado(e)) {
			return errorInesperado(e, request);
		}
		return mensaje(HttpStatus.CONFLICT, "El registro ya existe. Actualiza la página e intenta de nuevo.");
	}

	/** Otra operación modificó o bloqueó los mismos datos al mismo tiempo. */
	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<Map<String, String>> conflictoConcurrente() {
		return mensaje(HttpStatus.CONFLICT, "Otra operación modificó estos datos al mismo tiempo. Intenta de nuevo.");
	}

	/** La base de datos no responde: es temporal, no un error de la petición. */
	@ExceptionHandler({ DataAccessResourceFailureException.class, CannotCreateTransactionException.class })
	public ResponseEntity<Map<String, String>> baseNoDisponible(Exception e, HttpServletRequest request) {
		// Sin auditoría: se guarda en la misma base que no responde
		LOG.error("Base de datos no disponible en {} {}", request.getMethod(), request.getRequestURI(), e);
		return mensaje(HttpStatus.SERVICE_UNAVAILABLE,
				"El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.");
	}

	/** Lo lanza {@code @PreAuthorize} cuando el rol no alcanza. */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, String>> accesoDenegado(HttpServletRequest request) {
		auditoriaService.registrar(idActorActual(), AuditoriaService.ACCESO_DENEGADO, null, null,
				ResultadoAuditoria.RECHAZADO, request.getMethod() + " " + request.getRequestURI());
		return mensaje(HttpStatus.FORBIDDEN, "No tienes permisos para esta operación");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> errorInesperado(Exception e, HttpServletRequest request) {
		// Excepciones de Spring que ya traen su estado (por ejemplo ResponseStatusException):
		// un 4xx es culpa de la petición y se respeta; solo los 5xx son errores inesperados
		if (e instanceof ErrorResponse respuesta && respuesta.getStatusCode().is4xxClientError()) {
			return mensaje(HttpStatus.valueOf(respuesta.getStatusCode().value()),
					"La petición no es válida para esta ruta.");
		}
		LOG.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), e);
		auditoriaService.registrar(idActorActual(), AuditoriaService.ERROR_INTERNO, null, null,
				ResultadoAuditoria.ERROR,
				request.getMethod() + " " + request.getRequestURI() + ": " + e.getClass().getSimpleName());
		return mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo.");
	}

	/** SQLState 23505: violación de un índice único (PostgreSQL y H2). */
	private static boolean esDuplicado(Throwable e) {
		for (Throwable causa = e; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

	private static Long idActorActual() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion != null && autenticacion.getPrincipal() instanceof Usuario usuario) {
			return usuario.getIdUsuario();
		}
		return null;
	}

	private static ResponseEntity<Map<String, String>> mensaje(HttpStatus status, String texto) {
		return ResponseEntity.status(status).body(Map.of("message", texto));
	}
}
