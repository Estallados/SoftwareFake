package co.edu.unbosque.marketplace.controller;

import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.service.NotificacionService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de notificaciones. Ruta base: {@code /notificaciones}.
 * Cada usuario ve y marca solo las suyas.
 *
 * @see "RF-003, CA-013"
 */
@RestController
@RequestMapping("/notificaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificaciones", description = "Notificaciones dentro de la plataforma")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Operation(summary = "Mis notificaciones, de la más reciente a la más antigua")
    @GetMapping
    public ResponseEntity<Object> listar(Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(notificacionService.listar(RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.pagina(pagina, tamanio, Sort.by("fecha").descending().and(Sort.by("idNotificacion")
                        .descending()))));
    }

    @Operation(summary = "Cantidad de notificaciones sin leer (contador de la campana)",
            description = "Devuelve {cantidad}.")
    @GetMapping("/noleidas")
    public ResponseEntity<Object> noLeidas(Authentication authentication) {
        return ResponseEntity.ok(Map.of("cantidad",
                notificacionService.contarNoLeidas(RespuestaUtil.idUsuario(authentication))));
    }

    @Operation(summary = "Marcar todas mis notificaciones como leídas", description = "Devuelve {actualizadas}.")
    @PatchMapping("/leidas")
    public ResponseEntity<Object> marcarTodasLeidas(Authentication authentication) {
        return ResponseEntity.ok(Map.of("actualizadas",
                notificacionService.marcarTodasLeidas(RespuestaUtil.idUsuario(authentication))));
    }

    @Operation(summary = "Marcar una notificación propia como leída")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Object> marcarLeida(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(notificacionService.marcarLeida(id, RespuestaUtil.idUsuario(authentication)));
    }
}
