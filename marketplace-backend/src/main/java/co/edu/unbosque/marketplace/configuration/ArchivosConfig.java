package co.edu.unbosque.marketplace.configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import co.edu.unbosque.marketplace.service.AlmacenamientoService;

/**
 * Sirve los archivos subidos (imágenes de alojamientos) desde la carpeta de
 * {@link AlmacenamientoService} en la ruta pública {@code /archivos/**}. La
 * lectura es pública (las imágenes se muestran en la búsqueda); subir y borrar
 * pasa por {@code AlojamientoController} con token.
 *
 * @see "RF-017"
 */
@Configuration
public class ArchivosConfig implements WebMvcConfigurer {

	private final AlmacenamientoService almacenamientoService;

	public ArchivosConfig(AlmacenamientoService almacenamientoService) {
		this.almacenamientoService = almacenamientoService;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		try {
			Files.createDirectories(almacenamientoService.getRaiz());
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo crear la carpeta de archivos", e);
		}
		String ubicacion = almacenamientoService.getRaiz().toUri().toString();
		registry.addResourceHandler(AlmacenamientoService.RUTA_PUBLICA + "**")
				.addResourceLocations(ubicacion.endsWith("/") ? ubicacion : ubicacion + "/");
	}
}
