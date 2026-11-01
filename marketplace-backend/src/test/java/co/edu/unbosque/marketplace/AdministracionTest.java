package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.service.AuditoriaService;

/** Módulo 7: bloqueo de publicaciones, restricción de usuarios, moderación de reseñas y auditoría. */
class AdministracionTest extends PruebaIntegracionBase {

	@Autowired
	private ReservaRepository reservaRepo;

	@Test
	void bloquearCambiaABloqueadaYLaOcultaDeLasBusquedas_CA054_CA055_CA111() throws Exception {
		String ciudad = unico("Bloqueo");
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion, ciudad, "Casa", 2, new BigDecimal("90000"), Set.of(), "FLEXIBLE");
		String admin = tokenAdmin();

		llamar(post("/admin/alojamientos/" + id + "/bloquear"), admin, Map.of(), 400);
		JsonNode bloqueado = llamar(post("/admin/alojamientos/" + id + "/bloquear"), admin,
				Map.of("motivo", "Publicidad engañosa"), 200);

		assertEquals("BLOQUEADA", bloqueado.get("estado").asText());
		assertEquals(0, llamar(get("/alojamientos?ciudad=" + ciudad), null, null, 200).get("totalElementos").asInt());
		cotizar(registrar("HUESPED"), id, 5, 2, 1, List.of(), 409);
		JsonNode desbloqueado = llamar(post("/admin/alojamientos/" + id + "/desbloquear"), admin, null, 200);
		assertEquals("PAUSADA", desbloqueado.get("estado").asText());
	}

	@Test
	void alBloquearSeConservanLasReservasFuturasYQuedanEnRevision_CA112() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 15, 2).get("idReserva").asLong();

		llamar(post("/admin/alojamientos/" + id + "/bloquear"), tokenAdmin(), Map.of("motivo", "Revisión"), 200);

		assertEquals(EstadoReserva.EN_REVISION, reservaRepo.findById(idReserva).orElseThrow().getEstado());
		assertTrue(llamar(get("/notificaciones"), huesped.token(), null, 200).toString()
				.contains("RESERVA_EN_REVISION"));
		assertTrue(llamar(get("/notificaciones"), anfitrion.token(), null, 200).toString()
				.contains("ALOJAMIENTO_BLOQUEADO"));
	}

	@Test
	void restringirUnUsuarioRegistraLaRestriccionYBloqueaSusAcciones_CA058_CA059_CA060() throws Exception {
		Sesion huesped = registrar("HUESPED");
		String admin = tokenAdmin();

		llamar(patch("/admin/usuarios/" + huesped.id() + "/restringir"), admin, Map.of(), 400);
		JsonNode restringido = llamar(patch("/admin/usuarios/" + huesped.id() + "/restringir"), admin,
				Map.of("motivo", "Fraude"), 200);
		assertEquals("RESTRINGIDO", restringido.get("estado").asText());

		llamar(get("/usuarios/perfil"), huesped.token(), null, 401);
		llamar(post("/auth/login"), null, Map.of("correo", huesped.correo(), "contrasenia", CLAVE), 403);
		JsonNode consulta = llamar(get("/usuarios/obtener/" + huesped.id()), admin, null, 200);
		assertEquals("RESTRINGIDO", consulta.get("estado").asText());
		assertEquals("Fraude", consulta.get("motivoRestriccion").asText());

		llamar(patch("/admin/usuarios/" + huesped.id() + "/liberar"), admin, Map.of("motivo", "Aclarado"), 200);
		llamar(post("/auth/login"), null, Map.of("correo", huesped.correo(), "contrasenia", CLAVE), 200);

		// Liberar no exige motivo: devolver el acceso no perjudica al usuario
		llamar(patch("/admin/usuarios/" + huesped.id() + "/restringir"), admin, Map.of("motivo", "Revisión"), 200);
		llamar(patch("/admin/usuarios/" + huesped.id() + "/liberar"), admin, null, 200);
	}

	@Test
	void elAdminConsultaTodasLasReservasYLasDeCadaUsuario() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 3, 2).get("idReserva").asLong();
		String admin = tokenAdmin();

		JsonNode confirmadas = llamar(get("/admin/reservas?estado=CONFIRMADA&tamanio=100"), admin, null, 200);
		assertTrue(confirmadas.get("contenido").toString().contains("\"idReserva\":" + idReserva));
		JsonNode fila = confirmadas.get("contenido").get(0);
		for (String campo : new String[] { "nombreHuesped", "tituloAlojamiento", "ciudad", "nombreAnfitrion",
				"fechaEntrada", "fechaSalida", "noches", "total", "estado" }) {
			assertTrue(fila.hasNonNull(campo), campo);
		}
		assertFalse(fila.has("desglose") || fila.has("pago") || fila.has("cancelacion"));
		JsonNode canceladas = llamar(get("/admin/reservas?estado=CANCELADA&tamanio=100"), admin, null, 200);
		assertFalse(canceladas.get("contenido").toString().contains("\"idReserva\":" + idReserva));

		JsonNode delHuesped = llamar(get("/admin/usuarios/" + huesped.id() + "/reservas"), admin, null, 200);
		assertEquals(1, delHuesped.get("totalElementos").asInt());
		JsonNode delAnfitrion = llamar(get("/admin/usuarios/" + anfitrion.id() + "/alojamientos"), admin, null, 200);
		assertEquals(1, delAnfitrion.get("totalElementos").asInt());
		assertEquals(anfitrion.correo(), delAnfitrion.get("contenido").get(0).get("correoAnfitrion").asText());
		// Un usuario que no existe es 404, no una lista vacía
		llamar(get("/admin/usuarios/999999/reservas"), admin, null, 404);
		llamar(get("/admin/usuarios/999999/alojamientos"), admin, null, 404);

		llamar(get("/admin/reservas"), huesped.token(), null, 403);
	}

	@Test
	void elListadoDeUsuariosMuestraLaFechaDeRegistro() throws Exception {
		Sesion huesped = registrar("HUESPED");

		JsonNode usuario = llamar(get("/usuarios/obtener/" + huesped.id()), tokenAdmin(), null, 200);

		assertNotNull(usuario.get("fechaRegistro").asText());
		assertTrue(usuario.get("correoVerificado").asBoolean());
	}

	@Test
	void noSePuedeRestringirAUnAdministrador() throws Exception {
		String admin = tokenAdmin();
		Long idAdmin = llamar(get("/usuarios/perfil"), admin, null, 200).get("idUsuario").asLong();
		llamar(patch("/admin/usuarios/" + idAdmin + "/restringir"), admin, Map.of("motivo", "x"), 403);
	}

	@Test
	void elAdministradorConsultaLaAuditoriaYElDetalleDeUnEvento_CA062_CA064_CA137_CA138_CA139() throws Exception {
		Sesion huesped = registrar("HUESPED");
		String admin = tokenAdmin();

		JsonNode eventos = llamar(get("/admin/auditoria?accion=registro_usuario&idActor=" + huesped.id()), admin,
				null, 200);
		assertEquals(1, eventos.get("totalElementos").asInt());
		JsonNode evento = eventos.get("contenido").get(0);
		assertEquals(huesped.id(), evento.get("idUsuario").asLong());
		assertEquals("EXITOSO", evento.get("resultado").asText());
		assertNotNull(evento.get("fecha").asText());

		JsonNode detalle = llamar(get("/admin/auditoria/" + evento.get("idEvento").asLong()), admin, null, 200);
		assertEquals(AuditoriaService.REGISTRO_USUARIO, detalle.get("accion").asText());
		assertEquals("USUARIO", detalle.get("entidad").asText());

		String desde = LocalDateTime.now().plusDays(1).toString();
		assertEquals(0, llamar(get("/admin/auditoria?desde=" + desde), admin, null, 200).get("totalElementos").asInt());
	}

	@Test
	void unUsuarioSinPermisosNoPuedeConsultarLaAuditoria_CA065_CA134() throws Exception {
		llamar(get("/admin/auditoria"), registrar("ANFITRION").token(), null, 403);
		llamar(get("/admin/auditoria"), null, null, 401);
	}

	@Test
	void losRegistrosDeAuditoriaNoSePuedenModificarNiBorrar_CA150() throws Exception {
		String admin = tokenAdmin();
		Long idEvento = llamar(get("/admin/auditoria?tamanio=1"), admin, null, 200).get("contenido").get(0)
				.get("idEvento").asLong();

		llamar(put("/admin/auditoria/" + idEvento), admin, Map.of("accion", "OTRA"), 405);
		llamar(delete("/admin/auditoria/" + idEvento), admin, null, 405);
		assertEquals(idEvento, llamar(get("/admin/auditoria/" + idEvento), admin, null, 200).get("idEvento").asLong());
	}

	@Test
	void unaOperacionRechazadaQuedaRegistradaConElRecursoAfectado_CA151_CA152() throws Exception {
		Sesion propietario = registrar("ANFITRION");
		Sesion intruso = registrar("ANFITRION");
		Long id = crearBorrador(propietario, Map.of("titulo", "Mío"), 201).get("idAlojamiento").asLong();
		llamar(put("/alojamientos/" + id), intruso.token(), Map.of("titulo", "X"), 403);

		JsonNode eventos = llamar(get("/admin/auditoria?idActor=" + intruso.id()), tokenAdmin(), null, 200);
		String texto = eventos.get("contenido").toString();
		assertTrue(texto.contains("RECHAZADO") && texto.contains("ALOJAMIENTO") && texto.contains(id.toString()));
	}

	@Test
	void elAdministradorOcultaUnaResenaQueDejaDeMostrarse_CA066_CA067_CA069() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		Reserva reserva = new Reserva();
		reserva.setIdHuesped(huesped.id());
		reserva.setIdAlojamiento(id);
		reserva.setFechaEntrada(HOY.minusDays(3));
		reserva.setFechaSalida(HOY.minusDays(1));
		reserva.setTotal(new BigDecimal("200000"));
		reserva.setEstado(EstadoReserva.CONFIRMADA);
		reserva.setFechaCreacion(LocalDateTime.now().minusDays(5));
		Long idReserva = reservaRepo.save(reserva).getIdReserva();
		Long idResena = llamar(post("/resenas"), huesped.token(),
				Map.of("idReserva", idReserva, "calificacion", 1, "comentario", "Insultos"), 201).get("idResena")
				.asLong();

		llamar(patch("/admin/resenas/" + idResena + "/ocultar"), huesped.token(), Map.of("motivo", "x"), 403);
		llamar(patch("/admin/resenas/" + idResena + "/ocultar"), tokenAdmin(), Map.of("motivo", "Lenguaje ofensivo"),
				200);

		JsonNode resumen = llamar(get("/alojamientos/" + id + "/resenas"), null, null, 200);
		assertEquals(0, resumen.get("cantidad").asLong());
		llamar(patch("/admin/resenas/" + idResena + "/mostrar"), tokenAdmin(), null, 200);
		assertEquals(1, llamar(get("/alojamientos/" + id + "/resenas"), null, null, 200).get("cantidad").asLong());
	}

	@Test
	void laColaDeRevisionListaLasPublicacionesPendientes() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearBorrador(anfitrion,
				datosAlojamiento("Pasto", "Casa", 2, new BigDecimal("70000"), Set.of(), "FLEXIBLE"), 201)
				.get("idAlojamiento").asLong();
		ofrecerFechas(anfitrion, id);
		llamar(post("/alojamientos/" + id + "/enviarrevision"), anfitrion.token(), null, 200);

		JsonNode pendientes = llamar(get("/admin/alojamientos?estado=PENDIENTE_REVISION&tamanio=100"), tokenAdmin(),
				null, 200);

		assertTrue(pendientes.get("contenido").toString().contains("\"idAlojamiento\":" + id));
	}
}
