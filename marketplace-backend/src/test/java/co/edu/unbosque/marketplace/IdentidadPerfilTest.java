package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.EventoAuditoria;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.TokenRecuperacion;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.AuditoriaRepository;
import co.edu.unbosque.marketplace.repository.CodigoVerificacionRepository;
import co.edu.unbosque.marketplace.repository.TokenRecuperacionRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import co.edu.unbosque.marketplace.service.UsuarioService;
import co.edu.unbosque.marketplace.util.AESUtil;

/**
 * Módulo 1 de punta a punta: perfil, desactivación y reactivación, recuperación
 * de contraseña simulada, creación de administradores y auditoría.
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:identidad;DB_CLOSE_DELAY=-1",
		"app.admin.password=Admin#Prueba1" })
@AutoConfigureMockMvc
class IdentidadPerfilTest {

	private static final String ADMIN = "admin@marketplace.local";
	private static final String CLAVE_ADMIN = "Admin#Prueba1";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private UsuarioRepository usuarioRepo;

	@Autowired
	private TokenRecuperacionRepository tokenRepo;

	@Autowired
	private CodigoVerificacionRepository codigoRepo;

	@Autowired
	private AuditoriaRepository auditoriaRepo;

	@MockBean
	private JavaMailSender mailSender;

	@BeforeEach
	void limpiar() {
		// Primero las tablas que referencian al usuario (claves foráneas); se conserva el admin inicial
		tokenRepo.deleteAll();
		codigoRepo.deleteAll();
		usuarioRepo.findAll().stream().filter(u -> u.getRol() != Role.ADMIN)
				.forEach(usuarioRepo::delete);
	}

	/** Registra y verifica el correo con el código enviado; devuelve el token de sesión. */
	private String registrar(String correo) throws Exception {
		mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Ana\",\"correo\":\"" + correo + "\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isCreated());
		String respuesta = mvc.perform(post("/auth/verificar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"" + correo + "\",\"codigo\":\"" + CorreoPrueba.codigoEnviadoA(mailSender, correo)
						+ "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(respuesta).get("token").asText();
	}

	private String login(String correo, String contrasenia) throws Exception {
		String respuesta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"" + correo + "\",\"contrasenia\":\"" + contrasenia + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(respuesta).get("token").asText();
	}

	private Usuario buscar(String correo) {
		return usuarioRepo.findByCorreoHash(UsuarioService.hashCorreo(correo)).orElseThrow();
	}

	private boolean hayEvento(String accion, ResultadoAuditoria resultado) {
		return auditoriaRepo.findByAccion(accion, PageRequest.of(0, 500)).stream()
				.map(EventoAuditoria::getResultado).anyMatch(resultado::equals);
	}

	// ----- perfil -----

	@Test
	void debeActualizarSoloNombreYTelefonoYMostrarLosCambios_CA046_CA048() throws Exception {
		String token = registrar("perfil@correo.com");

		mvc.perform(put("/usuarios/perfil").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Ana María\",\"telefono\":\"3001234567\",\"rol\":\"ADMIN\","
						+ "\"correo\":\"otro@correo.com\",\"estado\":\"RESTRINGIDO\"}"))
				.andExpect(status().isOk());

		mvc.perform(get("/usuarios/perfil").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Ana María"))
				.andExpect(jsonPath("$.telefono").value("3001234567"))
				.andExpect(jsonPath("$.correo").value("perfil@correo.com"))
				.andExpect(jsonPath("$.rol").value("HUESPED"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));

		assertTrue(hayEvento(AuditoriaService.PERFIL_ACTUALIZADO, ResultadoAuditoria.EXITOSO));
	}

	@Test
	void debeRechazarTelefonoInvalidoONombreVacio_CA047() throws Exception {
		String token = registrar("perfil2@correo.com");

		mvc.perform(put("/usuarios/perfil").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"abc\"}"))
				.andExpect(status().isBadRequest());
		mvc.perform(put("/usuarios/perfil").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \"}"))
				.andExpect(status().isBadRequest());
		mvc.perform(put("/usuarios/perfil").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unUsuarioNoPuedeConsultarNiModificarOtraCuenta_CA049_CA053_CA133() throws Exception {
		String tokenA = registrar("cuentaA@correo.com");
		String tokenB = registrar("cuentaB@correo.com");
		Usuario cuentaA = buscar("cuentaA@correo.com");

		mvc.perform(get("/usuarios/obtener/" + cuentaA.getIdUsuario()).header("Authorization", "Bearer " + tokenB))
				.andExpect(status().isForbidden());
		// El perfil siempre es el del token: enviar el id de otra cuenta no la modifica
		mvc.perform(put("/usuarios/perfil").header("Authorization", "Bearer " + tokenB)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"idUsuario\":" + cuentaA.getIdUsuario() + ",\"nombre\":\"Cambiado por B\"}"))
				.andExpect(status().isOk());
		mvc.perform(get("/usuarios/perfil").header("Authorization", "Bearer " + tokenA))
				.andExpect(jsonPath("$.nombre").value("Ana"));
		// Desactivar solo afecta la cuenta del token
		mvc.perform(patch("/usuarios/perfil/desactivar").header("Authorization", "Bearer " + tokenB))
				.andExpect(status().isOk());
		assertEquals(EstadoUsuario.ACTIVO, buscar("cuentaA@correo.com").getEstado());
	}

	// ----- desactivar y reactivar -----

	@Test
	void debeDesactivarYReactivarLaCuentaPropiaMostrandoElEstado_CA050_CA052() throws Exception {
		String token = registrar("reactivar@correo.com");
		mvc.perform(patch("/usuarios/perfil/desactivar").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));

		mvc.perform(post("/auth/reactivar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"reactivar@correo.com\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.estado").value("ACTIVO"));

		assertEquals(EstadoUsuario.ACTIVO, buscar("reactivar@correo.com").getEstado());
		assertTrue(hayEvento(AuditoriaService.CUENTA_DESACTIVADA, ResultadoAuditoria.EXITOSO));
		assertTrue(hayEvento(AuditoriaService.CUENTA_REACTIVADA, ResultadoAuditoria.EXITOSO));
	}

	@Test
	void unaCuentaRestringidaNoPuedeReactivarseSola() throws Exception {
		registrar("restringida@correo.com");
		Usuario usuario = buscar("restringida@correo.com");
		usuario.setEstado(EstadoUsuario.RESTRINGIDO);
		usuarioRepo.save(usuario);

		mvc.perform(post("/auth/reactivar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"restringida@correo.com\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isForbidden());

		assertEquals(EstadoUsuario.RESTRINGIDO, buscar("restringida@correo.com").getEstado());
	}

	@Test
	void reactivarConCredencialesInvalidasResponde401() throws Exception {
		registrar("credenciales@correo.com");

		mvc.perform(post("/auth/reactivar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"credenciales@correo.com\",\"contrasenia\":\"Otra#2026\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/auth/reactivar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"credenciales@correo.com\",\"contrasenia\":\"Segura#2026\"}"))
				.andExpect(status().isConflict());
	}

	// ----- recuperación de contraseña simulada -----

	@Test
	void debeRecuperarLaContraseniaConElProcesoSimulado_CA038_CA039_CA040_CA041() throws Exception {
		registrar("olvido@correo.com");

		mvc.perform(post("/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"olvido@correo.com\"}"))
				.andExpect(status().isOk());
		assertEquals(1, tokenRepo.count());

		// El token real solo sale en el log; aquí se reemplaza por uno conocido
		TokenRecuperacion token = tokenRepo.findAll().get(0);
		token.setTokenHash(AESUtil.hash("token-de-prueba"));
		tokenRepo.save(token);

		mvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"token-de-prueba\",\"contrasenia\":\"Nueva#2026\"}"))
				.andExpect(status().isOk());

		login("olvido@correo.com", "Nueva#2026");

		// El token sirve una sola vez
		mvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"token-de-prueba\",\"contrasenia\":\"Otra#2026\"}"))
				.andExpect(status().isBadRequest());
		assertTrue(hayEvento(AuditoriaService.CONTRASENIA_RESTABLECIDA, ResultadoAuditoria.EXITOSO));
	}

	@Test
	void recuperarNoRevelaSiElCorreoExiste_CA039() throws Exception {
		mvc.perform(post("/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"nadie@correo.com\"}"))
				.andExpect(status().isOk());
		assertEquals(0, tokenRepo.count());
	}

	@Test
	void noDebeRestablecerConTokenVencido() throws Exception {
		registrar("vencido@correo.com");
		TokenRecuperacion token = new TokenRecuperacion();
		token.setIdUsuario(buscar("vencido@correo.com").getIdUsuario());
		token.setTokenHash(AESUtil.hash("token-vencido"));
		token.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
		token.setUsado(false);
		tokenRepo.save(token);

		mvc.perform(post("/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"token-vencido\",\"contrasenia\":\"Nueva#2026\"}"))
				.andExpect(status().isBadRequest());
	}

	// ----- administradores (decisión sobre RF-001) -----

	@Test
	void unAdministradorPuedeCrearOtroAdministrador_RF001() throws Exception {
		String tokenAdmin = login(ADMIN, CLAVE_ADMIN);

		mvc.perform(post("/usuarios/crearadministrador").header("Authorization", "Bearer " + tokenAdmin)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Segundo Admin\",\"correo\":\"admin2@correo.com\","
						+ "\"contrasenia\":\"Admin#2026\",\"rol\":\"HUESPED\"}"))
				.andExpect(status().isCreated());

		// El nuevo administrador también confirma su correo antes de entrar
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"admin2@correo.com\",\"contrasenia\":\"Admin#2026\"}"))
				.andExpect(status().isForbidden());
		mvc.perform(post("/auth/verificar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"admin2@correo.com\",\"codigo\":\""
						+ CorreoPrueba.codigoEnviadoA(mailSender, "admin2@correo.com") + "\"}"))
				.andExpect(status().isOk());

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"admin2@correo.com\",\"contrasenia\":\"Admin#2026\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("ADMIN"));
		assertTrue(hayEvento(AuditoriaService.CREACION_ADMINISTRADOR, ResultadoAuditoria.EXITOSO));
	}

	@Test
	void unHuespedNoPuedeCrearAdministradores_RF001() throws Exception {
		String token = registrar("intruso@correo.com");

		mvc.perform(post("/usuarios/crearadministrador").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"X\",\"correo\":\"x@correo.com\",\"contrasenia\":\"Admin#2026\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
		assertTrue(hayEvento(AuditoriaService.ACCESO_DENEGADO, ResultadoAuditoria.RECHAZADO));
	}

	// ----- auditoría de autenticación (RNF-024, RNF-025) -----

	@Test
	void debeAuditarLoginExitosoYFallido_CA136_CA152() throws Exception {
		registrar("auditoria@correo.com");
		login("auditoria@correo.com", "Segura#2026");
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"auditoria@correo.com\",\"contrasenia\":\"Mala#2026\"}"))
				.andExpect(status().isUnauthorized());

		assertTrue(hayEvento(AuditoriaService.LOGIN, ResultadoAuditoria.EXITOSO));
		assertTrue(hayEvento(AuditoriaService.LOGIN, ResultadoAuditoria.RECHAZADO));
		assertTrue(hayEvento(AuditoriaService.REGISTRO_USUARIO, ResultadoAuditoria.EXITOSO));
	}

	// ----- manejador global y salud -----

	@Test
	void erroresTecnicosRespondenConFormatoUniforme_CA149() throws Exception {
		mvc.perform(get("/auth/login"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void elEndpointDeSaludEsPublico() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}
}
