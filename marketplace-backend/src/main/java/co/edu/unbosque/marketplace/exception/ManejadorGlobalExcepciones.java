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
 * Manejador global de los errores técnicos de la API.
 * <p>
 * Las reglas de negocio las resuelven los servicios con sus códigos de
 * resultado; aquí llega todo lo que los controladores no controlan: JSON mal
 * formado, parámetros o cabeceras faltantes, permisos insuficientes, archivos
 * muy grandes, problemas con la base de datos y errores inesperados. Siempre
 * respondo con el mismo formato, {@code {"message": "..."}}, y nunca dejo que
 * se vea la traza del error.
 * </p>
 * <p>
 * Los accesos denegados y los errores inesperados quedan además registrados en
 * la auditoría.
 * </p>
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

	/** Log de la clase. */
	private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

	/** Servicio donde quedan los accesos denegados y los errores internos. */
	private final AuditoriaService auditoriaService;

	/**
	 * Crea el manejador.
	 *
	 * @param auditoriaService servicio de auditoría
	 */
	public ManejadorGlobalExcepciones(AuditoriaService auditoriaService) {
		this.auditoriaService = auditoriaService;
	}

	/**
	 * El cuerpo de la petición no se pudo leer como JSON (sintaxis inválida o un
	 * campo con un tipo que no corresponde).
	 *
	 * @return 400 con el mensaje
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> cuerpoInvalido() {
		return mensaje(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido.");
	}

	/**
	 * Falta una cabecera obligatoria, por ejemplo {@code Idempotency-Key}.
	 *
	 * @param e excepción con el nombre de la cabecera
	 * @return 400 indicando qué cabecera falta
	 */
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<Map<String, String>> cabeceraFaltante(MissingRequestHeaderException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "Falta la cabecera " + e.getHeaderName() + ".");
	}

	/**
	 * Falta un parámetro obligatorio de la URL.
	 *
	 * @param e excepción con el nombre del parámetro
	 * @return 400 indicando qué parámetro falta
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, String>> parametroFaltante(MissingServletRequestParameterException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "Falta el parámetro " + e.getParameterName() + ".");
	}

	/**
	 * Un parámetro de la ruta o de la URL no se pudo convertir al tipo esperado
	 * (por ejemplo, un id que no es número o una fecha mal escrita).
	 *
	 * @param e excepción con el nombre del parámetro
	 * @return 400 indicando qué valor no es válido
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, String>> tipoInvalido(MethodArgumentTypeMismatchException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "El valor de " + e.getName() + " no es válido.");
	}

	/**
	 * Se llamó una ruta con un método HTTP que no tiene (por ejemplo, GET en lugar
	 * de POST).
	 *
	 * @param e excepción con el método usado
	 * @return 405 con el método rechazado
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
		return mensaje(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " no permitido en esta ruta.");
	}

	/**
	 * El cuerpo llegó con un {@code Content-Type} que la ruta no acepta. Le digo al
	 * cliente cuál debe usar: multipart para las rutas de archivos y JSON para el
	 * resto.
	 *
	 * @param e excepción con los tipos que acepta la ruta
	 * @return 415 con el tipo esperado
	 */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<Map<String, String>> tipoContenidoNoSoportado(HttpMediaTypeNotSupportedException e) {
		String esperado = e.getSupportedMediaTypes().stream().anyMatch(MediaType.MULTIPART_FORM_DATA::includes)
				? "multipart/form-data"
				: "application/json";
		return mensaje(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo debe enviarse como " + esperado + ".");
	}

	/**
	 * Un archivo supera el tamaño máximo configurado para las subidas.
	 *
	 * @return 413 con el límite permitido
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> archivoMuyGrande() {
		return mensaje(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño permitido (5 MB por imagen).");
	}

	/**
	 * No se pudo guardar una imagen en Cloudinary.
	 *
	 * @param e excepción con el mensaje para el usuario
	 * @return 503 con el mensaje de la excepción
	 */
	@ExceptionHandler(AlmacenamientoNoDisponibleException.class)
	public ResponseEntity<Map<String, String>> almacenamientoNoDisponible(AlmacenamientoNoDisponibleException e) {
		return mensaje(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
	}

	/**
	 * La petición multipart no trae el campo con los archivos.
	 *
	 * @param e excepción con el nombre del campo
	 * @return 400 indicando qué campo falta
	 */
	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<Map<String, String>> parteFaltante(MissingServletRequestPartException e) {
		return mensaje(HttpStatus.BAD_REQUEST, "Falta el campo " + e.getRequestPartName() + " con los archivos.");
	}

	/**
	 * La petición de archivos no es un multipart válido.
	 *
	 * @return 400 con el formato esperado
	 */
	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<Map<String, String>> multipartInvalido() {
		return mensaje(HttpStatus.BAD_REQUEST, "Los archivos deben enviarse como multipart/form-data.");
	}

	/**
	 * La ruta pedida no existe en la API.
	 *
	 * @return 404 con el mensaje
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> rutaNoEncontrada() {
		return mensaje(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.");
	}

	/**
	 * El cliente pidió un formato que la API no entrega (una cabecera
	 * {@code Accept} distinta de JSON). Respondo sin cuerpo, porque un JSON
	 * tampoco lo aceptaría.
	 *
	 * @return 406 sin cuerpo
	 */
	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<Void> formatoNoAceptable() {
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}

	/**
	 * El cliente cerró la conexión antes de recibir la respuesta (cambió de página
	 * o canceló la petición). No hay a quién responder y no es un error del
	 * servidor, así que solo lo dejo en el log de depuración.
	 *
	 * @param e       excepción de la conexión cerrada
	 * @param request petición que se estaba atendiendo
	 */
	@ExceptionHandler(AsyncRequestNotUsableException.class)
	public void clienteDesconectado(AsyncRequestNotUsableException e, HttpServletRequest request) {
		LOG.debug("El cliente cerró la conexión en {} {}: {}", request.getMethod(), request.getRequestURI(),
				e.getMessage());
	}

	/**
	 * Dos peticiones simultáneas chocaron con un índice único (por ejemplo, por un
	 * doble clic). Las demás violaciones de integridad sí son errores del
	 * servidor y se tratan como inesperadas.
	 *
	 * @param e       excepción de la base de datos
	 * @param request petición que se estaba atendiendo
	 * @return 409 si es un duplicado; si no, la respuesta de
	 *         {@link #errorInesperado(Exception, HttpServletRequest)}
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> conflictoDeDatos(DataIntegrityViolationException e,
			HttpServletRequest request) {
		if (!esDuplicado(e)) {
			return errorInesperado(e, request);
		}
		return mensaje(HttpStatus.CONFLICT, "El registro ya existe. Actualiza la página e intenta de nuevo.");
	}

	/**
	 * Otra operación modificó o bloqueó los mismos datos al mismo tiempo.
	 *
	 * @return 409 pidiendo que se intente de nuevo
	 */
	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<Map<String, String>> conflictoConcurrente() {
		return mensaje(HttpStatus.CONFLICT, "Otra operación modificó estos datos al mismo tiempo. Intenta de nuevo.");
	}

	/**
	 * La base de datos no responde. Es un problema temporal, no de la petición.
	 * No lo registro en la auditoría porque la auditoría se guarda justamente en
	 * esa base.
	 *
	 * @param e       excepción de la conexión
	 * @param request petición que se estaba atendiendo
	 * @return 503 pidiendo intentar más tarde
	 */
	@ExceptionHandler({ DataAccessResourceFailureException.class, CannotCreateTransactionException.class })
	public ResponseEntity<Map<String, String>> baseNoDisponible(Exception e, HttpServletRequest request) {
		// Sin auditoría: se guarda en la misma base que no responde
		LOG.error("Base de datos no disponible en {} {}", request.getMethod(), request.getRequestURI(), e);
		return mensaje(HttpStatus.SERVICE_UNAVAILABLE,
				"El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.");
	}

	/**
	 * El usuario no tiene el rol que pide la ruta. Lo lanza {@code @PreAuthorize}
	 * y queda registrado en la auditoría.
	 *
	 * @param request petición rechazada
	 * @return 403 con el mensaje
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, String>> accesoDenegado(HttpServletRequest request) {
		auditoriaService.registrar(idActorActual(), AuditoriaService.ACCESO_DENEGADO, null, null,
				ResultadoAuditoria.RECHAZADO, request.getMethod() + " " + request.getRequestURI());
		return mensaje(HttpStatus.FORBIDDEN, "No tienes permisos para esta operación");
	}

	/**
	 * Cualquier otro error. Si es una excepción de Spring que ya trae un estado 4xx
	 * (la culpa es de la petición), respeto ese estado. Si no, es un error
	 * inesperado: lo dejo en el log con su traza, lo registro en la auditoría y
	 * respondo un mensaje genérico.
	 *
	 * @param e       excepción no controlada
	 * @param request petición que se estaba atendiendo
	 * @return el 4xx de la excepción o un 500 genérico
	 */
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

	/**
	 * Indica si la causa de la excepción es la violación de un índice único
	 * (SQLState 23505, igual en PostgreSQL y en H2).
	 *
	 * @param e excepción a revisar, con toda su cadena de causas
	 * @return {@code true} si en alguna causa aparece el SQLState 23505
	 */
	private static boolean esDuplicado(Throwable e) {
		for (Throwable causa = e; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Id del usuario de la petición actual, para la auditoría.
	 *
	 * @return id del usuario autenticado, o {@code null} si no hay sesión
	 */
	private static Long idActorActual() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion != null && autenticacion.getPrincipal() instanceof Usuario usuario) {
			return usuario.getIdUsuario();
		}
		return null;
	}

	/**
	 * Arma la respuesta con el formato de la API.
	 *
	 * @param status estado HTTP
	 * @param texto  mensaje para el usuario
	 * @return respuesta {@code {"message": texto}}
	 */
	private static ResponseEntity<Map<String, String>> mensaje(HttpStatus status, String texto) {
		return ResponseEntity.status(status).body(Map.of("message", texto));
	}
}
