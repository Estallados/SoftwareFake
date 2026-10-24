package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.service.UsuarioService;

/**
 * Flujo completo de autenticación contra una base H2 en memoria:
 * registro (con token automático), login y acceso a un endpoint protegido.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private UsuarioRepository usuarioRepo;

	/** Evita conectarse a un servidor SMTP real al enviar el correo de bienvenida. */
	@MockBean
	private JavaMailSender mailSender;

	@BeforeEach
	void limpiar() {
		usuarioRepo.deleteAll();
	}

	private String registro(String nombre, String correo, String contrasenia, String rol) throws Exception {
		var body = json.createObjectNode().put("nombre", nombre).put("correo", correo).put("contrasenia", contrasenia);
		if (rol != null) {
			body.put("rol", rol);
		}
		return json.writeValueAsString(body);
	}

	private JsonNode leer(String contenido) throws Exception {
		return json.readTree(contenido);
	}

	@Test
	void registroCreaLaCuentaConSuRolYDevuelveToken_CA030_CA032_CA033_CA042() throws Exception {
		String respuesta = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana Pérez", "  Ana@Correo.com ", "Segura#2026", null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rol").value("HUESPED"))
				.andExpect(jsonPath("$.correo").value("ana@correo.com"))
				.andReturn().getResponse().getContentAsString();

		String token = leer(respuesta).get("token").asText();

		mvc.perform(get("/usuarios/perfil").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Ana Pérez"))
				.andExpect(jsonPath("$.correo").value("ana@correo.com"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.contrasenia").doesNotExist());

		// En la base el correo y el nombre quedan cifrados
		Usuario guardado = usuarioRepo.findAll().get(0);
		assertNotEquals("ana@correo.com", guardado.getCorreo());
		assertEquals(UsuarioService.hashCorreo("ana@correo.com"), guardado.getCorreoHash());
		assertEquals(EstadoUsuario.ACTIVO, guardado.getEstado());
	}

	@Test
	void loginPermiteElAccesoIdentificaElRolYRechazaCredencialesMalas_CA034_CA035_CA036() throws Exception {
		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Luis", "luis@correo.com", "Segura#2026", "ANFITRION")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rol").value("ANFITRION"));

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\" LUIS@correo.com\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.rol").value("ANFITRION"));

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"luis@correo.com\",\"contrasenia\":\"Incorrecta#1\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void noPermiteCorreoDuplicado() throws Exception {
		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", "ana@correo.com", "Segura#2026", null)))
				.andExpect(status().isCreated());

		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Otra Ana", "ANA@correo.com", "Segura#2026", null)))
				.andExpect(status().isConflict());

		assertEquals(1, usuarioRepo.count());
	}

	@Test
	void noPermiteRegistrarseComoAdmin() throws Exception {
		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Mala", "mala@correo.com", "Segura#2026", "ADMIN")))
				.andExpect(status().isForbidden());

		assertEquals(0, usuarioRepo.count());
	}

	@Test
	void rechazaDatosFaltantesOInvalidos_CA031_CA135() throws Exception {
		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", "ana@correo.com", "debil", null)))
				.andExpect(status().isBadRequest());

		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", "no-es-correo", "Segura#2026", null)))
				.andExpect(status().isBadRequest());

		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("", "ana@correo.com", "Segura#2026", null)))
				.andExpect(status().isBadRequest());

		assertEquals(0, usuarioRepo.count());
	}

	@Test
	void ignoraIdYEstadoEnviadosPorElCliente() throws Exception {
		Usuario otro = new Usuario();
		otro.setNombre("x");
		otro.setCorreo("x");
		otro.setCorreoHash("x");
		otro.setContrasenia("x");
		otro.setRol(Role.HUESPED);
		otro.setEstado(EstadoUsuario.ACTIVO);
		Long idExistente = usuarioRepo.save(otro).getIdUsuario();

		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idUsuario\":" + idExistente + ",\"estado\":\"RESTRINGIDO\",\"nombre\":\"Eva\","
						+ "\"correo\":\"eva@correo.com\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isCreated());

		assertEquals(2, usuarioRepo.count());
		assertEquals("x", usuarioRepo.findById(idExistente).get().getNombre());
		assertEquals(EstadoUsuario.ACTIVO,
				usuarioRepo.findByCorreoHash(UsuarioService.hashCorreo("eva@correo.com")).get().getEstado());
	}

	@Test
	void sinSesionNoSePuedeAccederAFuncionesRestringidas_CA037_CA044_CA134() throws Exception {
		mvc.perform(get("/usuarios/perfil")).andExpect(status().isUnauthorized());
		mvc.perform(get("/usuarios/perfil").header("Authorization", "Bearer token.falso.123"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cuentaDesactivadaNoPuedeUsarSuToken() throws Exception {
		String respuesta = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registro("Ana", "ana@correo.com", "Segura#2026", null)))
				.andReturn().getResponse().getContentAsString();
		String token = leer(respuesta).get("token").asText();

		mvc.perform(patch("/usuarios/perfil/desactivar").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mvc.perform(get("/usuarios/perfil").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"ana@correo.com\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isForbidden());
	}
}
