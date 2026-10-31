package co.edu.unbosque.marketplace.service;

import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Convierte la dirección y la ciudad de un alojamiento en coordenadas con
 * Nominatim (OpenStreetMap), el mismo proveedor del mapa del front. Primero
 * busca la dirección completa, luego solo lo que va antes de la primera coma
 * (sin barrio) y, si tampoco la encuentra o el resultado queda en otra ciudad,
 * usa el centro de la ciudad. Nunca lanza excepción: si Nominatim no
 * responde, el alojamiento se guarda sin coordenadas.
 * <p>
 * Política de uso de Nominatim: máximo una petición por segundo y un User-Agent
 * que identifique la aplicación.
 * </p>
 *
 * @see "RNF-032, CA-164"
 */
@Service
public class GeocodingService {

	private static final Logger LOG = LoggerFactory.getLogger(GeocodingService.class);
	private static final String PAIS = "Colombia";
	/** Resultados que se piden por búsqueda, para elegir el que está en la ciudad. */
	private static final int RESULTADOS = 3;
	private static final Duration INTERVALO = Duration.ofMillis(1100);

	/** Punto encontrado en el mapa. */
	public record Coordenadas(double latitud, double longitud) {
	}

	private final ClienteHttp clienteHttp;
	private final String url;
	private final String agente;
	private final boolean habilitado;
	private final long intervaloMs;
	private long ultimaPeticion;

	@Autowired
	public GeocodingService(@Value("${app.geocoding.url:https://nominatim.openstreetmap.org/search}") String url,
			@Value("${app.geocoding.agente:MarketplaceAlojamientos/1.0 (proyecto academico)}") String agente,
			@Value("${app.geocoding.habilitado:true}") boolean habilitado) {
		this(new ClienteHttp(Duration.ofSeconds(3), Duration.ofSeconds(5)), url, agente, habilitado, INTERVALO);
	}

	/** Constructor de las pruebas: otro cliente HTTP y otra espera entre peticiones. */
	GeocodingService(ClienteHttp clienteHttp, String url, String agente, boolean habilitado, Duration intervalo) {
		this.clienteHttp = clienteHttp;
		this.url = url;
		this.agente = agente;
		this.habilitado = habilitado;
		this.intervaloMs = intervalo.toMillis();
	}

	/**
	 * Busca la dirección dentro de la ciudad y, si no la encuentra, el centro de la
	 * ciudad.
	 *
	 * @return las coordenadas, o vacío si no hay datos, no se encontró nada o
	 *         Nominatim no respondió.
	 */
	public Optional<Coordenadas> ubicar(String direccion, String ciudad) {
		if (!habilitado || (vacio(direccion) && vacio(ciudad))) {
			return Optional.empty();
		}
		try {
			Optional<Coordenadas> encontrada = Optional.empty();
			if (!vacio(direccion)) {
				encontrada = buscar(conCiudad(direccion, ciudad), ciudad);
				// El barrio o la referencia después de la coma a veces impiden encontrar la calle
				int coma = direccion.indexOf(',');
				if (encontrada.isEmpty() && coma > 0) {
					encontrada = buscar(conCiudad(direccion.substring(0, coma), ciudad), ciudad);
				}
			}
			if (encontrada.isEmpty() && !vacio(ciudad)) {
				LOG.info("No se encontró la dirección '{}' en el mapa; se usa el centro de {}.", direccion, ciudad);
				encontrada = buscar(ciudad.trim(), null);
			}
			return encontrada;
		} catch (RestClientException e) {
			LOG.warn("Nominatim no respondió ({}); el alojamiento queda sin coordenadas.", e.getMessage());
			return Optional.empty();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	/**
	 * @param ciudadEsperada si no es null, solo sirve un resultado que esté en esa
	 *                       ciudad (hay calles con el mismo nombre en muchas).
	 */
	private Optional<Coordenadas> buscar(String consulta, String ciudadEsperada) throws InterruptedException {
		// La variable {q} se codifica completa: el "#" de las direcciones no se toma como fragmento
		URI uri = UriComponentsBuilder.fromUriString(url).queryParam("q", "{q}").queryParam("format", "jsonv2")
				.queryParam("limit", RESULTADOS).queryParam("countrycodes", "co").encode()
				.buildAndExpand(consulta + ", " + PAIS).toUri();
		esperarTurno();
		JsonNode resultados = clienteHttp.getJson(uri,
				Map.of(HttpHeaders.USER_AGENT, agente, HttpHeaders.ACCEPT_LANGUAGE, "es"));
		if (resultados == null) {
			return Optional.empty();
		}
		for (JsonNode resultado : resultados) {
			if (vacio(ciudadEsperada)
					|| normalizar(resultado.path("display_name").asText()).contains(normalizar(ciudadEsperada))) {
				return coordenadas(resultado);
			}
		}
		return Optional.empty();
	}

	private static Optional<Coordenadas> coordenadas(JsonNode resultado) {
		try {
			double latitud = Double.parseDouble(resultado.path("lat").asText());
			double longitud = Double.parseDouble(resultado.path("lon").asText());
			if (Math.abs(latitud) > 90 || Math.abs(longitud) > 180) {
				return Optional.empty();
			}
			return Optional.of(new Coordenadas(latitud, longitud));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	/** Respeta el máximo de una petición por segundo de Nominatim. */
	private synchronized void esperarTurno() throws InterruptedException {
		long espera = ultimaPeticion + intervaloMs - System.currentTimeMillis();
		if (espera > 0) {
			Thread.sleep(espera);
		}
		ultimaPeticion = System.currentTimeMillis();
	}

	private static String conCiudad(String direccion, String ciudad) {
		return vacio(ciudad) ? direccion.trim() : direccion.trim() + ", " + ciudad.trim();
	}

	private static boolean vacio(String texto) {
		return texto == null || texto.isBlank();
	}

	private static String normalizar(String texto) {
		return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
				.trim();
	}
}
