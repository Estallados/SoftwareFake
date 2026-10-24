package co.edu.unbosque.marketplace.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.ConfiguracionTarifaDTO;
import co.edu.unbosque.marketplace.dto.MotivoDTO;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import co.edu.unbosque.marketplace.service.PublicacionService;
import co.edu.unbosque.marketplace.service.ResenaService;
import co.edu.unbosque.marketplace.service.TarifaService;
import co.edu.unbosque.marketplace.service.UsuarioService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de administración (solo ADMIN). Ruta base: {@code /admin}.
 * Moderación de alojamientos y reseñas, restricción de usuarios, tarifas
 * globales y consulta de la auditoría.
 *
 * @see "RF-013, RF-014, RF-015, RF-016, RF-023, RF-027"
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración", description = "Moderación, usuarios, tarifas y auditoría")
public class AdminController {

    private final UsuarioService usuarioService;
    private final PublicacionService publicacionService;
    private final ResenaService resenaService;
    private final TarifaService tarifaService;
    private final AuditoriaService auditoriaService;

    public AdminController(UsuarioService usuarioService, PublicacionService publicacionService,
            ResenaService resenaService, TarifaService tarifaService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.publicacionService = publicacionService;
        this.resenaService = resenaService;
        this.tarifaService = tarifaService;
        this.auditoriaService = auditoriaService;
    }

    // ----- usuarios -----

    @Operation(summary = "Usuarios (paginado)")
    @GetMapping("/usuarios")
    public ResponseEntity<Object> usuarios(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(usuarioService.listarPaginado(RespuestaUtil.pagina(pagina, tamanio,
                Sort.by("idUsuario"))));
    }

    @Operation(summary = "Restringir un usuario con motivo")
    @PatchMapping("/usuarios/{id}/restringir")
    public ResponseEntity<Object> restringir(@PathVariable Long id, @RequestBody MotivoDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(usuarioService.restringir(id, datos.getMotivo(),
                RespuestaUtil.idUsuario(authentication)));
    }

    @Operation(summary = "Liberar un usuario restringido con motivo")
    @PatchMapping("/usuarios/{id}/liberar")
    public ResponseEntity<Object> liberar(@PathVariable Long id, @RequestBody MotivoDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(usuarioService.liberar(id, datos.getMotivo(),
                RespuestaUtil.idUsuario(authentication)));
    }

    // ----- alojamientos -----

    @Operation(summary = "Alojamientos por estado (por ejemplo, la cola PENDIENTE_REVISION)")
    @GetMapping("/alojamientos")
    public ResponseEntity<Object> alojamientos(@RequestParam(required = false) EstadoAlojamiento estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(publicacionService.listarPorEstado(estado, RespuestaUtil.pagina(pagina, tamanio,
                Sort.by("fechaCreacion").descending())));
    }

    @Operation(summary = "Aprobar: PENDIENTE_REVISION → ACTIVA")
    @PostMapping("/alojamientos/{id}/aprobar")
    public ResponseEntity<Object> aprobar(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.aprobar(id, RespuestaUtil.idUsuario(authentication)));
    }

    @Operation(summary = "Rechazar con motivo: PENDIENTE_REVISION → BORRADOR (notifica al anfitrión)")
    @PostMapping("/alojamientos/{id}/rechazar")
    public ResponseEntity<Object> rechazar(@PathVariable Long id, @RequestBody MotivoDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.rechazar(id, RespuestaUtil.idUsuario(authentication),
                datos.getMotivo()));
    }

    @Operation(summary = "Bloquear con motivo (las reservas futuras pasan a EN_REVISION)")
    @PostMapping("/alojamientos/{id}/bloquear")
    public ResponseEntity<Object> bloquear(@PathVariable Long id, @RequestBody MotivoDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.bloquear(id, RespuestaUtil.idUsuario(authentication),
                datos.getMotivo()));
    }

    @Operation(summary = "Desbloquear: BLOQUEADA → PAUSADA")
    @PostMapping("/alojamientos/{id}/desbloquear")
    public ResponseEntity<Object> desbloquear(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.desbloquear(id, RespuestaUtil.idUsuario(authentication)));
    }

    // ----- reseñas -----

    @Operation(summary = "Ocultar una reseña con motivo (no se borra)")
    @PatchMapping("/resenas/{id}/ocultar")
    public ResponseEntity<Object> ocultarResena(@PathVariable Long id, @RequestBody MotivoDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(resenaService.moderar(id, true, datos.getMotivo(),
                RespuestaUtil.idUsuario(authentication)));
    }

    @Operation(summary = "Volver a mostrar una reseña oculta")
    @PatchMapping("/resenas/{id}/mostrar")
    public ResponseEntity<Object> mostrarResena(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(resenaService.moderar(id, false, null, RespuestaUtil.idUsuario(authentication)));
    }

    // ----- tarifas -----

    @Operation(summary = "Porcentajes globales vigentes de limpieza y servicio")
    @GetMapping("/tarifas")
    public ResponseEntity<Object> tarifas() {
        return ResponseEntity.ok(tarifaService.vigente());
    }

    @Operation(summary = "Cambiar los porcentajes globales (aplican a cotizaciones nuevas)")
    @PutMapping("/tarifas")
    public ResponseEntity<Object> actualizarTarifas(@RequestBody ConfiguracionTarifaDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(tarifaService.actualizar(datos, RespuestaUtil.idUsuario(authentication)));
    }

    // ----- auditoría -----

    @Operation(summary = "Consultar la auditoría con filtros por acción, actor y fechas")
    @GetMapping("/auditoria")
    public ResponseEntity<Object> auditoria(@RequestParam(required = false) String accion,
            @RequestParam(required = false) Long idActor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {
        return ResponseEntity.ok(auditoriaService.consultar(accion, idActor, desde, hasta,
                RespuestaUtil.pagina(pagina, tamanio, Sort.by("fecha").descending().and(Sort.by("idEvento")
                        .descending()))));
    }

    @Operation(summary = "Detalle de un evento de auditoría")
    @GetMapping("/auditoria/{id}")
    public ResponseEntity<Object> evento(@PathVariable Long id) {
        return RespuestaUtil.desde(auditoriaService.obtener(id));
    }
}
