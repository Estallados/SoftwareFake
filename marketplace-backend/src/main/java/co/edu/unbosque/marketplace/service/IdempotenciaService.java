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
 * Idempotencia por la cabecera {@code Idempotency-Key}, reutilizable en
 * reservas, pagos y cancelaciones.
 * <p>
 * Uso desde un controller:
 * </p>
 *
 * <pre>
 * return idempotenciaService.ejecutar(idUsuario, "CREAR_RESERVA", clave, dto, () -&gt; crearReserva(dto));
 * </pre>
 * <p>
 * Reglas:
 * </p>
 * <ul>
 * <li>Primera vez con una clave: ejecuta la acción y guarda su respuesta.</li>
 * <li>Misma clave y mismos datos: devuelve la respuesta guardada sin volver a
 * ejecutar la acción (con la cabecera {@code Idempotent-Replayed: true}).</li>
 * <li>Misma clave con datos distintos: 422.</li>
 * <li>Misma clave mientras la primera petición sigue en curso: 409.</li>
 * <li>Si la acción falla con excepción o con un 5xx, la clave se libera para
 * poder reintentar.</li>
 * </ul>
 *
 * @see "RNF-013, RNF-014"
 */
@Service
public class IdempotenciaService {

	public static final String CABECERA = "Idempotency-Key";
	public static final String CABECERA_REPETIDA = "Idempotent-Replayed";

	private static final int MAX_CLAVE = 100;

	private static final Logger LOG = LoggerFactory.getLogger(IdempotenciaService.class);

	private final RegistroIdempotenciaRepository registroRepo;
	private final TransactionTemplate transaccionPropia;
	private final ObjectMapper objectMapper;

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
	 * Marca el registro como completado. Se actualiza la fila recién leída en
	 * esta transacción, no la copia de la transacción anterior.
	 */
	private void completar(RegistroIdempotencia registro, ResponseEntity<?> respuesta, String cuerpo) {
		transaccionPropia
				.executeWithoutResult(estado -> registroRepo.findById(registro.getIdRegistro()).ifPresent(guardado -> {
					guardado.setEstado(EstadoIdempotencia.COMPLETADA);
					guardado.setCodigoRespuesta(respuesta.getStatusCode().value());
					guardado.setCuerpoRespuesta(cuerpo);
				}));
	}

	private void liberar(RegistroIdempotencia registro) {
		transaccionPropia.executeWithoutResult(estado -> registroRepo.deleteById(registro.getIdRegistro()));
	}

	private String huella(Object peticion) {
		try {
			byte[] datos = objectMapper.writeValueAsString(peticion).getBytes(StandardCharsets.UTF_8);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos));
		} catch (JsonProcessingException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("No se pudo calcular la huella de la petición", e);
		}
	}

	private static ResponseEntity<Object> mensaje(HttpStatus status, String texto) {
		return ResponseEntity.status(status).body(Map.of("message", texto));
	}
}
