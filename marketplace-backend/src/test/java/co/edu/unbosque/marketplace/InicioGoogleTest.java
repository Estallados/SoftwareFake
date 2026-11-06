package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.security.GoogleTokenVerificador;
import co.edu.unbosque.marketplace.util.TotpUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Inicio de sesión con Google. Los ID token se firman con una clave RSA de
 * prueba que reemplaza a las claves públicas de Google.
 */
@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:google;DB_CLOSE_DELAY=-1",
		"app.admin.password=Admin#Prueba1" })
@AutoConfigureMockMvc
class InicioGoogleTest {

	private static final String CLIENTE = "cliente-prueba.apps.googleusercontent.com";
	private static final String ID_CLAVE = "clave-prueba";
	private static final KeyPair CLAVES = generarClaves();
	private static final KeyPair OTRAS_CLAVES = generarClaves();
	private static final AtomicInteger CONTADOR = new AtomicInteger();

	@TestConfiguration
	static class GoogleDePrueba {
		@Bean
		@Primary
		GoogleTokenVerificador verificadorDePrueba() {
			return new GoogleTokenVerificador(CLIENTE, id -> ID_CLAVE.equals(id) ? CLAVES.getPublic() : null);
		}
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@MockBean
	private JavaMailSender mailSender;

	private static KeyPair generarClaves() {
		try {
			KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
			generador.initialize(2048);
			return generador.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String correoNuevo() {
		return "google" + CONTADOR.incrementAndGet() + "@gmail.com";
	}

	/** ID token como el que entrega Google, con los cambios indicados. */
	private static String token(String correo, Map<String, Object> cambios, PrivateKey clave) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("iss", "https://accounts.google.com");
		claims.put("aud", CLIENTE);
		claims.put("sub", "1098" + correo.hashCode());
		claims.put("email", correo);
		claims.put("email_verified", true);
		claims.put("name", "Ana María Prieto");
		Instant ahora = Instant.now();
		claims.put("iat", ahora.getEpochSecond());
		claims.put("exp", ahora.plusSeconds(3600).getEpochSecond());
		claims.putAll(cambios);
		return Jwts.builder().setHeaderParam("kid", ID_CLAVE).setClaims(claims)
				.signWith(clave, SignatureAlgorithm.RS256).compact();
	}

	private static String token(String correo) {
		return token(correo, Map.of(), CLAVES.getPrivate());
	}

	private JsonNode llamar(MockHttpServletRequestBuilder peticion, String jwt, Object cuerpo, int estado)
			throws Exception {
		if (jwt != null) {
			peticion.header("Authorization", "Bearer " + jwt);
		}
		if (cuerpo != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
		}
		MvcResult resultado = mvc.perform(peticion).andReturn();
		String texto = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertEquals(estado, resultado.getResponse().getStatus(), texto);
		return texto.isBlank() ? json.nullNode() : json.readTree(texto);
	}

	private JsonNode google(Map<String, Object> cuerpo, int estado) throws Exception {
		return llamar(post("/auth/google"), null, cuerpo, estado);
	}

	@Test
	void laPrimeraVezCreaLaCuentaVerificadaYLaSegundaIniciaSesion_RF008() throws Exception {
		String correo = correoNuevo();
		JsonNode configuracion = llamar(get("/auth/google/configuracion"), null, null, 200);
		assertTrue(configuracion.get("habilitado").asBoolean());
		assertEquals(CLIENTE, configuracion.get("clientId").asText());

		JsonNode creada = google(Map.of("idToken", token(correo)), 201);

		assertTrue(creada.get("cuentaNueva").asBoolean());
		assertEquals("HUESPED", creada.get("rol").asText());
		JsonNode perfil = llamar(get("/usuarios/perfil"), creada.get("token").asText(), null, 200);
		assertEquals("Ana María Prieto", perfil.get("nombre").asText());
		assertEquals(correo, perfil.get("correo").asText());
		assertTrue(perfil.get("correoVerificado").asBoolean());

		JsonNode otraVez = google(Map.of("idToken", token(correo.toUpperCase())), 200);
		assertFalse(otraVez.get("cuentaNueva").asBoolean());
		assertEquals(perfil.get("idUsuario").asLong(), llamar(get("/usuarios/perfil"),
				otraVez.get("token").asText(), null, 200).get("idUsuario").asLong());
	}

	@Test
	void sePuedeRegistrarComoAnfitrionPeroNuncaComoAdmin() throws Exception {
		assertEquals("ANFITRION", google(Map.of("idToken", token(correoNuevo()), "rol", "ANFITRION"), 201)
				.get("rol").asText());
		google(Map.of("idToken", token(correoNuevo()), "rol", "ADMIN"), 403);
	}

	@Test
	void unaCuentaConContraseniaSinVerificarEntraConGoogleYQuedaVerificada() throws Exception {
		String correo = correoNuevo();
		llamar(post("/auth/register"), null, Map.of("nombre", "Con Clave", "correo", correo, "contrasenia",
				"Segura#2026", "rol", "ANFITRION"), 201);
		llamar(post("/auth/login"), null, Map.of("correo", correo, "contrasenia", "Segura#2026"), 403);

		JsonNode sesion = google(Map.of("idToken", token(correo), "rol", "HUESPED"), 200);

		// Es la misma cuenta: conserva su rol y ahora también entra con contraseña
		assertEquals("ANFITRION", sesion.get("rol").asText());
		CorreoPrueba.iniciarSesion(mvc, mailSender, correo, "Segura#2026");
	}

	@Test
	void rechazaTokensQueNoSonDeGoogleParaEstaAplicacion() throws Exception {
		String correo = correoNuevo();
		google(Map.of("idToken", token(correo, Map.of("aud", "otra-app.apps.googleusercontent.com"),
				CLAVES.getPrivate())), 401);
		google(Map.of("idToken", token(correo, Map.of("iss", "https://evil.example.com"), CLAVES.getPrivate())), 401);
		google(Map.of("idToken", token(correo, Map.of("email_verified", false), CLAVES.getPrivate())), 401);
		google(Map.of("idToken", token(correo, Map.of("exp", Instant.now().minusSeconds(600).getEpochSecond()),
				CLAVES.getPrivate())), 401);
		google(Map.of("idToken", token(correo, Map.of(), OTRAS_CLAVES.getPrivate())), 401);
		google(Map.of("idToken", "no.es.un-token"), 401);
		google(Map.of(), 400);
		// Ningún intento creó la cuenta
		assertEquals(201, mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("idToken", token(correo))))).andReturn().getResponse()
				.getStatus());
	}

	@Test
	void unaCuentaRestringidaNoEntraYConSegundoFactorPideElCodigo() throws Exception {
		String admin = json.readTree(CorreoPrueba.iniciarSesion(mvc, mailSender, "admin@marketplace.local",
				"Admin#Prueba1")).get("token").asText();
		String restringido = correoNuevo();
		Long id = llamar(get("/usuarios/perfil"), google(Map.of("idToken", token(restringido)), 201).get("token")
				.asText(), null, 200).get("idUsuario").asLong();
		llamar(patch("/admin/usuarios/" + id + "/restringir"), admin, Map.of("motivo", "Fraude"), 200);
		google(Map.of("idToken", token(restringido)), 403);

		String correo = correoNuevo();
		String sesion = google(Map.of("idToken", token(correo)), 201).get("token").asText();
		String secreto = llamar(post("/usuarios/segundofactor/activar"), sesion, null, 200).get("secreto").asText();
		llamar(post("/usuarios/segundofactor/confirmar"), sesion,
				Map.of("codigo", TotpUtil.codigo(secreto, Instant.now())), 200);

		assertTrue(google(Map.of("idToken", token(correo)), 401).get("requiereSegundoFactor").asBoolean());
		google(Map.of("idToken", token(correo), "codigo", "000000"), 401);
		google(Map.of("idToken", token(correo), "codigo", TotpUtil.codigo(secreto, Instant.now())), 200);
	}
}
