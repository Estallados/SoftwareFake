package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;

/**
 * Administrador inicial, permisos por rol y CORS. Usa su propia base H2 para
 * que el admin creado al arrancar no se mezcle con los otros tests.
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:seguridad;DB_CLOSE_DELAY=-1",
		"app.admin.password=Admin#Prueba1" })
@AutoConfigureMockMvc
class SeguridadRolesYCorsTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private UsuarioRepository usuarioRepo;

	@MockBean
	private JavaMailSender mailSender;

	private String token(String correo, String contrasenia) throws Exception {
		String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"" + correo + "\",\"contrasenia\":\"" + contrasenia + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return json.readTree(respuesta).get("token").asText();
	}

	/** Registra y verifica el correo con el código enviado; devuelve el token de sesión. */
	private String registrarHuesped(String correo) throws Exception {
		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Huésped\",\"correo\":\"" + correo + "\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isCreated());
		String respuesta = mvc.perform(post("/auth/verificar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"" + correo + "\",\"codigo\":\"" + CorreoPrueba.codigoEnviadoA(mailSender, correo)
						+ "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return json.readTree(respuesta).get("token").asText();
	}

	@Test
	void seCreaElAdminInicialYPuedeIniciarSesion() throws Exception {
		assertEquals(true, usuarioRepo.existsByRol(Role.ADMIN));

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"admin@marketplace.local\",\"contrasenia\":\"Admin#Prueba1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("ADMIN"));
	}

	@Test
	void soloAdminPuedeListarUsuarios_CA131() throws Exception {
		String tokenAdmin = token("admin@marketplace.local", "Admin#Prueba1");
		String tokenHuesped = registrarHuesped("huesped.roles@correo.com");

		mvc.perform(get("/usuarios/listar").header("Authorization", "Bearer " + tokenAdmin))
				.andExpect(status().isOk());
		mvc.perform(get("/usuarios/listar").header("Authorization", "Bearer " + tokenHuesped))
				.andExpect(status().isForbidden());
	}

	@Test
	void corsPermiteAlFrontYBloqueaOtrosOrigenes() throws Exception {
		mvc.perform(options("/auth/login")
				.header("Origin", "http://localhost:4200")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));

		mvc.perform(options("/auth/login")
				.header("Origin", "http://sitio-malicioso.com")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}
}
