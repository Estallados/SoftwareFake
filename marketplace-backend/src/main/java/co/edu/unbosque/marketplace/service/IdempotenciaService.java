package co.edu.unbosque.marketplace.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.entity.EstadoIdempotencia;
import co.edu.unbosque.marketplace.entity.RegistroIdempotencia;
import co.edu.unbosque.marketplace.repository.RegistroIdempotenciaRepository;

/**
 * Idempotencia con la cabecera {@code Idempotency-Key}, para reservas, pagos y
 * cancelaciones: si el cliente reenvía la misma petición (por un doble clic o
 * un reintento), no se crea dos veces la reserva ni se cobra dos veces.
 * <p>
 * Uso desde un controlador:
 * {@code return idempotenciaService.ejecutar(idUsuario, "CREAR_RESERVA", clave, dto, () -> crearReserva(dto));}
 * </p>
 * <p>
 * Reglas: la primera vez con una clave ejecuta la acción y guarda su
 * respuesta. Con la misma clave y los mismos datos devuelve la respuesta
 * guardada sin volver a ejecutar la acción (con la cabecera
 * {@code Idempotent-Replayed: true}). Con la misma clave y datos distintos
 * responde 422. Si la primera petición sigue en curso responde 409. Si la
 * acción falla con una excepción o con un 5xx, la clave se libera para poder
 * reintentar.
 * </p>
 */
@Service
public class IdempotenciaService {

	/** Nombre de la cabecera que manda el cliente. */
	public static final String CABECERA = "Idempotency-Key";

	/** Cabecera que se agrega cuando se devuelve una respuesta guardada. */
	public static final String CABECERA_REPETIDA = "Idempotent-Replayed";

	/** Largo máximo de la clave. */
	private static final int MAX_CLAVE = 100;

	/** Log de la clase. */
	private static final Logger LOG = LoggerFactory.getLogger(IdempotenciaService.class);

	/** Repositorio de los registros de idempotencia. */
	private final RegistroIdempotenciaRepository registroRepo;

	/** Transacción independiente para registrar y liberar las claves. */
	private final TransactionTemplate transaccionPropia;

	/** Convierte las peticiones y respuestas a JSON. */
	private final ObjectMapper objectMapper;

