package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.AuditoriaRepository;
import co.edu.unbosque.marketplace.service.AuditoriaService;

/** Módulo 2: publicaciones, ciclo de estados, servicios adicionales y tarifas. */
class PublicacionesTest extends PruebaIntegracionBase {

	@Autowired
	private AuditoriaRepository auditoriaRepo;

	private Long crearBorrador(Sesion anfitrion, Map<String, Object> datos) throws Exception {
		return llamar(post("/alojamientos"), anfitrion.token(), datos, 201).get("idAlojamiento").asLong();
	}

	@Test
	void debeCrearPublicacionEnBorradorConLaInformacionMinima_CA094_CA095_CA096() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Map<String, Object> datos = datosAlojamiento("Medellín", "Casa", 6, new BigDecimal("250000"),
				Set.of("Wifi", "Piscina"), "FLEXIBLE");
		datos.put("estado", "ACTIVA");
		datos.put("idAnfitrion", 999);

		JsonNode creado = llamar(post("/alojamientos"), anfitrion.token(), datos, 201);

		assertEquals("BORRADOR", creado.get("estado").asText());
		assertEquals(anfitrion.id(), creado.get("idAnfitrion").asLong());
		assertEquals("Medellín", creado.get("ciudad").asText());
		assertEquals("FLEXIBLE", creado.get("nombrePolitica").asText());
		assertEquals(1, creado.get("imagenes").size());
		assertTrue(creado.get("amenidades").toString().contains("piscina"));
	}

	@Test
	void unHuespedNoPuedeCrearPublicaciones_CA131() throws Exception {
		Sesion huesped = registrar("HUESPED");
		llamar(post("/alojamientos"), huesped.token(), Map.of("titulo", "X"), 403);
	}

	@Test
	void debeRechazarCapacidadMenorOIgualACero_CA098() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		JsonNode error = llamar(post("/alojamientos"), anfitrion.token(), Map.of("titulo", "X", "capacidad", 0),
				400);
		assertTrue(error.get("message").asText().contains("capacidad debe ser mayor que cero"));
	}

	@Test
	void debeRechazarPrecioMenorOIgualACero_CA099() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, Map.of("titulo", "X"));
		JsonNode error = actualizarAlojamiento(anfitrion, id, Map.of("precioNoche", -5), 400);
		assertTrue(error.get("message").asText().contains("precio por noche debe ser positivo"));
	}

	@Test
	void debeImpedirEnviarARevisionSinInformacionMinimaEIndicarFaltantes_CA097() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, Map.of("titulo", "Solo título"));

		JsonNode error = llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 400);

		String mensaje = error.get("message").asText();
		assertTrue(mensaje.contains("descripción") && mensaje.contains("capacidad")
				&& mensaje.contains("al menos una imagen"), mensaje);
	}

	@Test
	void debeEnviarARevisionYAprobarQuedandoVisibleEnBusquedas_CA157_CA158_CA194() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		String ciudad = unico("CiudadAprobada");
		Long id = crearBorrador(anfitrion, datosAlojamiento(ciudad, "Casa", 2, new BigDecimal("90000"),
				Set.of(), "ESTRICTA"));

		JsonNode enviado = llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 200);
		assertEquals("PENDIENTE_REVISION", enviado.get("estado").asText());
		assertEquals(0, llamar(get("/alojamientos?ciudad=" + ciudad), null, null, 200).get("totalElementos").asInt());

		JsonNode aprobado = llamar(post("/admin/alojamientos/" + id + "/aprobar"), tokenAdmin(), null, 200);
		assertEquals("ACTIVA", aprobado.get("estado").asText());
		assertEquals(1, llamar(get("/alojamientos?ciudad=" + ciudad), null, null, 200).get("totalElementos").asInt());
	}

	@Test
	void debeRechazarConMotivoNotificarAlAnfitrionYAuditar_CA195_CA196() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, datosAlojamiento("Cali", "Casa", 2, new BigDecimal("90000"), Set.of(),
				"FLEXIBLE"));
		llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 200);
		String admin = tokenAdmin();

		llamar(post("/admin/alojamientos/" + id + "/rechazar"), admin, Map.of(), 400);
		JsonNode rechazado = llamar(post("/admin/alojamientos/" + id + "/rechazar"), admin,
				Map.of("motivo", "Fotos borrosas"), 200);

		assertEquals("BORRADOR", rechazado.get("estado").asText());
		JsonNode notificaciones = llamar(get("/notificaciones"), anfitrion.token(), null, 200);
		assertTrue(notificaciones.get("contenido").toString().contains("Fotos borrosas"));
		boolean auditado = auditoriaRepo.findByAccion(AuditoriaService.ALOJAMIENTO_CAMBIO_ESTADO,
				PageRequest.of(0, 1000)).stream()
				.anyMatch(e -> id.equals(e.getIdEntidad()) && e.getDetalle().contains("Fotos borrosas")
						&& e.getIdUsuario() != null && e.getFecha() != null
						&& e.getResultado() == ResultadoAuditoria.EXITOSO);
		assertTrue(auditado);
	}

	@Test
	void elPropietarioModificaYVeLosCambios_CA100_CA113() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, Map.of("titulo", "Original"));

		actualizarAlojamiento(anfitrion, id, Map.of("titulo", "Nuevo título", "capacidad", 3), 200);

		JsonNode detalle = llamar(get("/alojamientos/" + id), anfitrion.token(), null, 200);
		assertEquals("Nuevo título", detalle.get("titulo").asText());
		assertEquals(3, detalle.get("capacidad").asInt());
	}

	@Test
	void otroAnfitrionNoPuedeModificarPeroUnAdministradorSi_CA101_CA113_CA114_CA132() throws Exception {
		Sesion propietario = registrar("ANFITRION");
		Sesion otro = registrar("ANFITRION");
		Long id = crearBorrador(propietario, Map.of("titulo", "Ajeno"));

		actualizarAlojamiento(otro, id, Map.of("titulo", "Hackeado"), 403);
		llamar(put("/alojamientos/" + id), tokenAdmin(), Map.of("titulo", "Corregido por admin"), 200);

		assertEquals("Corregido por admin",
				llamar(get("/alojamientos/" + id), propietario.token(), null, 200).get("titulo").asText());
	}

	@Test
	void soloSePermitenLasTransicionesDefinidas_CA104() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, Map.of("titulo", "Borrador"));

		JsonNode error = llamar(post("/alojamientos/" + id + "/pausar"), anfitrion.token(), null, 409);
		assertTrue(error.get("message").asText().contains("BORRADOR a PAUSADA"));
		llamar(post("/admin/alojamientos/" + id + "/aprobar"), tokenAdmin(), null, 409);
	}

	@Test
	void pausarCambiaAPausadaEImpideNuevasReservas_CA108_CA109() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Sesion huesped = registrar("HUESPED");
		Long id = crearAlojamientoActivo(anfitrion);

		JsonNode pausado = llamar(post("/alojamientos/" + id + "/pausar"), anfitrion.token(), null, 200);
		assertEquals("PAUSADA", pausado.get("estado").asText());
		cotizar(huesped, id, 5, 2, 1, List.of(), 409);

		llamar(post("/alojamientos/" + id + "/reanudar"), anfitrion.token(), null, 200);
		cotizar(huesped, id, 5, 2, 1, List.of(), 201);
	}

	@Test
	void elDetalleDeUnaPublicacionActivaEsPublicoYElDeUnBorradorNo_CA093_CA102() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long activa = crearAlojamientoActivo(anfitrion);
		Long borrador = crearBorrador(anfitrion, Map.of("titulo", "Privado"));

		JsonNode detalle = llamar(get("/alojamientos/" + activa), null, null, 200);
		assertEquals("ACTIVA", detalle.get("estado").asText());
		assertTrue(detalle.has("latitud") && detalle.has("serviciosAdicionales"));
		llamar(get("/alojamientos/" + borrador), null, null, 404);
		llamar(get("/alojamientos/" + borrador), registrar("HUESPED").token(), null, 404);
	}

	@Test
	void elAnfitrionConsultaSusPublicacionesConSuEstado_CA103() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		crearAlojamientoActivo(anfitrion);
		crearBorrador(anfitrion, Map.of("titulo", "Otro"));

		JsonNode mios = llamar(get("/alojamientos/mios"), anfitrion.token(), null, 200);

		assertEquals(2, mios.get("totalElementos").asInt());
		String estados = mios.get("contenido").toString();
		assertTrue(estados.contains("ACTIVA") && estados.contains("BORRADOR"));
	}

	@Test
	void elHistorialConservaTodosLosCambiosDeEstado_CA153() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		llamar(post("/alojamientos/" + id + "/pausar"), anfitrion.token(), null, 200);

		JsonNode historial = llamar(get("/alojamientos/" + id + "/historial"), anfitrion.token(), null, 200);

		assertEquals(4, historial.size());
		assertEquals("BORRADOR", historial.get(0).get("estadoNuevo").asText());
		assertEquals("PAUSADA", historial.get(3).get("estadoNuevo").asText());
	}

	@Test
	void elPropietarioRegistraServiciosAdicionalesYOtrosNo_CA185_CA187() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, Map.of("titulo", "Con servicios"));

		JsonNode servicio = llamar(post("/alojamientos/" + id + "/servicios"), anfitrion.token(),
				Map.of("nombre", "Desayuno", "costo", 20000), 201);
		llamar(post("/alojamientos/" + id + "/servicios"), anfitrion.token(), Map.of("nombre", "Gratis", "costo", 0),
				400);
		Sesion otro = registrar("ANFITRION");
		Long idServicio = servicio.get("idServicio").asLong();
		llamar(put("/alojamientos/" + id + "/servicios/" + idServicio), otro.token(),
				Map.of("nombre", "Robado", "costo", 1), 403);
		llamar(delete("/alojamientos/" + id + "/servicios/" + idServicio), otro.token(), null, 403);

		assertEquals("Desayuno", llamar(get("/alojamientos/" + id), anfitrion.token(), null, 200)
				.get("serviciosAdicionales").get(0).get("nombre").asText());
	}

	@Test
	void sinConfiguracionSeUsaElPorcentajePorDefectoYAlCambiarloSeAplica_CA084_CA085_CA180() throws Exception {
		String admin = tokenAdmin();
		Sesion anfitrion = registrar("ANFITRION");
		Sesion huesped = registrar("HUESPED");
		Long id = crearAlojamientoActivo(anfitrion);

		JsonNode vigente = llamar(get("/admin/tarifas"), admin, null, 200);
		JsonNode antes = cotizar(huesped, id, 3, 2, 1, List.of(), 201);
		assertEquals(vigente.get("porcentajeServicio").decimalValue().compareTo(
				antes.get("porcentajeServicio").decimalValue()), 0);

		llamar(put("/admin/tarifas"), admin, Map.of("porcentajeLimpieza", 7, "porcentajeServicio", 12), 200);
		JsonNode despues = cotizar(huesped, id, 3, 2, 1, List.of(), 201);
		assertEquals(0, new BigDecimal("12").compareTo(despues.get("porcentajeServicio").decimalValue()));
		assertEquals(0, new BigDecimal("24000.00").compareTo(despues.get("valorServicio").decimalValue()));

		llamar(put("/admin/tarifas"), admin, Map.of("porcentajeLimpieza", 5, "porcentajeServicio", 10), 200);
		llamar(put("/admin/tarifas"), admin, Map.of("porcentajeLimpieza", 150, "porcentajeServicio", 10), 400);
	}

	@Test
	void elPorcentajePropioDelAlojamientoSobreescribeElGlobal_CA180() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Sesion huesped = registrar("HUESPED");
		Long id = crearAlojamientoActivo(anfitrion);
		actualizarAlojamiento(anfitrion, id, Map.of("porcentajeServicio", 3), 200);

		JsonNode cotizacion = cotizar(huesped, id, 3, 1, 1, List.of(), 201);

		assertEquals(0, new BigDecimal("3").compareTo(cotizacion.get("porcentajeServicio").decimalValue()));
	}

	@Test
	void lasPoliticasDeCancelacionSonPublicas() throws Exception {
		JsonNode politicas = llamar(get("/alojamientos/politicas"), null, null, 200);
		assertTrue(politicas.toString().contains("MODERADA"));
	}
}
