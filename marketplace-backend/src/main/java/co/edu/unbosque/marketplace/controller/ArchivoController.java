package co.edu.unbosque.marketplace.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.service.AlmacenamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Sirve las imágenes subidas, guardadas en la base, en la misma URL que
 * devuelven los DTO ({@code /archivos/alojamientos/{id}/{nombre}}). La lectura
 * es pública (las imágenes se muestran en la búsqueda); subir y borrar pasa por
 * {@code AlojamientoController} con token.
 *
 * @see "RF-017"
 */
@RestController
@Tag(name = "Archivos", description = "Imágenes subidas (público)")
public class ArchivoController {

	/** El nombre es un UUID: el contenido de una URL nunca cambia. */
	private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

	private final AlmacenamientoService almacenamientoService;

	public ArchivoController(AlmacenamientoService almacenamientoService) {
		this.almacenamientoService = almacenamientoService;
	}

	@Operation(summary = "Imagen subida (público)")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "La imagen")
	@ApiResponse(responseCode = "404", description = "No existe")
	@GetMapping(AlmacenamientoService.RUTA_PUBLICA + "{carpeta}/{id}/{nombre}")
	public ResponseEntity<byte[]> imagen(@PathVariable String carpeta, @PathVariable String id,
			@PathVariable String nombre) {
		return almacenamientoService.buscar(carpeta + "/" + id + "/" + nombre)
				.map(archivo -> ResponseEntity.ok().cacheControl(CACHE)
						.contentType(MediaType.parseMediaType(archivo.getTipoContenido())).body(archivo.getDatos()))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
