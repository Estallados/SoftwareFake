package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Usa el servidor real (no MockMvc) porque el reenvío de errores a /error solo
 * ocurre dentro de Tomcat.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ErrorResponseTest {

	@Autowired
	private TestRestTemplate rest;

	@Test
	void jsonMalFormadoEnLoginResponde400YNo401() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		// Falta la llave de apertura, como en el curl del reporte
		HttpEntity<String> peticion = new HttpEntity<>(
				"\"correo\": \"ana@correo.com\", \"contrasenia\": \"Segura#2026\"}", headers);

		ResponseEntity<String> respuesta = rest.postForEntity("/auth/login", peticion, String.class);

		assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
	}
}
