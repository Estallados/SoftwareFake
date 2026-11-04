package co.edu.unbosque.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import co.edu.unbosque.marketplace.exception.AlmacenamientoNoDisponibleException;
import co.edu.unbosque.marketplace.service.CloudinaryService.ImagenGuardada;
import co.edu.unbosque.marketplace.service.CloudinaryService.MetadatosImagen;

/** Configuración de Cloudinary sin conectarse a la API. */
class CloudinaryServiceTest {

	@Test
	void sinCloudinaryUrlLaAppArrancaPeroSubirResponde503() {
		CloudinaryService servicio = new CloudinaryService("");
		byte[] datos = { 1 };

		assertFalse(servicio.estaConfigurado());
		AlmacenamientoNoDisponibleException error = assertThrows(AlmacenamientoNoDisponibleException.class,
				() -> servicio.subir(datos, "marketplace/alojamientos/1", "foto", new MetadatosImagen("m", 1L, true, 1)));
		assertEquals("El almacenamiento de imágenes no está configurado.", error.getMessage());
		assertDoesNotThrow(() -> servicio.borrar("marketplace/alojamientos/1/foto"));
	}

	@Test
	void conCloudinaryUrlQuedaConfigurado() {
		assertTrue(new CloudinaryService("cloudinary://123:secreto@nube").estaConfigurado());
	}

	@Test
	void unaCloudinaryUrlMalEscritaDetieneElArranque() {
		IllegalStateException error = assertThrows(IllegalStateException.class,
				() -> new CloudinaryService("https://nube"));
		assertTrue(error.getMessage().contains("cloudinary://"));
	}

	@Test
	void sinCloudinaryUrlListarDevuelveVacio() {
		assertTrue(new CloudinaryService("").listar("marketplace-alojamientos").isEmpty());
	}

	@Test
	void leeLosDatosDeCadaImagenEIgnoraLasQueNoLosTienen() {
		Map<String, Object> valida = Map.of("public_id", "a", "secure_url", "https://c/image/upload/v1/a.png", "context",
				Map.of("custom", Map.of("idAlojamiento", "7", "principal", "true", "orden", "42")));
		Map<String, Object> sinDatos = Map.of("public_id", "b", "secure_url", "https://c/image/upload/v1/b.png");

		List<ImagenGuardada> imagenes = CloudinaryService.leerRecursos(Map.of("resources", List.of(valida, sinDatos)));

		assertEquals(List.of(new ImagenGuardada("a", "https://c/image/upload/" + CloudinaryService.ENTREGA + "/v1/a.png",
				7L, true, 42)), imagenes);
	}
}
