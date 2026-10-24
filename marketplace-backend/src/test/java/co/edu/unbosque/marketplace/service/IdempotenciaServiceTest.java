package co.edu.unbosque.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.repository.RegistroIdempotenciaRepository;

/**
 * Pruebas de {@link IdempotenciaService} contra H2, porque dependen del índice
 * único y de las transacciones propias.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:idempotencia;DB_CLOSE_DELAY=-1")
class IdempotenciaServiceTest {

	private static final String OPERACION = "OPERACION_PRUEBA";

	@Autowired
	private IdempotenciaService idempotenciaService;

	@Autowired
	private RegistroIdempotenciaRepository registroRepo;

	private AtomicInteger ejecuciones;

	@BeforeEach
	void preparar() {
		registroRepo.deleteAll();
		ejecuciones = new AtomicInteger();
	}

	private ResponseEntity<?> accionCreada() {
		int numero = ejecuciones.incrementAndGet();
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("numero", numero));
	}

	@Test
	void debeEjecutarUnaSolaVezYRepetirLaMismaRespuesta_CA140() {
		ResponseEntity<Object> primera = idempotenciaService.ejecutar(1L, OPERACION, "clave-1", Map.of("a", 1),
				this::accionCreada);
		ResponseEntity<Object> segunda = idempotenciaService.ejecutar(1L, OPERACION, "clave-1", Map.of("a", 1),
				this::accionCreada);

		assertEquals(1, ejecuciones.get());
		assertEquals(HttpStatus.CREATED, primera.getStatusCode());
		assertEquals(HttpStatus.CREATED, segunda.getStatusCode());
		assertEquals(1, ((JsonNode) segunda.getBody()).get("numero").asInt());
		assertEquals("true", segunda.getHeaders().getFirst(IdempotenciaService.CABECERA_REPETIDA));
		assertNull(primera.getHeaders().getFirst(IdempotenciaService.CABECERA_REPETIDA));
	}

	@Test
	void debeRechazarLaMismaClaveConOtraPeticion_CA141() {		idempotenciaService.ejecutar(1L, OPERACION, "clave-1", Map.of("a", 1), this::accionCreada);

		ResponseEntity<Object> respuesta = idempotenciaService.ejecutar(1L, OPERACION, "clave-1", Map.of("a", 2),
				this::accionCreada);

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, respuesta.getStatusCode());
		assertEquals(1, ejecuciones.get());
	}

	@Test
	void debeExigirUnaClaveValida_CA141() {		assertEquals(HttpStatus.BAD_REQUEST,
				idempotenciaService.ejecutar(1L, OPERACION, null, null, this::accionCreada).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST,
				idempotenciaService.ejecutar(1L, OPERACION, " ", null, this::accionCreada).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, idempotenciaService
				.ejecutar(1L, OPERACION, "x".repeat(101), null, this::accionCreada).getStatusCode());
		assertEquals(0, ejecuciones.get());
	}

	@Test
	void laClaveEsIndependientePorUsuarioYPorOperacion() {
		idempotenciaService.ejecutar(1L, OPERACION, "clave-1", null, this::accionCreada);
		idempotenciaService.ejecutar(2L, OPERACION, "clave-1", null, this::accionCreada);
		idempotenciaService.ejecutar(1L, "OTRA_OPERACION", "clave-1", null, this::accionCreada);

		assertEquals(3, ejecuciones.get());
	}

	@Test
	void debeLiberarLaClaveSiLaAccionFallaParaPoderReintentar() {
		assertThrows(IllegalStateException.class, () -> idempotenciaService.ejecutar(1L, OPERACION, "clave-1", null,
				() -> {
					throw new IllegalStateException("falla simulada");
				}));

		ResponseEntity<Object> reintento = idempotenciaService.ejecutar(1L, OPERACION, "clave-1", null,
				this::accionCreada);
		assertEquals(HttpStatus.CREATED, reintento.getStatusCode());
		assertEquals(1, ejecuciones.get());
	}

	@Test
	void noDebeGuardarRespuestasDeErrorDelServidor() {
		idempotenciaService.ejecutar(1L, OPERACION, "clave-1", null,
				() -> ResponseEntity.internalServerError().body(Map.of("message", "error")));

		assertEquals(0, registroRepo.count());
	}
}
