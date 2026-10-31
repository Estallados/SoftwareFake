package co.edu.unbosque.marketplace.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.service.GeocodingService.Coordenadas;

/**
 * Búsqueda de coordenadas. Un servidor falso responde como Nominatim, con el
 * mismo formato de sus resultados.
 */
class GeocodingServiceTest {

	private static final String URL = "https://nominatim.prueba/search";
	private static final String AGENTE = "MarketplacePruebas/1.0";
	/** Lo que llega en q= para "Calle 70A # 4-41, Bogotá, Colombia": el # va codificado. */
	private static final String DIRECCION_CODIFICADA = "q=Calle%2070A%20%23%204-41%2C%20Bogot%C3%A1%2C%20Colombia";

	private MockRestServiceServer nominatim;
	private GeocodingService geocoding;

	@BeforeEach
	void preparar() {
		geocoding = crear(true);
	}

	private GeocodingService crear(boolean habilitado) {
		RestClient.Builder builder = RestClient.builder();
		nominatim = MockRestServiceServer.bindTo(builder).build();
		return new GeocodingService(new ClienteHttp(builder.build()), URL, AGENTE, habilitado, Duration.ZERO);
	}

	private static String resultado(String lat, String lon, String nombre) {
		return "{\"lat\":\"" + lat + "\",\"lon\":\"" + lon + "\",\"display_name\":\"" + nombre + "\"}";
	}

	private void responder(String consulta, String... resultados) {
		nominatim.expect(requestTo(containsString(consulta))).andExpect(header("User-Agent", AGENTE))
				.andRespond(withSuccess("[" + String.join(",", resultados) + "]", MediaType.APPLICATION_JSON));
	}

	@Test
	void ubicaLaDireccionCompletaDentroDeLaCiudad() {
		responder(DIRECCION_CODIFICADA,
				resultado("4.6546184", "-74.0579372", "Calle 70A, Localidad Chapinero, Bogotá, Colombia"));

		Optional<Coordenadas> coordenadas = geocoding.ubicar(" Calle 70A # 4-41 ", "Bogotá");

		nominatim.verify();
		assertEquals(new Coordenadas(4.6546184, -74.0579372), coordenadas.orElseThrow());
	}

	@Test
	void descartaLosResultadosDeOtraCiudadYUsaElSiguiente() {
		responder(DIRECCION_CODIFICADA, resultado("10.45", "-73.23", "Calle 70A, Valledupar, Cesar, Colombia"),
				resultado("4.65", "-74.05", "Calle 70A, Chapinero, Bogota, Colombia"));

		assertEquals(new Coordenadas(4.65, -74.05), geocoding.ubicar("Calle 70A # 4-41", "Bogotá").orElseThrow());
	}

	@Test
	void siElBarrioEstorbaBuscaSoloLaCalle() {
		responder("q=Calle%2070A%20%23%204-41%2C%20Chapinero%20Alto%2C%20Bogot%C3%A1%2C%20Colombia");
		responder(DIRECCION_CODIFICADA, resultado("4.6546", "-74.0579", "Calle 70A, Chapinero, Bogotá, Colombia"));

		Optional<Coordenadas> coordenadas = geocoding.ubicar("Calle 70A # 4-41, Chapinero Alto", "Bogotá");

		nominatim.verify();
		assertEquals(new Coordenadas(4.6546, -74.0579), coordenadas.orElseThrow());
	}

	@Test
	void siNoEncuentraLaDireccionUsaElCentroDeLaCiudad() {
		responder("q=Vereda%20La%20Piedra%2C%20Guatap%C3%A9%2C%20Colombia");
		responder("q=Guatap%C3%A9%2C%20Colombia",
				resultado("6.2342179", "-75.1617452", "Guatapé, Oriente, Antioquia, Colombia"));

		Optional<Coordenadas> coordenadas = geocoding.ubicar("Vereda La Piedra", "Guatapé");

		nominatim.verify();
		assertEquals(new Coordenadas(6.2342179, -75.1617452), coordenadas.orElseThrow());
	}

	@Test
	void siNominatimFallaDevuelveVacioSinLanzarError() {
		nominatim.expect(requestTo(containsString(DIRECCION_CODIFICADA))).andRespond(withServerError());

		assertTrue(geocoding.ubicar("Calle 70A # 4-41", "Bogotá").isEmpty());
		nominatim.verify();
	}

	@Test
	void sinDatosODeshabilitadoNoConsultaNominatim() {
		nominatim.expect(never(), requestTo(containsString(URL)));
		assertTrue(geocoding.ubicar(null, " ").isEmpty());
		nominatim.verify();

		GeocodingService apagado = crear(false);
		nominatim.expect(never(), requestTo(containsString(URL)));
		assertTrue(apagado.ubicar("Calle 70A # 4-41", "Bogotá").isEmpty());
		nominatim.verify();
	}
}
