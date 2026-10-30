package co.edu.unbosque.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.exception.AlmacenamientoNoDisponibleException;

/** Configuración de Cloudinary sin conectarse a la API. */
class CloudinaryServiceTest {

	@Test
	void sinCloudinaryUrlLaAppArrancaPeroSubirResponde503() {
		CloudinaryService servicio = new CloudinaryService("");
		byte[] datos = { 1 };

		assertFalse(servicio.estaConfigurado());
		AlmacenamientoNoDisponibleException error = assertThrows(AlmacenamientoNoDisponibleException.class,
				() -> servicio.subir(datos, "marketplace/alojamientos/1", "foto"));
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
}
