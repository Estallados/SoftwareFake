package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.service.CalendarioService;

/** Módulo 3: períodos de disponibilidad, consulta por fechas y búsqueda pública. */
class DisponibilidadBusquedaTest extends PruebaIntegracionBase {

	private String consulta(Long id, int desdeHoy, int noches) {
		return "/alojamientos/" + id + "/disponibilidad/consultar?llegada=" + HOY.plusDays(desdeHoy) + "&salida="
				+ HOY.plusDays(desdeHoy + (long) noches);
	}

	@Test
	void debeRegistrarUnPeriodoAsociadoAlAlojamiento_CA115_CA116() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = llamar(post("/alojamientos"), anfitrion.token(), Map.of("titulo", "Con calendario"), 201)
				.get("idAlojamiento").asLong();

		JsonNode periodo = llamar(post("/alojamientos/" + id + "/disponibilidad"), anfitrion.token(),
				Map.of("fechaInicio", HOY.plusDays(1).toString(), "fechaFin", HOY.plusDays(30).toString()), 201);

		assertEquals(id, periodo.get("idAlojamiento").asLong());
		assertEquals(HOY.plusDays(30).toString(), periodo.get("fechaFin").asText());
		llamar(post("/alojamientos/" + id + "/disponibilidad"), registrar("ANFITRION").token(),
				Map.of("fechaInicio", HOY.plusDays(40).toString(), "fechaFin", HOY.plusDays(50).toString()), 403);
	}

	@Test
	void debeActualizarUnPeriodoNoReservado_CA117() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Long idPeriodo = llamar(get("/alojamientos/" + id + "/disponibilidad"), null, null, 200).get("periodos")
				.get(0).get("idPeriodo").asLong();

		llamar(put("/alojamientos/" + id + "/disponibilidad/" + idPeriodo), anfitrion.token(),
				Map.of("fechaInicio", HOY.toString(), "fechaFin", HOY.plusDays(60).toString()), 200);

		JsonNode calendario = llamar(get("/alojamientos/" + id + "/disponibilidad"), null, null, 200);
		assertEquals(HOY.plusDays(60).toString(), calendario.get("periodos").get(0).get("fechaFin").asText());
	}

	@Test
	void noDebePermitirModificarOEliminarUnPeriodoQueAfecteUnaReserva_CA118() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		reservaConfirmada(registrar("HUESPED"), id, 20, 3);
		Long idPeriodo = llamar(get("/alojamientos/" + id + "/disponibilidad"), null, null, 200).get("periodos")
				.get(0).get("idPeriodo").asLong();

		JsonNode error = llamar(put("/alojamientos/" + id + "/disponibilidad/" + idPeriodo), anfitrion.token(),
				Map.of("fechaInicio", HOY.toString(), "fechaFin", HOY.plusDays(21).toString()), 409);
		assertTrue(error.get("message").asText().contains("afecta la reserva"));
		llamar(delete("/alojamientos/" + id + "/disponibilidad/" + idPeriodo), anfitrion.token(), null, 409);
	}

	@Test
	void debeMostrarDisponibleUnPeriodoLibre_CA119_CA121() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));

		JsonNode respuesta = llamar(get(consulta(id, 10, 3)), null, null, 200);

		assertTrue(respuesta.get("disponible").asBoolean());
	}

	@Test
	void debeIndicarNoDisponibleSiHayReservaOFueraDelPeriodo_CA120() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		reservaConfirmada(registrar("HUESPED"), id, 10, 3);

		JsonNode cruce = llamar(get(consulta(id, 11, 2)), null, null, 200);
		assertFalse(cruce.get("disponible").asBoolean());
		assertTrue(cruce.get("motivo").asText().contains("reserva"));

		JsonNode fuera = llamar(get(consulta(id, 200, 2)), null, null, 200);
		assertFalse(fuera.get("disponible").asBoolean());
	}

	@Test
	void debeRechazarSalidaIgualOAnteriorALaLlegada_CA122_CA082() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		String url = "/alojamientos/" + id + "/disponibilidad/consultar?llegada=" + HOY.plusDays(5) + "&salida=";

		JsonNode igual = llamar(get(url + HOY.plusDays(5)), null, null, 400);
		assertTrue(igual.get("message").asText().contains("fechas son inválidas"));
		llamar(get(url + HOY.plusDays(4)), null, null, 400);
		assertTrue(CalendarioService.validarFechas(HOY.plusDays(5), HOY.plusDays(4)) != null);
	}

	@Test
	void unaReservaConfirmadaBloqueaElPeriodoEnElCalendario_CA076_CA123() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		reservaConfirmada(registrar("HUESPED"), id, 30, 4);

		JsonNode calendario = llamar(get("/alojamientos/" + id + "/disponibilidad"), null, null, 200);

		JsonNode reservado = calendario.get("reservados").get(0);
		assertEquals(HOY.plusDays(30).toString(), reservado.get("fechaEntrada").asText());
		assertEquals(HOY.plusDays(34).toString(), reservado.get("fechaSalida").asText());
		assertEquals("ACTIVA", calendario.get("estadoAlojamiento").asText());
	}

	@Test
	void debeBuscarPorCiudad_CA087() throws Exception {
		String ciudad = unico("Ciudad");
		Long id = crearAlojamientoActivo(registrar("ANFITRION"), ciudad, "Casa", 2, new BigDecimal("80000"),
				Set.of(), "FLEXIBLE");

		JsonNode resultado = llamar(get("/alojamientos?ciudad=" + ciudad.toUpperCase()), null, null, 200);

		assertEquals(1, resultado.get("totalElementos").asInt());
		assertEquals(id, resultado.get("contenido").get(0).get("idAlojamiento").asLong());
	}

	@Test
	void debeBuscarPorFechasExcluyendoLosOcupados_CA088_CA077() throws Exception {
		String ciudad = unico("Fechas");
		Sesion anfitrion = registrar("ANFITRION");
		Long libre = crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("80000"), Set.of(),
				"FLEXIBLE");
		Long ocupado = crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("80000"), Set.of(),
				"FLEXIBLE");
		reservaConfirmada(registrar("HUESPED"), ocupado, 15, 3);

		JsonNode resultado = llamar(get("/alojamientos?ciudad=" + ciudad + "&llegada=" + HOY.plusDays(16)
				+ "&salida=" + HOY.plusDays(18)), null, null, 200);

		assertEquals(1, resultado.get("totalElementos").asInt());
		assertEquals(libre, resultado.get("contenido").get(0).get("idAlojamiento").asLong());
	}

	@Test
	void debeBuscarPorCapacidad_CA089() throws Exception {
		String ciudad = unico("Capacidad");
		Sesion anfitrion = registrar("ANFITRION");
		crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("80000"), Set.of(), "FLEXIBLE");
		Long grande = crearAlojamientoActivo(anfitrion, ciudad, "Casa", 8, new BigDecimal("80000"), Set.of(),
				"FLEXIBLE");

		JsonNode resultado = llamar(get("/alojamientos?ciudad=" + ciudad + "&huespedes=5"), null, null, 200);

		assertEquals(1, resultado.get("totalElementos").asInt());
		assertEquals(grande, resultado.get("contenido").get(0).get("idAlojamiento").asLong());
	}

	@Test
	void debeBuscarPorTipo_CA090() throws Exception {
		String ciudad = unico("Tipo");
		Sesion anfitrion = registrar("ANFITRION");
		crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("80000"), Set.of(), "FLEXIBLE");
		Long cabana = crearAlojamientoActivo(anfitrion, ciudad, "Cabaña", 2, new BigDecimal("80000"), Set.of(),
				"FLEXIBLE");

		JsonNode resultado = llamar(get("/alojamientos?ciudad=" + ciudad + "&tipo=cabaña"), null, null, 200);

		assertEquals(1, resultado.get("totalElementos").asInt());
		assertEquals(cabana, resultado.get("contenido").get(0).get("idAlojamiento").asLong());
	}

	@Test
	void debeBuscarPorRangoDePrecio_CA091() throws Exception {
		String ciudad = unico("Precio");
		Sesion anfitrion = registrar("ANFITRION");
		crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("50000"), Set.of(), "FLEXIBLE");
		Long medio = crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("150000"), Set.of(),
				"FLEXIBLE");
		crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("500000"), Set.of(), "FLEXIBLE");

		JsonNode resultado = llamar(get("/alojamientos?ciudad=" + ciudad + "&precioMinimo=100000&precioMaximo=200000"),
				null, null, 200);

		assertEquals(1, resultado.get("totalElementos").asInt());
		assertEquals(medio, resultado.get("contenido").get(0).get("idAlojamiento").asLong());
		llamar(get("/alojamientos?precioMinimo=300&precioMaximo=100"), null, null, 400);
	}

	@Test
	void debeBuscarPorServicios_CA092() throws Exception {
		String ciudad = unico("Servicios");
		Sesion anfitrion = registrar("ANFITRION");
		crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("80000"), Set.of("wifi"), "FLEXIBLE");
		Long completo = crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("80000"),
				Set.of("wifi", "piscina"), "FLEXIBLE");

		JsonNode resultado = llamar(get("/alojamientos?ciudad=" + ciudad + "&amenidades=wifi&amenidades=Piscina"),
				null, null, 200);

		assertEquals(1, resultado.get("totalElementos").asInt());
		assertEquals(completo, resultado.get("contenido").get(0).get("idAlojamiento").asLong());
	}

	@Test
	void debeCombinarTodosLosFiltrosYMostrarSoloActivas_CA190() throws Exception {
		String ciudad = unico("Combinada");
		Sesion anfitrion = registrar("ANFITRION");
		Long ideal = crearAlojamientoActivo(anfitrion, ciudad, "Loft", 4, new BigDecimal("120000"),
				Set.of("wifi", "parqueadero"), "FLEXIBLE");
		crearAlojamientoActivo(anfitrion, ciudad, "Loft", 4, new BigDecimal("120000"), Set.of("wifi"), "FLEXIBLE");
		Long pausado = crearAlojamientoActivo(anfitrion, ciudad, "Loft", 4, new BigDecimal("120000"),
				Set.of("wifi", "parqueadero"), "FLEXIBLE");
		llamar(post("/alojamientos/" + pausado + "/pausar"), anfitrion.token(), null, 200);

		JsonNode resultado = llamar(get("/alojamientos?ciudad=" + ciudad + "&tipo=loft&huespedes=3&precioMinimo=100000"
				+ "&precioMaximo=130000&amenidades=parqueadero&llegada=" + HOY.plusDays(5) + "&salida="
				+ HOY.plusDays(7)), null, null, 200);

		assertEquals(1, resultado.get("totalElementos").asInt());
		assertEquals(ideal, resultado.get("contenido").get(0).get("idAlojamiento").asLong());
	}

	@Test
	void laBusquedaEsPaginada_RNF015() throws Exception {
		JsonNode resultado = llamar(get("/alojamientos?tamanio=1"), null, null, 200);
		assertEquals(1, resultado.get("tamanio").asInt());
		assertTrue(resultado.has("totalPaginas"));
	}
}
