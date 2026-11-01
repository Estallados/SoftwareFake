package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.AuditoriaRepository;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import co.edu.unbosque.marketplace.service.GeocodingService.Coordenadas;

/** Módulo 2: publicaciones, ciclo de estados, servicios adicionales y tarifas. */
class PublicacionesTest extends PruebaIntegracionBase {

	@Autowired
	private AuditoriaRepository auditoriaRepo;

	private Long crearBorrador(Sesion anfitrion, Map<String, Object> datos) throws Exception {
		return crearBorrador(anfitrion, datos, 201).get("idAlojamiento").asLong();
	}

	@Test
	void debeCrearPublicacionEnBorradorConLaInformacionMinima_CA094_CA095_CA096() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Map<String, Object> datos = datosAlojamiento("Medellín", "Casa", 6, new BigDecimal("250000"),
				Set.of("Wifi", "Piscina"), "FLEXIBLE");
		datos.put("estado", "ACTIVA");
		datos.put("idAnfitrion", 999);

		JsonNode creado = crearBorrador(anfitrion, datos, 201);

		assertEquals("BORRADOR", creado.get("estado").asText());
		assertEquals(anfitrion.id(), creado.get("idAnfitrion").asLong());
		assertEquals("Medellín", creado.get("ciudad").asText());
		assertEquals("FLEXIBLE", creado.get("nombrePolitica").asText());
		assertEquals(1, creado.get("imagenes").size());
		assertTrue(creado.get("amenidades").toString().contains("piscina"));
	}

	@Test
	void cuentaLosAlojamientosPorTipoSinToken() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		String tipo = unico("Tipo");
		for (int i = 0; i < 2; i++) {
			crearBorrador(anfitrion, datosAlojamiento("Bogotá", tipo, 2, new BigDecimal("100000"), Set.of("wifi"),
					"FLEXIBLE"), 201);
		}

		JsonNode tipos = llamar(get("/alojamientos/tipos"), null, null, 200);
		assertEquals(2, tipos.get(tipo).asInt());
	}

	@Test
	void guardaHabitacionesBanosYHorasDeEntradaYSalida() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Map<String, Object> datos = datosAlojamiento("Cartagena", "Apartamento", 4, new BigDecimal("180000"),
				Set.of("wifi"), "MODERADA");
		datos.put("habitaciones", 3);
		datos.put("camas", 4);
		datos.put("banos", 2);
		datos.put("horaEntrada", "14:30");

		JsonNode creado = crearBorrador(anfitrion, datos, 201);
		assertEquals(3, creado.get("habitaciones").asInt());
		assertEquals(4, creado.get("camas").asInt());
		assertEquals(2, creado.get("banos").asInt());
		assertEquals("14:30", creado.get("horaEntrada").asText());
		assertEquals("11:00", creado.get("horaSalida").asText());
		assertTrue(creado.hasNonNull("fechaActualizacion"));

		Long id = creado.get("idAlojamiento").asLong();
		JsonNode editado = actualizarAlojamiento(anfitrion, id, Map.of("banos", 3, "horaSalida", "12:00"), 200);
		assertEquals(3, editado.get("banos").asInt());
		assertEquals(3, editado.get("habitaciones").asInt());
		assertEquals(4, editado.get("camas").asInt());
		assertEquals("12:00", editado.get("horaSalida").asText());

		JsonNode error = actualizarAlojamiento(anfitrion, id, Map.of("habitaciones", -1), 400);
		assertTrue(error.get("message").asText().contains("no pueden ser negativos"));
		error = actualizarAlojamiento(anfitrion, id, Map.of("camas", -1), 400);
		assertTrue(error.get("message").asText().contains("no pueden ser negativos"));
	}

	@Test
	void paraEnviarARevisionFaltanHabitacionesBanosYHoras() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Map<String, Object> datos = datosAlojamiento("Pasto", "Casa", 4, new BigDecimal("80000"), Set.of(),
				"FLEXIBLE");
		datos.remove("habitaciones");
		datos.remove("banos");
		datos.remove("horaEntrada");
		datos.remove("horaSalida");
		Long id = crearBorrador(anfitrion, datos);

		String mensaje = llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 409)
				.get("message").asText();

		assertTrue(mensaje.contains("habitaciones") && mensaje.contains("baños") && mensaje.contains("hora de entrada")
				&& mensaje.contains("hora de salida"), mensaje);
	}

	@Test
	void elDetalleTraeCalificacionReservasYAnfitrionYSoloElAdminVeSuCorreo() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		reservaConfirmada(registrar("HUESPED"), id, 3, 2);

		JsonNode publico = llamar(get("/alojamientos/" + id), null, null, 200);
		assertEquals("Usuario ANFITRION", publico.get("nombreAnfitrion").asText());
		assertEquals(0, publico.get("cantidadResenas").asInt());
		assertTrue(publico.get("promedioCalificacion").isNull());
		assertEquals(1, publico.get("cantidadReservas").asInt());
		assertTrue(publico.get("correoAnfitrion").isNull());

		JsonNode admin = llamar(get("/alojamientos/" + id), tokenAdmin(), null, 200);
		assertEquals(anfitrion.correo(), admin.get("correoAnfitrion").asText());
		assertEquals("ACTIVO", admin.get("estadoAnfitrion").asText());

		JsonNode busqueda = llamar(get("/alojamientos?ciudad=Bogotá"), null, null, 200);
		assertTrue(busqueda.get("contenido").get(0).has("promedioCalificacion"));
	}

	@Test
	void unHuespedNoPuedeCrearPublicaciones_CA131() throws Exception {
		Sesion huesped = registrar("HUESPED");
		crearBorrador(huesped, Map.of("titulo", "X"), 403);
	}

	@Test
	void debeRechazarCapacidadMenorOIgualACero_CA098() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		JsonNode error = crearBorrador(anfitrion, Map.of("titulo", "X", "capacidad", 0), 400);
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

		JsonNode error = llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 409);

		String mensaje = error.get("message").asText();
		assertTrue(mensaje.contains("descripción") && mensaje.contains("capacidad"), mensaje);
		// La imagen ya se exigió al crear el borrador
		assertFalse(mensaje.contains("imagen"), mensaje);
	}

	@Test
	void debeEnviarARevisionYAprobarQuedandoVisibleEnBusquedas_CA157_CA158_CA194() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		String ciudad = unico("CiudadAprobada");
		Long id = crearBorrador(anfitrion, datosAlojamiento(ciudad, "Casa", 2, new BigDecimal("90000"),
				Set.of(), "ESTRICTA"));
		ofrecerFechas(anfitrion, id);

		JsonNode enviado = llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 200);
		assertEquals("PENDIENTE_REVISION", enviado.get("estado").asText());
		assertEquals(0, llamar(get("/alojamientos?ciudad=" + ciudad), null, null, 200).get("totalElementos").asInt());

		JsonNode aprobado = llamar(post("/admin/alojamientos/" + id + "/aprobar"), tokenAdmin(), null, 200);
		assertEquals("ACTIVA", aprobado.get("estado").asText());
		assertEquals(1, llamar(get("/alojamientos?ciudad=" + ciudad), null, null, 200).get("totalElementos").asInt());
	}

	@Test
	void sinFechasDeDisponibilidadNoSePuedeEnviarNiAprobar() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, datosAlojamiento("Cancún", "Casa", 2, new BigDecimal("90000"), Set.of(),
				"FLEXIBLE"));

		String mensaje = llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 409)
				.get("message").asText();
		assertTrue(mensaje.contains("fechas de disponibilidad"), mensaje);

		// Con fechas sí se envía; si las borra mientras está en revisión, no se aprueba
		Long idPeriodo = ofrecerFechas(anfitrion, id);
		llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 200);
		llamar(delete("/alojamientos/" + id + "/disponibilidad/" + idPeriodo), anfitrion.token(), null, 200);
		llamar(post("/admin/alojamientos/" + id + "/aprobar"), tokenAdmin(), null, 409);
	}

	@Test
	void debeRechazarConMotivoNotificarAlAnfitrionYAuditar_CA195_CA196() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion, datosAlojamiento("Cali", "Casa", 2, new BigDecimal("90000"), Set.of(),
				"FLEXIBLE"));
		ofrecerFechas(anfitrion, id);
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
	void sePuedeCrearConDatosComoTextoComoLoEnviaElFront() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		MockMultipartHttpServletRequestBuilder peticion = multipart("/alojamientos");
		// FormData.append('datos', JSON.stringify(...)) llega sin tipo de contenido
		peticion.file(new MockMultipartFile("datos", "", null,
				json.writeValueAsBytes(Map.of("titulo", "Desde el front", "ciudad", "Chía"))));
		peticion.file(imagen("foto.png", PNG));

		JsonNode creado = llamar(peticion, anfitrion.token(), null, 201);

		assertEquals("Desde el front", creado.get("titulo").asText());
		assertEquals("Chía", creado.get("ciudad").asText());
	}

	@Test
	void lasPoliticasDeCancelacionSonPublicas() throws Exception {
		JsonNode politicas = llamar(get("/alojamientos/politicas"), null, null, 200);
		assertTrue(politicas.toString().contains("MODERADA"));
	}

	// ----- ubicación en el mapa (RNF-032, CA-164) -----

	private Map<String, Object> datosSinCoordenadas(String ubicacion, String ciudad) {
		Map<String, Object> datos = datosAlojamiento(ciudad, "Casa", 4, new BigDecimal("150000"), Set.of("wifi"),
				"FLEXIBLE");
		datos.remove("latitud");
		datos.remove("longitud");
		datos.put("ubicacion", ubicacion);
		return datos;
	}

	@Test
	void alCrearSinCoordenadasSeUbicaConLaDireccionYLaCiudad() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		when(geocodingService.ubicar("Calle 70A # 4-41", "Bogotá"))
				.thenReturn(Optional.of(new Coordenadas(4.6546, -74.0579)));

		JsonNode creado = crearBorrador(anfitrion, datosSinCoordenadas("Calle 70A # 4-41", "Bogotá"), 201);

		assertEquals(4.6546, creado.get("latitud").asDouble());
		assertEquals(-74.0579, creado.get("longitud").asDouble());
		JsonNode publico = llamar(get("/alojamientos/" + creado.get("idAlojamiento").asLong()), anfitrion.token(),
				null, 200);
		assertEquals(4.6546, publico.get("latitud").asDouble());
	}

	@Test
	void siNoSeEncuentraLaDireccionElAlojamientoSeCreaSinCoordenadas() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");

		JsonNode creado = crearBorrador(anfitrion, datosSinCoordenadas("Vereda sin nombre", "Ningunaparte"), 201);

		assertTrue(creado.get("latitud").isNull());
		assertTrue(creado.get("longitud").isNull());
	}

	@Test
	void lasCoordenadasEnviadasSeRespetanSinBuscarlas() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");

		JsonNode creado = crearBorrador(anfitrion, datosAlojamiento("Bogotá", "Casa", 4, new BigDecimal("150000"),
				Set.of("wifi"), "FLEXIBLE"), 201);

		assertEquals(4.711, creado.get("latitud").asDouble());
		verify(geocodingService, never()).ubicar(any(), any());
	}

	@Test
	void alCambiarLaDireccionSeVuelveAUbicarYSiNoCambiaNo() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		when(geocodingService.ubicar("Carrera 37 # 8A-24, El Poblado", "Medellín"))
				.thenReturn(Optional.of(new Coordenadas(6.2092, -75.5666)));
		Long id = crearBorrador(anfitrion, datosAlojamiento("Bogotá", "Casa", 4, new BigDecimal("150000"),
				Set.of("wifi"), "FLEXIBLE"));

		JsonNode editado = actualizarAlojamiento(anfitrion, id,
				Map.of("ubicacion", "Carrera 37 # 8A-24, El Poblado", "ciudad", "Medellín"), 200);
		assertEquals(6.2092, editado.get("latitud").asDouble());
		assertEquals(-75.5666, editado.get("longitud").asDouble());

		clearInvocations(geocodingService);
		editado = actualizarAlojamiento(anfitrion, id,
				Map.of("ubicacion", "Carrera 37 # 8A-24, El Poblado", "titulo", "Casa en El Poblado"), 200);
		assertEquals(6.2092, editado.get("latitud").asDouble());
		verify(geocodingService, never()).ubicar(any(), any());
	}
}
