package co.edu.unbosque.marketplace.service;

import java.time.Duration;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cliente HTTP común para las APIs externas (Stripe y las claves públicas de
 * Google). Todas las peticiones tienen tiempo máximo de conexión y de lectura,
 * así una API lenta no deja colgada la petición del usuario.
 */
@Component
public class ClienteHttp {

	private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(5);
	private static final Duration TIEMPO_LECTURA = Duration.ofSeconds(15);

	private final RestClient restClient;

	@Autowired
	public ClienteHttp() {
		this(RestClient.builder().requestFactory(fabricaConTiempos()).build());
	}

	/**
	 * Constructor con otro cliente; las pruebas lo usan para simular las respuestas
	 * de las APIs.
	 */
	public ClienteHttp(RestClient restClient) {
		this.restClient = restClient;
	}

	private static SimpleClientHttpRequestFactory fabricaConTiempos() {
		SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(TIEMPO_CONEXION);
		fabrica.setReadTimeout(TIEMPO_LECTURA);
		return fabrica;
	}

	/**
	 * GET que espera JSON.
	 *
	 * @return la respuesta con sus encabezados.
	 * @throws org.springframework.web.client.RestClientException si no hay
	 *                                                            comunicación o la
	 *                                                            API responde con
	 *                                                            un error.
	 */
	public ResponseEntity<JsonNode> getJson(String url) {
		return restClient.get().uri(url).retrieve().toEntity(JsonNode.class);
	}

	/**
	 * POST con un formulario (application/x-www-form-urlencoded). Las respuestas de
	 * error no lanzan excepción: el intérprete recibe el estado HTTP y el cuerpo
	 * JSON para decidir.
	 *
	 * @throws org.springframework.web.client.RestClientException si no hay
	 *                                                            comunicación.
	 */
	public <T> T postFormulario(String url, Map<String, String> encabezados, MultiValueMap<String, String> formulario,
			BiFunction<HttpStatusCode, JsonNode, T> interprete) {
		return restClient.post().uri(url).headers(h -> encabezados.forEach(h::set))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formulario).exchange((peticion,
						respuesta) -> interprete.apply(respuesta.getStatusCode(), respuesta.bodyTo(JsonNode.class)));
	}
}
