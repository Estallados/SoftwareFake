package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.Cancelacion;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.CancelacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;

/** Módulo 5: cancelación con política, devolución simulada y liberación del período. */
class CancelacionesTest extends PruebaIntegracionBase {

	@Autowired
	private CancelacionRepository cancelacionRepo;

	@Autowired
	private ReservaRepository reservaRepo;

	private JsonNode cancelar(Sesion huesped, long idReserva, Map<String, Object> cuerpo, String clave, int estado)
			throws Exception {
		return llamar(post("/reservas/" + idReserva + "/cancelar").header("Idempotency-Key", clave), huesped.token(),
				cuerpo, estado);
	}

	@Test
	void debeIdentificarYAplicarLaPoliticaDelAlojamientoConSuDevolucion_CA001_CA002_CA003() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"), "Bogotá", "Casa", 4, new BigDecimal("100000"),
				Set.of(), "MODERADA");
		Sesion huesped = registrar("HUESPED");
		JsonNode reserva = reservaConfirmada(huesped, id, 2, 2);

		JsonNode cancelacion = cancelar(huesped, reserva.get("idReserva").asLong(),
				Map.of("motivo", "Cambio de planes"), unico("c-"), 200);

		assertEquals("MODERADA", cancelacion.get("politica").asText());
		assertEquals(50, cancelacion.get("porcentajeReembolso").asInt());
		BigDecimal esperado = reserva.get("total").decimalValue().divide(BigDecimal.valueOf(2), 2,
				RoundingMode.HALF_UP);
		assertEquals(0, esperado.compareTo(cancelacion.get("valorReembolso").decimalValue()));
	}

	@Test
	void laSimulacionMuestraLaDevolucionSinCancelarYCoincideConLaReal_CA002_CA003() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"), "Bogotá", "Casa", 4, new BigDecimal("100000"),
				Set.of(), "MODERADA");
		Sesion huesped = registrar("HUESPED");
		JsonNode reserva = reservaConfirmada(huesped, id, 2, 2);
		long idReserva = reserva.get("idReserva").asLong();

		JsonNode simulacion = llamar(get("/reservas/" + idReserva + "/simularcancelacion"), huesped.token(), null,
				200);
		assertEquals("MODERADA", simulacion.get("politica").asText());
		assertEquals(2, simulacion.get("diasAnticipacion").asInt());
		assertEquals(50, simulacion.get("porcentajeReembolso").asInt());
		assertEquals(0, reserva.get("total").decimalValue().compareTo(simulacion.get("totalPagado").decimalValue()));
		assertEquals(EstadoReserva.CONFIRMADA, reservaRepo.findById(idReserva).orElseThrow().getEstado());

		JsonNode cancelacion = cancelar(huesped, idReserva, Map.of("motivo", "Cambio de planes"), unico("c-"), 200);
		assertEquals(0, simulacion.get("valorReembolso").decimalValue()
				.compareTo(cancelacion.get("valorReembolso").decimalValue()));

		// Ya cancelada no se puede simular, y otro usuario nunca ve la reserva
		llamar(get("/reservas/" + idReserva + "/simularcancelacion"), huesped.token(), null, 409);
		llamar(get("/reservas/" + idReserva + "/simularcancelacion"), registrar("HUESPED").token(), null, 404);
	}

	@Test
	void laReservaCanceladaMuestraFechaMotivoYDevolucion() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 10, 2).get("idReserva").asLong();
		cancelar(huesped, idReserva, Map.of("motivo", "Encontré otra opción"), unico("c-"), 200);

		JsonNode detalle = llamar(get("/reservas/" + idReserva), huesped.token(), null, 200);

		assertEquals("Encontré otra opción", detalle.get("cancelacion").get("motivo").asText());
		assertNotNull(detalle.get("cancelacion").get("fecha").asText());
		assertTrue(detalle.get("cancelacion").get("valorReembolso").decimalValue().signum() > 0);
	}

	@Test
	void debeRegistrarUsuarioFechaMotivoDevolucionYEstadoFinal_CA004_CA005_CA006() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"), "Bogotá", "Casa", 4, new BigDecimal("100000"),
				Set.of(), "FLEXIBLE");
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 10, 2).get("idReserva").asLong();

		cancelar(huesped, idReserva, Map.of("motivo", "Enfermedad"), unico("c-"), 200);

		Cancelacion registro = cancelacionRepo.findByIdReserva(idReserva).orElseThrow();
		assertEquals(huesped.id(), registro.getIdUsuario());
		assertNotNull(registro.getFecha());
		assertEquals("Enfermedad", registro.getMotivo());
		assertEquals(100, registro.getPorcentajeReembolso());
		assertEquals(EstadoReserva.CANCELADA, registro.getEstadoFinalReserva());
		assertEquals(EstadoReserva.CANCELADA, reservaRepo.findById(idReserva).orElseThrow().getEstado());
	}

	@Test
	void debeLiberarElPeriodoYMostrarloDisponibleDeNuevo_CA007_CA008() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 10, 3).get("idReserva").asLong();
		String consulta = "/alojamientos/" + id + "/disponibilidad/consultar?llegada=" + HOY.plusDays(10)
				+ "&salida=" + HOY.plusDays(13);
		assertEquals(false, llamar(get(consulta), null, null, 200).get("disponible").asBoolean());

		cancelar(huesped, idReserva, Map.of("motivo", "Ya no viajo"), unico("c-"), 200);

		assertTrue(llamar(get(consulta), null, null, 200).get("disponible").asBoolean());
		assertTrue(llamar(get("/alojamientos/" + id + "/disponibilidad"), null, null, 200).get("reservados").isEmpty());
	}

	@Test
	void noDebeCancelarReservasQueYaEmpezaronOYaFueronCanceladas_CA009() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		Reserva enCurso = new Reserva();
		enCurso.setIdHuesped(huesped.id());
		enCurso.setIdAlojamiento(id);
		enCurso.setFechaEntrada(HOY);
		enCurso.setFechaSalida(HOY.plusDays(2));
		enCurso.setTotal(new BigDecimal("100000"));
		enCurso.setEstado(EstadoReserva.CONFIRMADA);
		enCurso.setFechaCreacion(LocalDateTime.now());
		enCurso = reservaRepo.save(enCurso);

		JsonNode error = cancelar(huesped, enCurso.getIdReserva(), Map.of("motivo", "Tarde"), unico("c-"), 409);
		assertTrue(error.get("message").asText().contains("reservas futuras"));

		long futura = reservaConfirmada(huesped, id, 20, 2).get("idReserva").asLong();
		cancelar(huesped, futura, Map.of("motivo", "Primera vez"), unico("c-"), 200);
		cancelar(huesped, futura, Map.of("motivo", "Segunda vez"), unico("c-"), 409);
	}

	@Test
	void elMotivoEsObligatorioYSoloElHuespedPuedeCancelar() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 10, 2).get("idReserva").asLong();

		cancelar(huesped, idReserva, Map.of(), unico("c-"), 400);
		cancelar(registrar("HUESPED"), idReserva, Map.of("motivo", "No es mía"), unico("c-"), 404);
	}

	@Test
	void cancelarEsIdempotenteYNotificaAlAnfitrion_CA140() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 10, 2).get("idReserva").asLong();

		JsonNode primera = cancelar(huesped, idReserva, Map.of("motivo", "Viaje"), "cancelar-1", 200);
		JsonNode segunda = cancelar(huesped, idReserva, Map.of("motivo", "Viaje"), "cancelar-1", 200);

		assertEquals(primera.get("idCancelacion").asLong(), segunda.get("idCancelacion").asLong());
		assertTrue(llamar(get("/notificaciones"), anfitrion.token(), null, 200).toString()
				.contains("RESERVA_CANCELADA"));
		assertEquals("REEMBOLSADO", llamar(get("/reservas/" + idReserva), huesped.token(), null, 200).get("pago")
				.get("estado").asText());
	}
}
