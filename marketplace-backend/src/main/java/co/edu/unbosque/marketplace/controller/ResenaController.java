package co.edu.unbosque.marketplace.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.ResenaDTO;
import co.edu.unbosque.marketplace.service.ResenaService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de reseñas. Registrar requiere ser el huésped de una reserva
 * completada; la consulta por alojamiento es pública. El huésped ve sus reseñas
 * y el anfitrión las de sus alojamientos.
 *
 * @see "RF-004"
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reseñas", description = "Calificaciones de estancias completadas")
@CrossOrigin(origins = "*")
public class ResenaController {

	private static final Sort RECIENTES = Sort.by("fecha").descending();

	private final ResenaService resenaService;

	public ResenaController(ResenaService resenaService) {
		this.resenaService = resenaService;
	}

	@Operation(summary = "Reseñar una estancia completada (HUESPED)", description = "Una reseña por reserva, calificación de 1 a 5.")
	@PreAuthorize("hasRole('HUESPED')")
	@ApiResponse(responseCode = "201", description = "Reseña publicada; se notifica al anfitrión")
	@ApiResponse(responseCode = "400", description = "Indica la reserva y una calificación entre 1 y 5. | El "
			+ "comentario no puede superar 2000 caracteres.")
	@ApiResponse(responseCode = "404", description = "La reserva no existe.")
	@ApiResponse(responseCode = "409", description = "Solo puedes reseñar una estancia completada. | Ya registraste "
			+ "una reseña para esta reserva.")
	@PostMapping("/resenas")
	public ResponseEntity<Object> crear(@RequestBody ResenaDTO datos, Authentication authentication) {
		return RespuestaUtil.desde(resenaService.crear(datos, RespuestaUtil.idUsuario(authentication)),
				HttpStatus.CREATED);
	}

	@Operation(summary = "Mis reseñas como huésped, incluidas las ocultas (HUESPED)")
	@PreAuthorize("hasRole('HUESPED')")
	@ApiResponse(responseCode = "200", description = "Página de reseñas del huésped con su estado")
	@GetMapping("/resenas/mias")
	public ResponseEntity<Object> mias(Authentication authentication, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(resenaService.listarMias(RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	@Operation(summary = "Reseñas visibles de mis alojamientos con promedio y cantidad (ANFITRION)", description = "Con idAlojamiento se filtran las de ese alojamiento (debe ser propio).")
	@PreAuthorize("hasRole('ANFITRION')")
	@ApiResponse(responseCode = "200", description = "Reseñas de los alojamientos del anfitrión con promedio y "
			+ "cantidad")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@GetMapping("/resenas/anfitrion")
	public ResponseEntity<Object> deAnfitrion(Authentication authentication,
			@RequestParam(required = false) Long idAlojamiento, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		return RespuestaUtil.desde(resenaService.resumenDeAnfitrion(RespuestaUtil.idUsuario(authentication),
				idAlojamiento, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	@Operation(summary = "Reseñas visibles de un alojamiento con promedio y cantidad (público)")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Reseñas visibles con promedio y cantidad")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@GetMapping("/alojamientos/{idAlojamiento}/resenas")
	public ResponseEntity<Object> deAlojamiento(@PathVariable Long idAlojamiento,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return RespuestaUtil
				.desde(resenaService.resumen(idAlojamiento, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}
}
