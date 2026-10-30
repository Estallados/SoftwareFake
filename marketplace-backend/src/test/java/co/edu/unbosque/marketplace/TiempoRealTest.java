package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Mensajes y notificaciones en tiempo real por WebSocket + STOMP, con un
 * cliente STOMP real contra el servidor levantado en un puerto aleatorio.
 *
 * @see "RF-003, RF-029"
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"spring.datasource.url=jdbc:h2:mem:tiemporeal;DB_CLOSE_DELAY=-1",
		"app.admin.password=Admin#Prueba1" })
class TiempoRealTest extends PruebaIntegracionBase {

	private static final long ESPERA = 5;

	@LocalServerPort
	private int puerto;

	@Autowired
	private SimpUserRegistry usuariosConectados;

	private WebSocketStompClient cliente;

	@BeforeEach
	void crearCliente() {
		cliente = new WebSocketStompClient(new StandardWebSocketClient());
		MappingJackson2MessageConverter conversor = new MappingJackson2MessageConverter();
		conversor.setObjectMapper(json);
		cliente.setMessageConverter(conversor);
	}

	@AfterEach
	void detenerCliente() {
		cliente.stop();
	}

	/** Conecta; los frames ERROR del servidor completan {@code error}. */
	private CompletableFuture<StompSession> conectar(String token, CompletableFuture<String> error) {
		StompHeaders conexion = new StompHeaders();
		if (token != null) {
			conexion.add("Authorization", "Bearer " + token);
		}
		return cliente.connectAsync("ws://localhost:" + puerto + "/ws", new WebSocketHttpHeaders(), conexion,
				new StompSessionHandlerAdapter() {
					@Override
					public void handleFrame(StompHeaders headers, Object payload) {
						error.complete(headers.getFirst("message"));
					}
				});
	}

	private StompSession conectar(Sesion usuario) throws Exception {
		return conectar(usuario.token(), new CompletableFuture<>()).get(ESPERA, TimeUnit.SECONDS);
	}

	private static BlockingQueue<JsonNode> suscribir(StompSession sesion, String destino) {
		BlockingQueue<JsonNode> cola = new LinkedBlockingQueue<>();
		sesion.subscribe(destino, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return JsonNode.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				cola.add((JsonNode) payload);
			}
		});
		return cola;
	}

	/** El broker no confirma las suscripciones: se espera a verlas registradas. */
	private void esperarSuscripciones(Sesion usuario, int cantidad) throws InterruptedException {
		for (int intento = 0; intento < 50; intento++) {
			SimpUser conectado = usuariosConectados.getUser(String.valueOf(usuario.id()));
			if (conectado != null && conectado.getSessions().stream()
					.mapToLong(s -> s.getSubscriptions().size()).sum() >= cantidad) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Las suscripciones no se registraron");
	}

	private static JsonNode siguiente(BlockingQueue<JsonNode> cola) throws InterruptedException {
		JsonNode dato = cola.poll(ESPERA, TimeUnit.SECONDS);
		assertNotNull(dato, "No llegó el mensaje esperado por WebSocket");
		return dato;
	}

	@Test
	void sinTokenOTokenInvalidoNoConecta() {
		assertThrows(ExecutionException.class,
				() -> conectar(null, new CompletableFuture<>()).get(ESPERA, TimeUnit.SECONDS));
		assertThrows(ExecutionException.class,
				() -> conectar("token.falso.123", new CompletableFuture<>()).get(ESPERA, TimeUnit.SECONDS));
	}

	@Test
	void mensajesYNotificacionesLleganEnTiempoReal() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Sesion huesped = registrar("HUESPED");
		Long idAlojamiento = crearAlojamientoActivo(anfitrion);
		StompSession sesion = conectar(anfitrion);
		BlockingQueue<JsonNode> mensajes = suscribir(sesion, "/user/queue/mensajes");
		BlockingQueue<JsonNode> notificaciones = suscribir(sesion, "/user/queue/notificaciones");
		esperarSuscripciones(anfitrion, 2);

		// El huésped escribe por REST y al anfitrión le llegan el mensaje y la notificación
		Long idConversacion = llamar(post("/conversaciones"), huesped.token(),
				Map.of("idAlojamiento", idAlojamiento, "mensaje", "Hola, ¿está disponible?"), 201)
				.get("idConversacion").asLong();
		JsonNode recibido = siguiente(mensajes);
		assertEquals("Hola, ¿está disponible?", recibido.get("contenido").asText());
		assertEquals(idConversacion, recibido.get("idConversacion").asLong());
		assertEquals(huesped.id(), recibido.get("idUsuario").asLong());
		assertEquals("MENSAJE_RECIBIDO", siguiente(notificaciones).get("tipo").asText());

		// El anfitrión responde por REST y también le llega su propio mensaje (otras pestañas)
		llamar(post("/conversaciones/" + idConversacion + "/mensajes"), anfitrion.token(),
				Map.of("contenido", "Sí, te esperamos"), 201);
		assertEquals(anfitrion.id(), siguiente(mensajes).get("idUsuario").asLong());
	}

	@Test
	void noSePuedeEscucharNiEscribirEnColasAjenas() throws Exception {
		Sesion victima = registrar("HUESPED");
		StompSession sesionVictima = conectar(victima);
		BlockingQueue<JsonNode> mensajesVictima = suscribir(sesionVictima, "/user/queue/mensajes");
		esperarSuscripciones(victima, 1);

		// Enviar directo a la cola de otro usuario se rechaza y no le llega nada
		CompletableFuture<String> errorEnvio = new CompletableFuture<>();
		StompSession atacante = conectar(registrar("HUESPED").token(), errorEnvio).get(ESPERA, TimeUnit.SECONDS);
		atacante.send("/user/" + victima.id() + "/queue/mensajes", Map.of("contenido", "falso"));
		assertNotNull(errorEnvio.get(ESPERA, TimeUnit.SECONDS));
		assertNull(mensajesVictima.poll(1, TimeUnit.SECONDS));

		// Suscribirse fuera de /user/queue/ también se rechaza
		CompletableFuture<String> errorSuscripcion = new CompletableFuture<>();
		StompSession curioso = conectar(registrar("HUESPED").token(), errorSuscripcion).get(ESPERA,
				TimeUnit.SECONDS);
		curioso.subscribe("/queue/mensajes", new StompSessionHandlerAdapter() {
		});
		assertNotNull(errorSuscripcion.get(ESPERA, TimeUnit.SECONDS));
	}
}