	/**
	 * Crea el servicio y prepara la transacción propia (REQUIRES_NEW).
	 *
	 * @param registroRepo repositorio de registros de idempotencia
	 * @param transactionManager administrador de transacciones de Spring
	 * @param objectMapper conversor a JSON
	 */
	public IdempotenciaService(RegistroIdempotenciaRepository registroRepo,
			PlatformTransactionManager transactionManager, ObjectMapper objectMapper) {
		this.registroRepo = registroRepo;
		this.objectMapper = objectMapper;
		this.transaccionPropia = new TransactionTemplate(transactionManager);
		this.transaccionPropia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/**
	 * Ejecuta una acción como máximo una vez por clave.
	 *
	 * @param idUsuario Usuario autenticado (sale del token JWT).
	 * @param operacion Nombre de la operación, por ejemplo CREAR_RESERVA.
	 * @param clave     Valor de la cabecera Idempotency-Key.
	 * @param peticion  Cuerpo de la petición; se usa para detectar reusos con otros
	 *                  datos.
	 * @param accion    Lo que hay que hacer la primera vez.
	 * @return La respuesta de la acción, o la guardada si la clave ya se usó.
	 */
	public ResponseEntity<Object> ejecutar(Long idUsuario, String operacion, String clave, Object peticion,
			Supplier<ResponseEntity<?>> accion) {
		if (clave == null || clave.isBlank() || clave.length() > MAX_CLAVE) {
			return mensaje(HttpStatus.BAD_REQUEST,
					"La cabecera Idempotency-Key es obligatoria y debe tener entre 1 y 100 caracteres.");
		}

		String huella = huella(peticion);
		Optional<RegistroIdempotencia> existente = registroRepo.findByIdUsuarioAndOperacionAndClave(idUsuario,
				operacion, clave);
		if (existente.isPresent()) {
			return responderExistente(existente.get(), huella);
		}

		RegistroIdempotencia registro;
		try {
			registro = transaccionPropia
					.execute(estado -> registroRepo.saveAndFlush(nuevoRegistro(idUsuario, operacion, clave, huella)));
		} catch (DataIntegrityViolationException e) {
			// Otra petición con la misma clave se registró en el mismo instante
			return mensaje(HttpStatus.CONFLICT, "Ya hay una petición en proceso con esta Idempotency-Key.");
		}

		ResponseEntity<?> respuesta;
		try {
			respuesta = accion.get();
		} catch (RuntimeException e) {
			liberar(registro);
			throw e;
		}

		if (respuesta.getStatusCode().is5xxServerError()) {
			liberar(registro);
		} else {
			guardarRespuesta(registro, respuesta);
		}
		return ResponseEntity.status(respuesta.getStatusCode()).headers(respuesta.getHeaders())
				.body(respuesta.getBody());
	}

	/**
	 * Responde a una clave que ya se usó: 422 si los datos son otros, 409 si
	 * la petición sigue en curso, o la respuesta guardada si ya terminó.
	 *
	 * @param registro registro de la clave
	 * @param huella huella de la petición actual
	 * @return respuesta para el cliente
	 */
	private ResponseEntity<Object> responderExistente(RegistroIdempotencia registro, String huella) {
		if (!registro.getHuellaPeticion().equals(huella)) {
			return mensaje(HttpStatus.UNPROCESSABLE_ENTITY,
					"Esta Idempotency-Key ya se usó con una petición diferente.");
		}
		if (registro.getEstado() == EstadoIdempotencia.EN_PROCESO) {
			return mensaje(HttpStatus.CONFLICT, "La petición con esta Idempotency-Key todavía está en proceso.");
		}

		Object cuerpo = null;
		if (registro.getCuerpoRespuesta() != null) {
			try {
				cuerpo = objectMapper.readTree(registro.getCuerpoRespuesta());
			} catch (JsonProcessingException e) {
				LOG.error("No se pudo leer la respuesta guardada del registro {}", registro.getIdRegistro(), e);
			}
		}
		return ResponseEntity.status(registro.getCodigoRespuesta()).header(CABECERA_REPETIDA, "true")
				.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
	}

	/**
	 * Arma el registro de una clave nueva, en estado EN_PROCESO.
	 *
	 * @param idUsuario usuario
	 * @param operacion operación protegida
	 * @param clave valor de la cabecera
	 * @param huella huella de la petición
	 * @return registro sin guardar
	 */
	private static RegistroIdempotencia nuevoRegistro(Long idUsuario, String operacion, String clave, String huella) {
		RegistroIdempotencia registro = new RegistroIdempotencia();
		registro.setIdUsuario(idUsuario);
		registro.setOperacion(operacion);
		registro.setClave(clave);
		registro.setHuellaPeticion(huella);
		registro.setEstado(EstadoIdempotencia.EN_PROCESO);
		registro.setFechaCreacion(LocalDateTime.now());
		return registro;
	}

	/**
	 * Guarda la respuesta de la acción en el registro. Si no se puede guardar
	 * el cuerpo, igual marca el registro como completado, porque la acción ya
	 * se hizo.
	 *
	 * @param registro registro de la clave
	 * @param respuesta respuesta de la acción
	 */
	private void guardarRespuesta(RegistroIdempotencia registro, ResponseEntity<?> respuesta) {
		String cuerpo = null;
		try {
			cuerpo = respuesta.getBody() == null ? null : objectMapper.writeValueAsString(respuesta.getBody());
		} catch (JsonProcessingException e) {
			LOG.error("No se pudo guardar la respuesta de la operación {}", registro.getOperacion(), e);
		}
		try {
			completar(registro, respuesta, cuerpo);
		} catch (DataAccessException e) {
			// La acción ya se hizo (por ejemplo, la reserva quedó pagada): no se
			// convierte en error; se guarda el registro sin el cuerpo.
			LOG.error("No se pudo guardar la respuesta de la operación {}", registro.getOperacion(), e);
			completar(registro, respuesta, null);
		}
	}

	/**
	 * Marca el registro como completado con el código y el cuerpo de la
	 * respuesta. Se actualiza la fila recién leída en esta transacción, no la
	 * copia de la transacción anterior.
	 *
	 * @param registro registro de la clave
	 * @param respuesta respuesta de la acción
	 * @param cuerpo cuerpo de la respuesta en JSON; puede ser {@code null}
	 */
	private void completar(RegistroIdempotencia registro, ResponseEntity<?> respuesta, String cuerpo) {
		transaccionPropia
				.executeWithoutResult(estado -> registroRepo.findById(registro.getIdRegistro()).ifPresent(guardado -> {
					guardado.setEstado(EstadoIdempotencia.COMPLETADA);
					guardado.setCodigoRespuesta(respuesta.getStatusCode().value());
					guardado.setCuerpoRespuesta(cuerpo);
				}));
	}

	/**
	 * Borra el registro para que la clave se pueda volver a usar.
	 *
	 * @param registro registro a borrar
	 */
	private void liberar(RegistroIdempotencia registro) {
		transaccionPropia.executeWithoutResult(estado -> registroRepo.deleteById(registro.getIdRegistro()));
	}

	/**
	 * Calcula el SHA-256 del cuerpo de la petición, para detectar si la clave
	 * se reusa con otros datos.
	 *
	 * @param peticion cuerpo de la petición
	 * @return huella en hexadecimal
	 * @throws IllegalStateException si no se puede calcular
	 */
	private String huella(Object peticion) {
		try {
			byte[] datos = objectMapper.writeValueAsString(peticion).getBytes(StandardCharsets.UTF_8);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos));
		} catch (JsonProcessingException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("No se pudo calcular la huella de la petición", e);
		}
	}

	/**
	 * Arma una respuesta con el formato de la API.
	 *
	 * @param status estado HTTP
	 * @param texto mensaje para el usuario
	 * @return respuesta {@code {"message": texto}}
	 */
	private static ResponseEntity<Object> mensaje(HttpStatus status, String texto) {
		return ResponseEntity.status(status).body(Map.of("message", texto));
	}
}
