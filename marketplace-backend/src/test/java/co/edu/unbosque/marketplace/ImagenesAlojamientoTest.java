package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.exception.AlmacenamientoNoDisponibleException;
import co.edu.unbosque.marketplace.service.CloudinaryService.ImagenSubida;

/**
 * Imágenes de alojamientos con selector de archivos (multipart), obligatorias desde la creación. Cloudinary
 * está simulado (ver {@link PruebaIntegracionBase}).
 */
class ImagenesAlojamientoTest extends PruebaIntegracionBase {

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I',
			'F' };

	private JsonNode subir(Sesion actor, Long idAlojamiento, int estado, MockMultipartFile... archivos)
			throws Exception {
		MockMultipartHttpServletRequestBuilder peticion = multipart("/alojamientos/" + idAlojamiento + "/imagenes");
		for (MockMultipartFile archivo : archivos) {
			peticion.file(archivo);
		}
		return llamar(peticion, actor.token(), null, estado);
	}

	private Map<String, Object> datos() {
		return datosAlojamiento("Cali", "Casa", 4, new BigDecimal("90000"), Set.of("wifi"), "FLEXIBLE");
	}

	/** Borrador con las imágenes indicadas; devuelve su id. */
	private Long borrador(Sesion anfitrion, MockMultipartFile... archivos) throws Exception {
		return crearBorrador(anfitrion, datos(), 201, archivos).get("idAlojamiento").asLong();
	}

	private int cantidadImagenes(Sesion anfitrion, Long id) throws Exception {
		return llamar(get("/alojamientos/" + id), anfitrion.token(), null, 200).get("imagenes").size();
	}

	/** public_id de una imagen subida, sacado de la URL del Cloudinary simulado. */
	private static String publicId(JsonNode imagen) {
		return imagen.get("url").asText().substring(URL_CLOUDINARY.length());
	}

	@Test
	void alCrearLaPrimeraImagenQuedaComoPrincipalYSeGuardaEnCloudinary_CA094() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");

		JsonNode creado = crearBorrador(anfitrion, datos(), 201, imagen("sala.png", PNG),
				imagen("cuarto.jpg", JPEG));

		Long id = creado.get("idAlojamiento").asLong();
		JsonNode imagenes = creado.get("detalleImagenes");
		assertEquals("BORRADOR", creado.get("estado").asText());
		assertEquals(2, imagenes.size());
		assertTrue(imagenes.get(0).get("principal").asBoolean());
		assertFalse(imagenes.get(1).get("principal").asBoolean());
		String url = imagenes.get(0).get("url").asText();
		assertTrue(url.startsWith(URL_CLOUDINARY + "marketplace/alojamientos/" + id + "/"));
		assertEquals(url, creado.get("imagenes").get(0).asText());
		verify(cloudinaryService).subir(eq(PNG), eq("marketplace/alojamientos/" + id), anyString());
		verify(cloudinaryService).subir(eq(JPEG), eq("marketplace/alojamientos/" + id), anyString());

		// Las siguientes se agregan sin cambiar la portada
		JsonNode todas = subir(anfitrion, id, 201, imagen("bano.png", PNG));
		assertEquals(3, todas.size());
		assertEquals(url, todas.get(0).get("url").asText());
	}

	@Test
	void noSePuedeCrearUnBorradorSinImagen_CA096() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");

		JsonNode error = crearBorrador(anfitrion, datos(), 400, new MockMultipartFile[0]);

		assertTrue(error.get("message").asText().contains("al menos una imagen"));
		assertEquals(0, llamar(get("/alojamientos/mios"), anfitrion.token(), null, 200).get("totalElementos")
				.asInt());
	}

	@Test
	void siUnaImagenDelBorradorNoSirveNoSeCreaElAlojamiento() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");

		JsonNode error = crearBorrador(anfitrion, datos(), 400, imagen("buena.png", PNG),
				imagen("falsa.png", "<script>".getBytes(StandardCharsets.UTF_8)));

		assertTrue(error.get("message").asText().contains("falsa.png"));
		assertEquals(0, llamar(get("/alojamientos/mios"), anfitrion.token(), null, 200).get("totalElementos")
				.asInt());
	}

	@Test
	void losDatosPuedenLlegarComoTextoSinTipoYUnJsonInvalidoSeRechaza() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		byte[] texto = json.writeValueAsBytes(Map.of("titulo", "Desde Swagger"));

		// Así envían el campo Swagger UI y Postman: una parte de texto sin Content-Type
		JsonNode creado = llamar(multipart("/alojamientos").file(imagen("a.png", PNG)).part(new MockPart("datos", texto)),
				anfitrion.token(), null, 201);
		assertEquals("Desde Swagger", creado.get("titulo").asText());

		JsonNode error = llamar(multipart("/alojamientos").file(imagen("a.png", PNG))
				.part(new MockPart("datos", "{titulo".getBytes(StandardCharsets.UTF_8))), anfitrion.token(), null, 400);
		assertTrue(error.get("message").asText().contains("'datos'"));
		llamar(multipart("/alojamientos").file(imagen("a.png", PNG)), anfitrion.token(), null, 400);
	}

	@Test
	void crearComoJsonResponde415() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		JsonNode error = llamar(post("/alojamientos"), anfitrion.token(), datos(), 415);
		assertTrue(error.get("message").asText().contains("multipart/form-data"));
	}

	@Test
	void rechazaArchivosQueNoSonImagenesAunqueDiganSerlo() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borrador(anfitrion, imagen("sala.png", PNG));

		JsonNode error = subir(anfitrion, id, 400, imagen("virus.png", "<script>alert(1)</script>".getBytes()));
		assertTrue(error.get("message").asText().contains("JPG, PNG o WEBP"));
		subir(anfitrion, id, 400, imagen("vacia.png", new byte[0]));

		// Si un archivo del lote no sirve no se guarda ninguno
		subir(anfitrion, id, 400, imagen("buena.png", PNG), imagen("mala.png", new byte[] { 1, 2, 3 }));
		assertEquals(1, cantidadImagenes(anfitrion, id));
	}

	@Test
	void noSePuedeSuperarElMaximoDeImagenes() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		MockMultipartFile[] muchas = new MockMultipartFile[16];
		Arrays.fill(muchas, imagen("foto.png", PNG));

		assertTrue(crearBorrador(anfitrion, datos(), 400, muchas).get("message").asText().contains("15"));

		Long id = borrador(anfitrion, imagen("sala.png", PNG));
		JsonNode error = subir(anfitrion, id, 400, Arrays.copyOf(muchas, 15));
		assertTrue(error.get("message").asText().contains("15"));
		subir(anfitrion, id, 201, Arrays.copyOf(muchas, 14));
	}

	@Test
	void otroAnfitrionNoPuedeSubirNiBorrarImagenes() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Sesion otro = registrar("ANFITRION");
		Long id = borrador(anfitrion, imagen("sala.png", PNG), imagen("cuarto.png", PNG));
		Long idImagen = subir(anfitrion, id, 201, imagen("cocina.png", PNG)).get(0).get("idImagen").asLong();

		subir(otro, id, 403, imagen("intruso.png", PNG));
		llamar(delete("/alojamientos/" + id + "/imagenes/" + idImagen), otro.token(), null, 403);
		llamar(patch("/alojamientos/" + id + "/imagenes/" + idImagen + "/principal"), otro.token(), null, 403);
		subir(registrar("HUESPED"), id, 403, imagen("huesped.png", PNG));
	}

	@Test
	void cambiarLaPrincipalYBorrarLaPortadaPasaLaSiguienteAPrincipal() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		JsonNode creado = crearBorrador(anfitrion, datos(), 201, imagen("a.png", PNG), imagen("b.png", PNG),
				imagen("c.png", PNG));
		Long id = creado.get("idAlojamiento").asLong();
		JsonNode creadas = creado.get("detalleImagenes");
		Long segunda = creadas.get(1).get("idImagen").asLong();
		Long tercera = creadas.get(2).get("idImagen").asLong();

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
	void todoAlojamientoDebeConservarAlMenosUnaImagenTambienEnBorrador() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long borrador = borrador(anfitrion, imagen("unica.png", PNG));
		Long unicaDelBorrador = llamar(get("/alojamientos/" + borrador), anfitrion.token(), null, 200)
				.get("detalleImagenes").get(0).get("idImagen").asLong();
		JsonNode error = llamar(delete("/alojamientos/" + borrador + "/imagenes/" + unicaDelBorrador),
				anfitrion.token(), null, 409);
		assertTrue(error.get("message").asText().contains("al menos una imagen"));

		Long id = crearAlojamientoActivo(anfitrion);
		Long unica = llamar(get("/alojamientos/" + id), null, null, 200).get("detalleImagenes").get(0)
				.get("idImagen").asLong();
		llamar(delete("/alojamientos/" + id + "/imagenes/" + unica), anfitrion.token(), null, 409);

		Long nueva = subir(anfitrion, id, 201, imagen("nueva.png", PNG)).get(1).get("idImagen").asLong();
		llamar(delete("/alojamientos/" + id + "/imagenes/" + unica), anfitrion.token(), null, 200);
		JsonNode detalle = llamar(get("/alojamientos/" + id), null, null, 200);
		assertEquals(nueva, detalle.get("detalleImagenes").get(0).get("idImagen").asLong());
		assertTrue(detalle.get("detalleImagenes").get(0).get("principal").asBoolean());
	}

	@Test
	void editarConUnaListaDeImagenesNoLasCambia() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borrador(anfitrion, imagen("sala.png", PNG));

		JsonNode editado = actualizarAlojamiento(anfitrion, id,
				Map.of("titulo", "Nuevo título", "imagenes", List.of()), 200);

		assertEquals("Nuevo título", editado.get("titulo").asText());
		assertEquals(1, editado.get("imagenes").size());
	}

	@Test
	void sinArchivosResponde400YComoJsonResponde415() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borrador(anfitrion, imagen("sala.png", PNG));

		llamar(multipart("/alojamientos/" + id + "/imagenes"), anfitrion.token(), null, 400);
		llamar(post("/alojamientos/" + id + "/imagenes"), anfitrion.token(), Map.of("archivos", "x"), 415);
	}

	@Test
	void unaImagenBorradaSeEliminaDeCloudinary() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		JsonNode imagenes = crearBorrador(anfitrion, datos(), 201, imagen("sala.png", PNG),
				imagen("cuarto.jpg", JPEG)).get("detalleImagenes");
		Long id = imagenes.get(0).get("idAlojamiento").asLong();

		llamar(delete("/alojamientos/" + id + "/imagenes/" + imagenes.get(1).get("idImagen").asLong()),
				anfitrion.token(), null, 200);

		verify(cloudinaryService).borrar(publicId(imagenes.get(1)));
		verify(cloudinaryService, never()).borrar(publicId(imagenes.get(0)));
	}

	@Test
	void siCloudinaryFallaNoSeCreaElAlojamientoYSeBorraLoQueAlcanzoASubir() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		ImagenSubida primera = new ImagenSubida("marketplace/alojamientos/x/primera", URL_CLOUDINARY + "primera");
		when(cloudinaryService.subir(any(), anyString(), anyString())).thenReturn(primera)
				.thenThrow(new AlmacenamientoNoDisponibleException("No se pudo guardar la imagen. Intenta de nuevo."));

		JsonNode error = crearBorrador(anfitrion, datos(), 503, imagen("sala.png", PNG), imagen("cuarto.jpg", JPEG));

		assertTrue(error.get("message").asText().contains("No se pudo guardar la imagen"));
		assertEquals(0, llamar(get("/alojamientos/mios"), anfitrion.token(), null, 200).get("totalElementos")
				.asInt());
		verify(cloudinaryService).borrar(primera.publicId());
	}

	@Test
	void siSeRechazaElLoteNoSeSubeNadaACloudinary() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = borrador(anfitrion, imagen("sala.png", PNG));
		clearInvocations(cloudinaryService);

		subir(anfitrion, id, 400, imagen("buena.png", PNG), imagen("mala.png", new byte[] { 1, 2, 3 }));

		verify(cloudinaryService, never()).subir(any(), anyString(), anyString());
	}
}
