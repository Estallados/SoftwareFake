package co.edu.unbosque.marketplace.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejador global de errores técnicos. Las reglas de negocio se resuelven con
 * los códigos int de los services; aquí llega lo que los controllers no
 * controlan (JSON mal formado, parámetros faltantes, permisos, errores
 * inesperados) y siempre se responde con el mismo formato
 * {@code {"message": "..."}}.
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
	public ResponseEntity<Map<String, String>> tipoContenidoNoSoportado() {
		return mensaje(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo debe enviarse como application/json.");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> rutaNoEncontrada() {
		return mensaje(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.");
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
		LOG.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), e);
		auditoriaService.registrar(idActorActual(), AuditoriaService.ERROR_INTERNO, null, null,
				ResultadoAuditoria.ERROR,
				request.getMethod() + " " + request.getRequestURI() + ": " + e.getClass().getSimpleName());
		return mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo.");
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
