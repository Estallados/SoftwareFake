package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/** Subida de imágenes de alojamientos con selector de archivos (multipart). */
class ImagenesAlojamientoTest extends PruebaIntegracionBase {

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 1, 2, 3 };
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I',
			'F' };

	private static MockMultipartFile archivo(String nombre, byte[] contenido) {
		return new MockMultipartFile("archivos", nombre, "image/png", contenido);
	}

	private JsonNode subir(Sesion actor, Long idAlojamiento, int estado, MockMultipartFile... archivos)
			throws Exception {
		MockMultipartHttpServletRequestBuilder peticion = multipart("/alojamientos/" + idAlojamiento + "/imagenes");
		for (MockMultipartFile archivo : archivos) {
			peticion.file(archivo);
		}
		return llamar(peticion, actor.token(), null, estado);
	}

	/** Borrador sin imágenes. */
	private Long borradorSinImagenes(Sesion anfitrion) throws Exception {
		Map<String, Object> datos = new HashMap<>(datosAlojamiento("Cali", "Casa", 4,
				new BigDecimal("90000"), Set.of("wifi"), "FLEXIBLE"));
		datos.remove("imagenes");
		return llamar(post("/alojamientos"), anfitrion.token(), datos, 201).get("idAlojamiento").asLong();
	}

	@Test
	void elAnfitrionSubeImagenesLaPrimeraQuedaComoPrincipalYSeSirvenPublicamente() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borradorSinImagenes(anfitrion);

		JsonNode imagenes = subir(anfitrion, id, 201, archivo("sala.png", PNG), archivo("cuarto.jpg", JPEG));

		assertEquals(2, imagenes.size());
		assertTrue(imagenes.get(0).get("principal").asBoolean());
		assertFalse(imagenes.get(1).get("principal").asBoolean());
		String url = imagenes.get(0).get("url").asText();
		assertTrue(url.startsWith("/archivos/alojamientos/" + id + "/") && url.endsWith(".png"));
		assertTrue(imagenes.get(1).get("url").asText().endsWith(".jpg"));

		byte[] servida = mvc.perform(get(url)).andReturn().getResponse().getContentAsByteArray();
		assertArrayEquals(PNG, servida);

		JsonNode detalle = llamar(get("/alojamientos/" + id), anfitrion.token(), null, 200);
		assertEquals(url, detalle.get("imagenes").get(0).asText());
		assertEquals(2, detalle.get("detalleImagenes").size());
	}

	@Test
	void rechazaArchivosQueNoSonImagenesAunqueDiganSerlo() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borradorSinImagenes(anfitrion);

		JsonNode error = subir(anfitrion, id, 400, archivo("virus.png", "<script>alert(1)</script>".getBytes()));
		assertTrue(error.get("message").asText().contains("JPG, PNG o WEBP"));
		subir(anfitrion, id, 400, archivo("vacia.png", new byte[0]));

		// Si un archivo del lote no sirve no se guarda ninguno
		subir(anfitrion, id, 400, archivo("buena.png", PNG), archivo("mala.png", new byte[] { 1, 2, 3 }));
		assertEquals(0, llamar(get("/alojamientos/" + id), anfitrion.token(), null, 200).get("imagenes").size());
	}

	@Test
	void noSePuedeSuperarElMaximoDeImagenes() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borradorSinImagenes(anfitrion);
		MockMultipartFile[] muchas = new MockMultipartFile[16];
		Arrays.fill(muchas, archivo("foto.png", PNG));

		JsonNode error = subir(anfitrion, id, 400, muchas);

		assertTrue(error.get("message").asText().contains("15"));
	}

	@Test
	void otroAnfitrionNoPuedeSubirNiBorrarImagenes() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Sesion otro = registrar("ANFITRION");
		Long id = borradorSinImagenes(anfitrion);
		Long idImagen = subir(anfitrion, id, 201, archivo("sala.png", PNG)).get(0).get("idImagen").asLong();

		subir(otro, id, 403, archivo("intruso.png", PNG));
		llamar(delete("/alojamientos/" + id + "/imagenes/" + idImagen), otro.token(), null, 403);
		subir(registrar("HUESPED"), id, 403, archivo("huesped.png", PNG));
	}

	@Test
	void cambiarLaPrincipalYBorrarLaPortadaPasaLaSiguienteAPrincipal() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borradorSinImagenes(anfitrion);
		JsonNode subidas = subir(anfitrion, id, 201, archivo("a.png", PNG), archivo("b.png", PNG),
				archivo("c.png", PNG));
		Long segunda = subidas.get(1).get("idImagen").asLong();
		Long tercera = subidas.get(2).get("idImagen").asLong();

		JsonNode imagenes = llamar(patch("/alojamientos/" + id + "/imagenes/" + tercera + "/principal"),
				anfitrion.token(), null, 200);
		assertEquals(tercera, imagenes.get(0).get("idImagen").asLong());
		assertTrue(imagenes.get(0).get("principal").asBoolean());

		imagenes = llamar(delete("/alojamientos/" + id + "/imagenes/" + tercera), anfitrion.token(), null, 200);
		assertEquals(2, imagenes.size());
		assertTrue(imagenes.get(0).get("principal").asBoolean());
		llamar(delete("/alojamientos/" + id + "/imagenes/" + segunda), anfitrion.token(), null, 200);
		llamar(delete("/alojamientos/" + id + "/imagenes/999999"), anfitrion.token(), null, 404);
	}

	@Test
	void unaPublicacionYaEnviadaDebeConservarAlMenosUnaImagen() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Long unica = llamar(get("/alojamientos/" + id), null, null, 200).get("detalleImagenes").get(0)
				.get("idImagen").asLong();

		llamar(delete("/alojamientos/" + id + "/imagenes/" + unica), anfitrion.token(), null, 409);

		Long nueva = subir(anfitrion, id, 201, archivo("nueva.png", PNG)).get(1).get("idImagen").asLong();
		llamar(delete("/alojamientos/" + id + "/imagenes/" + unica), anfitrion.token(), null, 200);
		JsonNode detalle = llamar(get("/alojamientos/" + id), null, null, 200);
		assertEquals(nueva, detalle.get("detalleImagenes").get(0).get("idImagen").asLong());
		assertTrue(detalle.get("detalleImagenes").get(0).get("principal").asBoolean());
	}

	@Test
	void sinArchivosResponde400YComoJsonResponde415() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borradorSinImagenes(anfitrion);

		llamar(multipart("/alojamientos/" + id + "/imagenes"), anfitrion.token(), null, 400);
		llamar(post("/alojamientos/" + id + "/imagenes"), anfitrion.token(), Map.of("archivos", "x"), 415);
	}
}
