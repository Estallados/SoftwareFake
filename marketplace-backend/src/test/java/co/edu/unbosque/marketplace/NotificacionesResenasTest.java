package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.Resena;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;

/** Módulo 6: notificaciones y reseñas. */
class NotificacionesResenasTest extends PruebaIntegracionBase {

	@Autowired
	private ReservaRepository reservaRepo;

	@Autowired
	private ResenaRepository resenaRepo;

	/** Reserva ya terminada (la salida fue ayer), creada directo en la base. */
	private Long reservaCompletada(Sesion huesped, Long idAlojamiento) {
		Reserva reserva = new Reserva();
		reserva.setIdHuesped(huesped.id());
		reserva.setIdAlojamiento(idAlojamiento);
		reserva.setFechaEntrada(HOY.minusDays(4));
		reserva.setFechaSalida(HOY.minusDays(1));
		reserva.setTotal(new BigDecimal("300000"));
		reserva.setEstado(EstadoReserva.CONFIRMADA);
		reserva.setFechaCreacion(LocalDateTime.now().minusDays(10));
		return reservaRepo.save(reserva).getIdReserva();
	}

	private JsonNode resenar(Sesion huesped, Long idReserva, int calificacion, int estado) throws Exception {
		return llamar(post("/resenas"), huesped.token(),
				Map.of("idReserva", idReserva, "calificacion", calificacion, "comentario", "Muy buena estancia"),
				estado);
	}

	@Test
	void unEventoRelevanteRegistraUnaNotificacionParaElUsuario_CA010() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);

		JsonNode notificaciones = llamar(get("/notificaciones"), anfitrion.token(), null, 200);

		assertTrue(notificaciones.get("contenido").toString().contains("ALOJAMIENTO_APROBADO"));
		assertTrue(notificaciones.get("contenido").toString().contains(
				llamar(get("/alojamientos/" + id), null, null, 200).get("titulo").asText()));
	}

	@Test
	void cadaUsuarioVeYMarcaSoloSusNotificaciones_CA013() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		crearAlojamientoActivo(anfitrion);
		Sesion otro = registrar("HUESPED");
		Long idNotificacion = llamar(get("/notificaciones"), anfitrion.token(), null, 200).get("contenido").get(0)
				.get("idNotificacion").asLong();

		assertEquals(0, llamar(get("/notificaciones"), otro.token(), null, 200).get("totalElementos").asInt());
		llamar(patch("/notificaciones/" + idNotificacion + "/leida"), otro.token(), null, 404);
		JsonNode leida = llamar(patch("/notificaciones/" + idNotificacion + "/leida"), anfitrion.token(), null, 200);
		assertTrue(leida.get("leida").asBoolean());
	}

	@Test
	void elHuespedDeUnaReservaCompletadaPuedeResenarYQuedaAsociada_CA016_CA018() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		Long idReserva = reservaCompletada(huesped, id);

		JsonNode resena = resenar(huesped, idReserva, 5, 201);

		Resena guardada = resenaRepo.findById(resena.get("idResena").asLong()).orElseThrow();
		assertEquals(huesped.id(), guardada.getIdUsuario());
		assertEquals(idReserva, guardada.getIdReserva());
		assertEquals(id, guardada.getIdAlojamiento());
		assertEquals("Muy buena estancia", guardada.getComentario());
		assertTrue(guardada.getFecha() != null);
		assertTrue(llamar(get("/notificaciones"), anfitrion.token(), null, 200).toString().contains("RESENA_RECIBIDA"));
		resenar(huesped, idReserva, 4, 409);
	}

	@Test
	void noSePuedeResenarUnaReservaNoCompletadaNiConCalificacionInvalida_CA017() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		Long futura = reservaConfirmada(huesped, id, 10, 2).get("idReserva").asLong();

		JsonNode error = resenar(huesped, futura, 5, 409);
		assertTrue(error.get("message").asText().contains("completada"));
		resenar(huesped, reservaCompletada(huesped, id), 6, 400);
		resenar(registrar("HUESPED"), reservaCompletada(huesped, id), 5, 404);
	}

	@Test
	void laConsultaPorPublicacionMuestraSoloSusResenasConPromedio_CA019_CA020() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Long otroAlojamiento = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		resenar(huesped, reservaCompletada(huesped, id), 5, 201);
		resenar(huesped, reservaCompletada(huesped, id), 4, 201);
		resenar(huesped, reservaCompletada(huesped, otroAlojamiento), 1, 201);

		JsonNode resumen = llamar(get("/alojamientos/" + id + "/resenas"), null, null, 200);

		assertEquals(2, resumen.get("cantidad").asLong());
		assertEquals(4.5, resumen.get("promedio").asDouble());
		assertEquals(2, resumen.get("resenas").get("totalElementos").asInt());
	}
}
