package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.util.TotpUtil;

/** Módulo 9: favoritos, mensajería y segundo factor (TOTP). */
class OpcionalesTest extends PruebaIntegracionBase {

	// ----- favoritos -----

	@Test
	void unaListaSinFavoritosEstaVacia_CA198() throws Exception {
		JsonNode favoritos = llamar(get("/favoritos"), registrar("HUESPED").token(), null, 200);
		assertEquals(0, favoritos.get("totalElementos").asInt());
		assertTrue(favoritos.get("contenido").isEmpty());
	}

	@Test
	void agregarFavoritoNoDuplicaYSoloMuestraLosPropios_CA170_CA172_CA173() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");

		// 201 al crearlo; 200 si ya estaba (devuelve el mismo)
		JsonNode primero = llamar(post("/favoritos/" + id), huesped.token(), null, 201);
		JsonNode segundo = llamar(post("/favoritos/" + id), huesped.token(), null, 200);

		assertEquals(primero.get("idFavorito").asLong(), segundo.get("idFavorito").asLong());
		JsonNode lista = llamar(get("/favoritos"), huesped.token(), null, 200);
		assertEquals(1, lista.get("totalElementos").asInt());
		assertEquals(id, lista.get("contenido").get(0).get("alojamiento").get("idAlojamiento").asLong());
		assertEquals(0, llamar(get("/favoritos"), registrar("HUESPED").token(), null, 200).get("totalElementos")
				.asInt());
		llamar(post("/favoritos/999999"), huesped.token(), null, 404);
	}

	@Test
	void eliminarUnFavoritoLoQuitaDeLaLista_CA171() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		llamar(post("/favoritos/" + id), huesped.token(), null, 201);

		llamar(delete("/favoritos/" + id), huesped.token(), null, 200);

		assertEquals(0, llamar(get("/favoritos"), huesped.token(), null, 200).get("totalElementos").asInt());
		llamar(delete("/favoritos/" + id), huesped.token(), null, 404);
	}

	// ----- mensajería -----

	@Test
	void elHuespedIniciaUnaConversacionYElAnfitrionResponde_CA165_CA166_CA168_CA169() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");

		JsonNode conversacion = llamar(post("/conversaciones"), huesped.token(),
				Map.of("idAlojamiento", id, "mensaje", "¿Tiene parqueadero?"), 201);
		long idConversacion = conversacion.get("idConversacion").asLong();
		assertEquals(anfitrion.id(), conversacion.get("idAnfitrion").asLong());
		assertEquals("Usuario HUESPED", conversacion.get("nombreUsuario").asText());
		assertEquals("Usuario ANFITRION", conversacion.get("nombreAnfitrion").asText());
		JsonNode lista = llamar(get("/conversaciones"), anfitrion.token(), null, 200);
		assertEquals("Usuario HUESPED", lista.get("contenido").get(0).get("nombreUsuario").asText());

		JsonNode respuesta = llamar(post("/conversaciones/" + idConversacion + "/mensajes"), anfitrion.token(),
				Map.of("contenido", "Sí, cubierto"), 201);
		assertEquals(anfitrion.id(), respuesta.get("idUsuario").asLong());
		assertEquals("Usuario ANFITRION", respuesta.get("nombreUsuario").asText());
		assertTrue(respuesta.has("fechaEnvio"));

		JsonNode mensajes = llamar(get("/conversaciones/" + idConversacion + "/mensajes"), huesped.token(), null, 200);
		assertEquals(2, mensajes.get("totalElementos").asInt());
		assertEquals(huesped.id(), mensajes.get("contenido").get(0).get("idUsuario").asLong());
		assertEquals("Usuario HUESPED", mensajes.get("contenido").get(0).get("nombreUsuario").asText());
		assertEquals("Sí, cubierto", mensajes.get("contenido").get(1).get("contenido").asText());
	}

	@Test
	void quienNoParticipaNoPuedeVerNiEscribirEnLaConversacion_CA167() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		long idConversacion = llamar(post("/conversaciones"), huesped.token(), Map.of("idAlojamiento", id), 201)
				.get("idConversacion").asLong();
		Sesion intruso = registrar("HUESPED");

		llamar(get("/conversaciones/" + idConversacion + "/mensajes"), intruso.token(), null, 403);
		llamar(post("/conversaciones/" + idConversacion + "/mensajes"), intruso.token(), Map.of("contenido", "Hola"),
				403);
	}

	@Test
	void retomarUnaConversacionResponde200YConElPropioAlojamiento403() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		long creada = llamar(post("/conversaciones"), huesped.token(), Map.of("idAlojamiento", id), 201)
				.get("idConversacion").asLong();

		long retomada = llamar(post("/conversaciones"), huesped.token(),
				Map.of("idAlojamiento", id, "mensaje", "Hola de nuevo"), 200).get("idConversacion").asLong();

		assertEquals(creada, retomada);
		llamar(post("/conversaciones"), anfitrion.token(), Map.of("idAlojamiento", id), 403);
	}

	// ----- segundo factor -----

	@Test
	void conSegundoFactorElLoginPideElCodigoYSoloAceptaUnoValido_CA174_CA175_CA176_CA178() throws Exception {
		Sesion usuario = registrar("HUESPED");
		String secreto = llamar(post("/usuarios/segundofactor/activar"), usuario.token(), null, 200).get("secreto")
				.asText();
		llamar(post("/usuarios/segundofactor/confirmar"), usuario.token(), Map.of("codigo", "000000"), 400);
		llamar(post("/usuarios/segundofactor/confirmar"), usuario.token(),
				Map.of("codigo", TotpUtil.codigo(secreto, Instant.now())), 200);

		JsonNode sinCodigo = llamar(post("/auth/login"), null,
				Map.of("correo", usuario.correo(), "contrasenia", CLAVE), 401);
		assertTrue(sinCodigo.get("requiereSegundoFactor").asBoolean());
		assertTrue(!sinCodigo.has("token"));

		llamar(post("/auth/login"), null, Map.of("correo", usuario.correo(), "contrasenia", CLAVE, "codigo", "123456"),
				401);
		JsonNode valido = llamar(post("/auth/login"), null, Map.of("correo", usuario.correo(), "contrasenia", CLAVE,
				"codigo", TotpUtil.codigo(secreto, Instant.now())), 200);
		assertTrue(valido.has("token"));
	}

	@Test
	void sinSegundoFactorElLoginSigueNormal_CA177() throws Exception {
		Sesion usuario = registrar("HUESPED");
		JsonNode sinCodigo = llamar(post("/auth/login"), null,
				Map.of("correo", usuario.correo(), "contrasenia", CLAVE), 401);
		assertTrue(sinCodigo.get("requiereCodigoCorreo").asBoolean());
		assertTrue(!sinCodigo.has("token"));

		llamar(post("/auth/login"), null, Map.of("correo", usuario.correo(), "contrasenia", CLAVE, "codigo", "000000"),
				401);
		JsonNode login = login(usuario.correo(), CLAVE);
		assertTrue(login.has("token"));
	}
}
