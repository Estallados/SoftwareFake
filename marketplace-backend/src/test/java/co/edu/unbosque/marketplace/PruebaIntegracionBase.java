package co.edu.unbosque.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;

/**
 * Base de las pruebas de integración de los módulos 2 a 9: un contexto Spring
 * compartido con H2 en memoria y utilidades para registrar usuarios, publicar
 * alojamientos, cotizar y reservar por la API. Cada prueba usa correos y
 * alojamientos propios, así no depende de las demás.
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:modulos;DB_CLOSE_DELAY=-1",
		"app.admin.password=Admin#Prueba1" })
@AutoConfigureMockMvc
abstract class PruebaIntegracionBase {

	protected static final String ADMIN = "admin@marketplace.local";
	protected static final String CLAVE_ADMIN = "Admin#Prueba1";
	protected static final String CLAVE = "Segura#2026";
	protected static final LocalDate HOY = LocalDate.now();

	private static final AtomicInteger CONTADOR = new AtomicInteger();

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected ObjectMapper json;

	@Autowired
	protected PoliticaCancelacionRepository politicaRepo;

	/** Evita conectarse a un servidor SMTP real. */
	@MockBean
	protected JavaMailSender mailSender;

	/** Usuario registrado en la prueba. */
	protected record Sesion(String token, Long id, String correo) {
	}

	protected static String unico(String prefijo) {
		return prefijo + CONTADOR.incrementAndGet();
	}

	/** Ejecuta una petición, verifica el estado HTTP y devuelve el JSON de la respuesta. */
	protected JsonNode llamar(MockHttpServletRequestBuilder peticion, String token, Object cuerpo, int estado)
			throws Exception {
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		if (cuerpo != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
		}
		MvcResult resultado = mvc.perform(peticion).andReturn();
		String texto = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
		if (resultado.getResponse().getStatus() != estado) {
			throw new AssertionError("Se esperaba HTTP " + estado + " y llegó " + resultado.getResponse().getStatus()
					+ ": " + texto);
		}
		return texto.isBlank() ? json.nullNode() : json.readTree(texto);
	}

	protected Sesion registrar(String rol) throws Exception {
		String correo = unico(rol.toLowerCase()) + "@correo.com";
		llamar(post("/auth/register"), null,
				Map.of("nombre", "Usuario " + rol, "correo", correo, "contrasenia", CLAVE, "rol", rol), 201);
		String token = verificarCorreo(correo).get("token").asText();
		JsonNode perfil = llamar(get("/usuarios/perfil"), token, null, 200);
		return new Sesion(token, perfil.get("idUsuario").asLong(), correo);
	}

	/** Confirma el correo con el código que llegó al registrarse; devuelve la respuesta con el token. */
	protected JsonNode verificarCorreo(String correo) throws Exception {
		return llamar(post("/auth/verificar"), null,
				Map.of("correo", correo, "codigo", CorreoPrueba.codigoEnviadoA(mailSender, correo)), 200);
	}

	protected String tokenAdmin() throws Exception {
		return llamar(post("/auth/login"), null, Map.of("correo", ADMIN, "contrasenia", CLAVE_ADMIN), 200)
				.get("token").asText();
	}

	protected Long idPolitica(String nombre) {
		return politicaRepo.findByNombre(nombre).orElseThrow().getIdPolitica();
	}

	/** Datos completos de un alojamiento (todos los obligatorios para enviar a revisión). */
	protected Map<String, Object> datosAlojamiento(String ciudad, String tipo, int capacidad, BigDecimal precio,
			Set<String> amenidades, String politica) {
		Map<String, Object> datos = new HashMap<>();
		datos.put("titulo", "Alojamiento " + unico("A"));
		datos.put("descripcion", "Lugar tranquilo y bien ubicado");
		datos.put("ubicacion", "Calle 1 # 2-3");
		datos.put("ciudad", ciudad);
		datos.put("tipo", tipo);
		datos.put("capacidad", capacidad);
		datos.put("habitaciones", 2);
		datos.put("banos", 1);
		datos.put("horaEntrada", "15:00");
		datos.put("horaSalida", "11:00");
		datos.put("precioNoche", precio);
		datos.put("reglas", "No fumar");
		datos.put("idPolitica", idPolitica(politica));
		datos.put("amenidades", amenidades);
		datos.put("imagenes", List.of("/img/foto1.jpg"));
		datos.put("latitud", 4.711);
		datos.put("longitud", -74.072);
		return datos;
	}

	/**
	 * Crea, ofrece de hoy a hoy + 120 días, envía a revisión y aprueba un
	 * alojamiento. Devuelve su id.
	 */
	protected Long crearAlojamientoActivo(Sesion anfitrion, String ciudad, String tipo, int capacidad,
			BigDecimal precio, Set<String> amenidades, String politica) throws Exception {
		Long id = llamar(post("/alojamientos"), anfitrion.token(),
				datosAlojamiento(ciudad, tipo, capacidad, precio, amenidades, politica), 201).get("idAlojamiento")
				.asLong();
		llamar(post("/alojamientos/" + id + "/disponibilidad"), anfitrion.token(),
				Map.of("fechaInicio", HOY.toString(), "fechaFin", HOY.plusDays(120).toString()), 201);
		llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 200);
		llamar(post("/admin/alojamientos/" + id + "/aprobar"), tokenAdmin(), null, 200);
		return id;
	}

	protected Long crearAlojamientoActivo(Sesion anfitrion) throws Exception {
		return crearAlojamientoActivo(anfitrion, "Bogotá", "Apartamento", 4, new BigDecimal("100000"),
				Set.of("wifi"), "MODERADA");
	}

	protected JsonNode cotizar(Sesion huesped, Long idAlojamiento, int desdeHoy, int noches, int huespedes,
			List<Long> servicios, int estado) throws Exception {
		Map<String, Object> solicitud = new HashMap<>();
		solicitud.put("idAlojamiento", idAlojamiento);
		solicitud.put("fechaEntrada", HOY.plusDays(desdeHoy).toString());
		solicitud.put("fechaSalida", HOY.plusDays(desdeHoy + (long) noches).toString());
		solicitud.put("huespedes", huespedes);
		solicitud.put("serviciosSeleccionados", servicios);
		return llamar(post("/cotizaciones"), huesped.token(), solicitud, estado);
	}

	protected JsonNode reservar(Sesion huesped, Long idCotizacion, String metodoPago, int estado) throws Exception {
		Map<String, Object> solicitud = new HashMap<>();
		solicitud.put("idCotizacion", idCotizacion);
		solicitud.put("metodoPago", metodoPago);
		return llamar(post("/reservas").header("Idempotency-Key", unico("clave-")), huesped.token(), solicitud,
				estado);
	}

	/** Cotiza y reserva con pago aprobado; devuelve la reserva. */
	protected JsonNode reservaConfirmada(Sesion huesped, Long idAlojamiento, int desdeHoy, int noches)
			throws Exception {
		Long idCotizacion = cotizar(huesped, idAlojamiento, desdeHoy, noches, 2, List.of(), 201).get("idCotizacion")
				.asLong();
		return reservar(huesped, idCotizacion, "pm_card_visa", 201);
	}

	protected JsonNode actualizarAlojamiento(Sesion actor, Long id, Map<String, Object> cambios, int estado)
			throws Exception {
		return llamar(put("/alojamientos/" + id), actor.token(), cambios, estado);
	}
}
