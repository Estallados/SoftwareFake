package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.dto.ReservaDTO;
import co.edu.unbosque.marketplace.dto.SolicitudReservaDTO;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.service.ReservaService;
import co.edu.unbosque.marketplace.util.Resultado;

/** Módulo 4: cotización, reserva con pago de prueba, idempotencia e historial. */
class ReservasPagosTest extends PruebaIntegracionBase {

	@Autowired
	private ReservaService reservaService;

	@Autowired
	private ReservaRepository reservaRepo;

	@Test
	void laReservaTraeLosDatosQueMuestranLasPantallasAunqueElAlojamientoSeBloquee() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion, "Santa Marta", "Cabaña", 4, new BigDecimal("120000"), Set.of(),
				"ESTRICTA");
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 5, 2).get("idReserva").asLong();
		llamar(post("/admin/alojamientos/" + id + "/bloquear"), tokenAdmin(), Map.of("motivo", "Revisión"), 200);

		JsonNode reserva = llamar(get("/reservas/" + idReserva), huesped.token(), null, 200);

		assertEquals("Santa Marta", reserva.get("ciudad").asText());
		assertEquals("Cabaña", reserva.get("tipoAlojamiento").asText());
		assertEquals(4, reserva.get("capacidadAlojamiento").asInt());
		assertEquals("ESTRICTA", reserva.get("nombrePolitica").asText());
		assertEquals("Usuario HUESPED", reserva.get("nombreHuesped").asText());
		assertEquals("Usuario ANFITRION", reserva.get("nombreAnfitrion").asText());
		assertEquals(anfitrion.id(), reserva.get("idAnfitrion").asLong());
		assertTrue(reserva.get("imagenAlojamiento").asText().startsWith(URL_CLOUDINARY + "marketplace/alojamientos/" + id + "/"));
		assertFalse(reserva.get("tieneResena").asBoolean());
		assertTrue(reserva.get("cancelacion").isNull());

		JsonNode deAnfitrion = llamar(get("/reservas/anfitrion"), anfitrion.token(), null, 200);
		assertEquals("Usuario HUESPED", deAnfitrion.get("contenido").get(0).get("nombreHuesped").asText());
	}

	private static BigDecimal valor(JsonNode nodo, String campo) {
		return nodo.get(campo).decimalValue();
	}

	@Test
	void laCotizacionMuestraTodosLosComponentesYElTotalEsSuSuma_CA080_CA081_CA179() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));

		JsonNode c = cotizar(registrar("HUESPED"), id, 10, 3, 2, List.of(), 201);

		assertEquals(3, c.get("noches").asInt());
		assertEquals(0, new BigDecimal("300000.00").compareTo(valor(c, "precioBase")));
		for (String campo : List.of("descuento", "valorLimpieza", "valorServicio", "valorAdicionales", "total")) {
			assertTrue(c.has(campo), campo);
		}
		BigDecimal suma = valor(c, "precioBase").subtract(valor(c, "descuento")).add(valor(c, "valorLimpieza"))
				.add(valor(c, "valorServicio")).add(valor(c, "valorAdicionales"));
		assertEquals(0, suma.compareTo(valor(c, "total")));
	}

	@Test
	void debeRechazarCotizacionConFechasInvalidas_CA082_CA181() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		JsonNode error = cotizar(registrar("HUESPED"), id, 10, 0, 1, List.of(), 400);
		assertTrue(error.get("message").asText().contains("fechas son inválidas"));
	}

	@Test
	void debeRechazarCotizacionQueExcedeLaCapacidad_CA083() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		JsonNode error = cotizar(registrar("HUESPED"), id, 10, 2, 9, List.of(), 400);
		assertTrue(error.get("message").asText().contains("supera la capacidad"));
	}

	@Test
	void debeRechazarCotizacionEnFechasNoDisponibles_CA181() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		reservaConfirmada(registrar("HUESPED"), id, 10, 3);
		JsonNode error = cotizar(registrar("HUESPED"), id, 11, 2, 1, List.of(), 409);
		assertTrue(error.get("message").asText().contains("no está disponible"));
	}

	@Test
	void laCotizacionMuestraIncluyeYExcluyeServiciosAdicionales_CA182_CA183_CA184() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Long desayuno = llamar(post("/alojamientos/" + id + "/servicios"), anfitrion.token(),
				Map.of("nombre", "Desayuno", "costo", 30000), 201).get("idServicio").asLong();
		llamar(post("/alojamientos/" + id + "/servicios"), anfitrion.token(), Map.of("nombre", "Traslado", "costo", 50000),
				201);
		Sesion huesped = registrar("HUESPED");

		JsonNode sin = cotizar(huesped, id, 10, 2, 1, List.of(), 201);
		JsonNode con = cotizar(huesped, id, 10, 2, 1, List.of(desayuno), 201);

		assertEquals(2, sin.get("serviciosDisponibles").size());
		assertEquals(0, BigDecimal.ZERO.compareTo(valor(sin, "valorAdicionales")));
		assertEquals(0, new BigDecimal("30000").compareTo(valor(con, "valorAdicionales")));
		assertEquals(0, valor(sin, "total").add(new BigDecimal("30000")).compareTo(valor(con, "total")));
		assertEquals("Desayuno", con.get("serviciosAdicionales").get(0).get("nombre").asText());
	}

	@Test
	void debeCrearLaReservaConfirmadaConElMontoCotizadoYPagoAprobado_CA070_CA075_CA086_CA161() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		JsonNode cotizacion = cotizar(huesped, id, 10, 2, 2, List.of(), 201);

		JsonNode reserva = reservar(huesped, cotizacion.get("idCotizacion").asLong(), "pm_card_visa", 201);

		assertEquals("CONFIRMADA", reserva.get("estado").asText());
		assertEquals(0, valor(cotizacion, "total").compareTo(valor(reserva, "total")));
		assertEquals("APROBADO", reserva.get("pago").get("estado").asText());
		assertEquals("SIMULADO", reserva.get("pago").get("proveedor").asText());
		assertTrue(reserva.get("desglose").asText().contains("valorLimpieza"));
		assertTrue(llamar(get("/notificaciones"), anfitrion.token(), null, 200).toString()
				.contains("RESERVA_CONFIRMADA"));
	}

	@Test
	void debeRechazarReservaSiElAlojamientoDejoDeEstarActivo_CA071_CA159() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		Long idCotizacion = cotizar(huesped, id, 10, 2, 1, List.of(), 201).get("idCotizacion").asLong();
		llamar(post("/alojamientos/" + id + "/pausar"), anfitrion.token(), null, 200);

		JsonNode error = reservar(huesped, idCotizacion, "pm_card_visa", 409);

		assertTrue(error.get("message").asText().contains("PAUSADA"));
	}

	@Test
	void debeRevalidarYRechazarUnPeriodoQueOtroHuespedYaConfirmo_CA072_CA074() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion primero = registrar("HUESPED");
		Sesion segundo = registrar("HUESPED");
		Long cotizacionPrimero = cotizar(primero, id, 10, 3, 1, List.of(), 201).get("idCotizacion").asLong();
		Long cotizacionSegundo = cotizar(segundo, id, 11, 3, 1, List.of(), 201).get("idCotizacion").asLong();

		reservar(primero, cotizacionPrimero, "pm_card_visa", 201);
		JsonNode error = reservar(segundo, cotizacionSegundo, "pm_card_visa", 409);

		assertTrue(error.get("message").asText().contains("ya no está disponible"));
	}

	@Test
	void dosSolicitudesSimultaneasParaElMismoPeriodoSoloConfirmanUna_CA073() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		List<Sesion> huespedes = new ArrayList<>();
		List<Long> cotizaciones = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Sesion huesped = registrar("HUESPED");
			huespedes.add(huesped);
			cotizaciones.add(cotizar(huesped, id, 40, 2, 1, List.of(), 201).get("idCotizacion").asLong());
		}

		ExecutorService hilos = Executors.newFixedThreadPool(4);
		CountDownLatch salida = new CountDownLatch(1);
		List<Future<Resultado<ReservaDTO>>> resultados = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			SolicitudReservaDTO solicitud = new SolicitudReservaDTO();
			solicitud.setIdCotizacion(cotizaciones.get(i));
			Long idHuesped = huespedes.get(i).id();
			Callable<Resultado<ReservaDTO>> tarea = () -> {
				salida.await();
				return reservaService.crear(solicitud, idHuesped);
			};
			resultados.add(hilos.submit(tarea));
		}
		salida.countDown();
		int confirmadas = 0;
		for (Future<Resultado<ReservaDTO>> resultado : resultados) {
			if (resultado.get().esExito()) {
				confirmadas++;
			}
		}
		hilos.shutdown();

		assertEquals(1, confirmadas);
		assertEquals(1, reservaRepo.findAll().stream().filter(r -> r.getIdAlojamiento().equals(id)
				&& r.getEstado() == EstadoReserva.CONFIRMADA).count());
	}

	@Test
	void unPagoRechazadoNoDejaLaReservaPagadaYLiberaLasFechas_CA162() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		Long idCotizacion = cotizar(huesped, id, 10, 2, 1, List.of(), 201).get("idCotizacion").asLong();

		JsonNode respuesta = reservar(huesped, idCotizacion, "pm_card_chargeDeclined", 402);

		assertTrue(respuesta.get("message").asText().contains("rechazado"));
		assertEquals("CANCELADA", respuesta.get("dato").get("estado").asText());
		assertEquals("RECHAZADO", respuesta.get("dato").get("pago").get("estado").asText());
		// La cotización vuelve a servir y las fechas siguen libres: se puede reintentar
		assertEquals("CONFIRMADA", reservar(huesped, idCotizacion, "pm_card_visa", 201).get("estado").asText());
	}

	@Test
	void elRechazoDiceElMotivoYUnMetodoQueNoEsDeStripeNiSeIntenta_CA162() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		Long idCotizacion = cotizar(huesped, id, 10, 2, 1, List.of(), 201).get("idCotizacion").asLong();

		JsonNode error = reservar(huesped, idCotizacion, "4242424242424242", 400);
		assertTrue(error.get("message").asText().contains("pm_"));
		assertEquals(0, reservaRepo.findAll().stream().filter(r -> r.getIdAlojamiento().equals(id)).count());

		JsonNode rechazo = reservar(huesped, idCotizacion, "pm_card_chargeDeclinedInsufficientFunds", 402);
		assertTrue(rechazo.get("message").asText().contains("fondos suficientes"));
		assertEquals("pm_card_chargeDeclinedInsufficientFunds", rechazo.get("dato").get("pago").get("metodo")
				.asText());
	}

	@Test
	void unErrorDePagoInformaQueNoSeConfirmo_CA163() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		Long idCotizacion = cotizar(huesped, id, 10, 2, 1, List.of(), 201).get("idCotizacion").asLong();

		JsonNode respuesta = reservar(huesped, idCotizacion, "pm_card_error", 502);

		assertTrue(respuesta.get("message").asText().contains("No se pudo confirmar el pago"));
		assertFalse("APROBADO".equals(respuesta.get("dato").get("pago").get("estado").asText()));
	}

	@Test
	void reenviarLaMismaSolicitudNoCreaUnaSegundaReserva_CA140_CA141() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		Long idCotizacion = cotizar(huesped, id, 10, 2, 1, List.of(), 201).get("idCotizacion").asLong();
		Map<String, Object> solicitud = Map.of("idCotizacion", idCotizacion, "metodoPago", "pm_card_visa");

		JsonNode primera = llamar(post("/reservas").header("Idempotency-Key", "reintento-1"), huesped.token(),
				solicitud, 201);
		JsonNode segunda = llamar(post("/reservas").header("Idempotency-Key", "reintento-1"), huesped.token(),
				solicitud, 201);

		assertEquals(primera.get("idReserva").asLong(), segunda.get("idReserva").asLong());
		assertEquals(1, reservaRepo.findAll().stream().filter(r -> r.getIdAlojamiento().equals(id)).count());
		llamar(post("/reservas"), huesped.token(), solicitud, 400);
	}

	@Test
	void elHuespedVeSuHistorialYElAnfitrionSoloLasDeSusAlojamientos_CA078_CA079_CA133() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Sesion otroAnfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		Long idReserva = reservaConfirmada(huesped, id, 10, 2).get("idReserva").asLong();

		JsonNode mias = llamar(get("/reservas/mias"), huesped.token(), null, 200);
		assertEquals(1, mias.get("totalElementos").asInt());
		assertEquals("CONFIRMADA", mias.get("contenido").get(0).get("estado").asText());
		assertEquals(1, llamar(get("/reservas/anfitrion"), anfitrion.token(), null, 200).get("totalElementos").asInt());
		assertEquals(0, llamar(get("/reservas/anfitrion"), otroAnfitrion.token(), null, 200).get("totalElementos")
				.asInt());

		llamar(get("/reservas/" + idReserva), anfitrion.token(), null, 200);
		llamar(get("/reservas/" + idReserva), registrar("HUESPED").token(), null, 404);
		llamar(get("/reservas/" + idReserva), otroAnfitrion.token(), null, 404);
	}

	@Test
	void modificarUnServicioAdicionalNoAlteraUnaReservaConfirmada_CA186() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion, "Bogotá", "Casa", 4, new BigDecimal("100000"), Set.of(),
				"FLEXIBLE");
		Long idServicio = llamar(post("/alojamientos/" + id + "/servicios"), anfitrion.token(),
				Map.of("nombre", "Desayuno", "costo", 30000), 201).get("idServicio").asLong();
		Sesion huesped = registrar("HUESPED");
		Long idCotizacion = cotizar(huesped, id, 10, 2, 1, List.of(idServicio), 201).get("idCotizacion").asLong();
		JsonNode reserva = reservar(huesped, idCotizacion, "pm_card_visa", 201);

		llamar(put("/alojamientos/" + id + "/servicios/" + idServicio), anfitrion.token(),
				Map.of("nombre", "Desayuno premium", "costo", 90000), 200);

		JsonNode despues = llamar(get("/reservas/" + reserva.get("idReserva").asLong()), huesped.token(), null, 200);
		assertEquals(0, valor(reserva, "total").compareTo(valor(despues, "total")));
		assertTrue(despues.get("desglose").asText().contains("30000"));
		assertEquals(0, new BigDecimal("90000").compareTo(valor(
				cotizar(huesped, id, 20, 2, 1, List.of(idServicio), 201), "valorAdicionales")));
	}

	@Test
	void unAnfitrionNoPuedeCrearReservas_CA131() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		llamar(post("/reservas").header("Idempotency-Key", "x"), anfitrion.token(), Map.of("idCotizacion", 1), 403);
	}
}
