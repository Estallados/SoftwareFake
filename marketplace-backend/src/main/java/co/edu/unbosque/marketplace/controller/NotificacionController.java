package co.edu.unbosque.marketplace.controller;

import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.service.NotificacionService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de notificaciones. Ruta base: {@code /notificaciones}. Cada
 * usuario ve y marca solo las suyas.
 */
@RestController
@RequestMapping("/notificaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificaciones", description = "Notificaciones dentro de la plataforma")
@CrossOrigin(origins = "*")
public class NotificacionController {

	/** Servicio de notificaciones. */
	private final NotificacionService notificacionService;

	/**
	 * Crea el controlador.
	 *
	 * @param notificacionService servicio de notificaciones
	 */
	public NotificacionController(NotificacionService notificacionService) {
		this.notificacionService = notificacionService;
	}

	/**
	 * Mis notificaciones, de la más reciente a la más antigua. Ruta:
	 * {@code GET /notificaciones}.
	 *
	 * @param authentication usuario autenticado
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de notificaciones
	 */
	@Operation(summary = "Mis notificaciones, de la más reciente a la más antigua")
	@ApiResponse(responseCode = "200", description = "Página de notificaciones, las más recientes primero")
	@GetMapping
	public ResponseEntity<Object> listar(Authentication authentication, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(notificacionService.listar(RespuestaUtil.idUsuario(authentication), RespuestaUtil
				.pagina(pagina, tamanio, Sort.by("fecha").descending().and(Sort.by("idNotificacion").descending()))));
	}

	/**
	 * Cantidad de notificaciones sin leer, para el contador de la campana.
	 * Ruta: {@code GET /notificaciones/noleidas}.
	 *
	 * @param authentication usuario autenticado
	 * @return 200 con {cantidad}
	 */
	@Operation(summary = "Cantidad de notificaciones sin leer (contador de la campana)", description = "Devuelve {cantidad}.")
	@ApiResponse(responseCode = "200", description = "{cantidad}: notificaciones sin leer")
	@GetMapping("/noleidas")
	public ResponseEntity<Object> noLeidas(Authentication authentication) {
		return ResponseEntity
				.ok(Map.of("cantidad", notificacionService.contarNoLeidas(RespuestaUtil.idUsuario(authentication))));
	}

	/**
	 * Marca todas mis notificaciones como leídas. Ruta:
	 * {@code PATCH /notificaciones/leidas}.
	 *
	 * @param authentication usuario autenticado
	 * @return 200 con {actualizadas}: cuántas se marcaron
	 */
	@Operation(summary = "Marcar todas mis notificaciones como leídas", description = "Devuelve {actualizadas}.")
	@ApiResponse(responseCode = "200", description = "{actualizadas}: cuántas se marcaron")
	@PatchMapping("/leidas")
	public ResponseEntity<Object> marcarTodasLeidas(Authentication authentication) {
		return ResponseEntity.ok(
				Map.of("actualizadas", notificacionService.marcarTodasLeidas(RespuestaUtil.idUsuario(authentication))));
	}

	/**
	 * Marca una notificación propia como leída. Ruta:
	 * {@code PATCH /notificaciones/{id}/leida}.
	 *
	 * @param id notificación
	 * @param authentication usuario autenticado
	 * @return 200 con la notificación; 404 si no existe o es de otro usuario
	 */
	@Operation(summary = "Marcar una notificación propia como leída")
	@ApiResponse(responseCode = "200", description = "Notificación marcada como leída")
	@ApiResponse(responseCode = "404", description = "La notificación no existe.")
	@PatchMapping("/{id}/leida")
	public ResponseEntity<Object> marcarLeida(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(notificacionService.marcarLeida(id, RespuestaUtil.idUsuario(authentication)));
	}
}
