package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.PeriodoDisponibilidad;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;

/** Catálogo público: todos los alojamientos que un visitante puede reservar. */
class CatalogoTest extends PruebaIntegracionBase {

	@Autowired
	private PeriodoDisponibilidadRepository periodoRepo;

	@Autowired
	private ReservaRepository reservaRepo;

	/** Ids del catálogo en el orden en que llegan (página grande para ver los de esta prueba). */
	private List<Long> catalogo(String orden) throws Exception {
		JsonNode pagina = llamar(get("/alojamientos/disponibles?tamanio=100" + (orden == null ? "" : "&orden=" + orden)),
				null, null, 200);
		List<Long> ids = new ArrayList<>();
		pagina.get("contenido").forEach(a -> ids.add(a.get("idAlojamiento").asLong()));
		return ids;
	}

	private Long activoConPrecio(Sesion anfitrion, int precio) throws Exception {
		return crearAlojamientoActivo(anfitrion, "Bogotá", "Casa", 4, new BigDecimal(precio), Set.of(), "FLEXIBLE");
	}

	/** Estancia ya terminada y reseñada con la calificación indicada. */
	private void resena(Long idAlojamiento, int calificacion) throws Exception {
		Sesion huesped = registrar("HUESPED");
		Reserva reserva = new Reserva();
		reserva.setIdHuesped(huesped.id());
		reserva.setIdAlojamiento(idAlojamiento);
		reserva.setFechaEntrada(HOY.minusDays(5));
		reserva.setFechaSalida(HOY.minusDays(2));
		reserva.setTotal(new BigDecimal("300000"));
		reserva.setEstado(EstadoReserva.CONFIRMADA);
		reserva.setFechaCreacion(LocalDateTime.now().minusDays(10));
		Long idReserva = reservaRepo.save(reserva).getIdReserva();
		llamar(post("/resenas"), huesped.token(),
				Map.of("idReserva", idReserva, "calificacion", calificacion, "comentario", "Buena estancia"), 201);
	}

	@Test
	void unVisitanteSinSesionVeSoloLosQueSePuedenReservar_CA087() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long disponible = crearAlojamientoActivo(anfitrion);
		Long sinFechas = crearAlojamientoActivo(anfitrion);
		Long pausado = crearAlojamientoActivo(anfitrion);
		Long borrador = crearBorrador(anfitrion, Map.of("titulo", "Sin publicar"), 201).get("idAlojamiento").asLong();
		llamar(post("/alojamientos/" + pausado + "/pausar"), anfitrion.token(), null, 200);
		// Todos sus períodos ya vencieron: está activo pero no se puede reservar
		for (PeriodoDisponibilidad periodo : periodoRepo.findByIdAlojamientoOrderByFechaInicio(sinFechas)) {
			periodo.setFechaInicio(HOY.minusDays(30));
			periodo.setFechaFin(HOY);
			periodoRepo.save(periodo);
		}

		JsonNode pagina = llamar(get("/alojamientos/disponibles?tamanio=100"), null, null, 200);

		List<Long> ids = catalogo(null);
		assertTrue(ids.contains(disponible));
		assertFalse(ids.contains(sinFechas));
		assertFalse(ids.contains(pausado));
		assertFalse(ids.contains(borrador));
		assertTrue(pagina.get("totalElementos").asInt() >= 1);
		JsonNode tarjeta = pagina.get("contenido").get(0);
		assertTrue(tarjeta.has("imagenes") && tarjeta.has("promedioCalificacion") && tarjeta.has("nombreAnfitrion"));
	}

	@Test
	void ordenaPorPrecioDeMenorAMayorYAlReves() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long medio = activoConPrecio(anfitrion, 2);
		Long barato = activoConPrecio(anfitrion, 1);
		Long caro = activoConPrecio(anfitrion, 3);

		List<Long> menor = catalogo("precio_menor");
		assertEquals(List.of(barato, medio, caro), menor.subList(0, 3));

		Long carisimo = activoConPrecio(anfitrion, 900_000_000);
		assertEquals(carisimo, catalogo("precio_mayor").get(0));
		JsonNode busqueda = llamar(get("/alojamientos?ciudad=Bogotá&orden=precio_menor&tamanio=3"), null, null, 200);
		assertEquals(barato, busqueda.get("contenido").get(0).get("idAlojamiento").asLong());
	}

	@Test
	void recomendadosPrefiereMasResenasYMejorCalificadosElMejorPromedio() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long unaPerfecta = crearAlojamientoActivo(anfitrion);
		Long variasBuenas = crearAlojamientoActivo(anfitrion);
		resena(unaPerfecta, 5);
		resena(variasBuenas, 4);
		resena(variasBuenas, 4);
		resena(variasBuenas, 5);

		List<Long> mejorCalificados = catalogo("mejor_calificados");
		assertTrue(mejorCalificados.indexOf(unaPerfecta) < mejorCalificados.indexOf(variasBuenas));
		List<Long> recomendados = catalogo(null);
		assertTrue(recomendados.indexOf(variasBuenas) < recomendados.indexOf(unaPerfecta));
		assertEquals(recomendados, catalogo("recomendados"));
	}

	@Test
	void unOrdenQueNoExisteResponde400() throws Exception {
		JsonNode error = llamar(get("/alojamientos/disponibles?orden=alfabetico"), null, null, 400);
		assertTrue(error.get("message").asText().contains("precio_menor"));
		llamar(get("/alojamientos?orden=alfabetico"), null, null, 400);
	}
}
