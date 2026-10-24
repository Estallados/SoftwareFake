package co.edu.unbosque.marketplace.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.AlojamientoDTO;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalDTO;
import co.edu.unbosque.marketplace.service.PublicacionService;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de alojamientos (publicaciones). Ruta base:
 * {@code /alojamientos}. La búsqueda, el detalle de publicaciones ACTIVA y las
 * políticas son públicos; lo demás requiere token.
 *
 * @see "RF-017, RF-018, RF-019, RF-024, RF-027"
 */
@RestController
@RequestMapping("/alojamientos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Alojamientos", description = "Publicaciones, ciclo de estados, servicios adicionales y búsqueda")
public class AlojamientoController {

    private static final Sort RECIENTES = Sort.by("fechaCreacion").descending();

    private final PublicacionService publicacionService;

    public AlojamientoController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @Operation(summary = "Buscar alojamientos ACTIVA (público)",
            description = "Filtros opcionales combinables: ciudad, fechas, huéspedes, tipo, rango de precio y amenidades.")
    @GetMapping
    public ResponseEntity<Object> buscar(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate llegada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam(required = false) Integer huespedes,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) BigDecimal precioMinimo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @RequestParam(required = false) List<String> amenidades,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio) {
        return RespuestaUtil.desde(publicacionService.buscar(ciudad, llegada, salida, huespedes, tipo, precioMinimo,
                precioMaximo, amenidades, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
    }

    @Operation(summary = "Políticas de cancelación disponibles (público)")
    @GetMapping("/politicas")
    public ResponseEntity<Object> politicas() {
        return ResponseEntity.ok(publicacionService.politicas());
    }

    @Operation(summary = "Mis alojamientos con su estado (ANFITRION)")
    @PreAuthorize("hasRole('ANFITRION')")
    @GetMapping("/mios")
    public ResponseEntity<Object> mios(Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(publicacionService.mios(RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
    }

    @Operation(summary = "Detalle de un alojamiento",
            description = "Público si está ACTIVA; en otro estado solo para su anfitrión o un administrador.")
    @GetMapping("/{id}")
    public ResponseEntity<Object> detalle(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.obtener(id, RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.esAdmin(authentication)));
    }

    @Operation(summary = "Historial de estados (propietario o ADMIN)")
    @GetMapping("/{id}/historial")
    public ResponseEntity<Object> historial(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.historial(id, RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.esAdmin(authentication)));
    }

    @Operation(summary = "Crear alojamiento en BORRADOR (ANFITRION)")
    @PreAuthorize("hasRole('ANFITRION')")
    @PostMapping
    public ResponseEntity<Object> crear(@RequestBody AlojamientoDTO datos, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.crear(datos, RespuestaUtil.idUsuario(authentication)),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Editar alojamiento (propietario o ADMIN)",
            description = "Los campos en null no cambian; imágenes y amenidades enviadas reemplazan las anteriores.")
    @PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id, @RequestBody AlojamientoDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.actualizar(id, datos, RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.esAdmin(authentication)));
    }

    @Operation(summary = "Enviar a revisión: BORRADOR → PENDIENTE_REVISION (ANFITRION)")
    @PreAuthorize("hasRole('ANFITRION')")
    @PostMapping("/{id}/enviarrevision")
    public ResponseEntity<Object> enviarRevision(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.enviarRevision(id, RespuestaUtil.idUsuario(authentication)));
    }

    @Operation(summary = "Pausar: ACTIVA → PAUSADA (ANFITRION)")
    @PreAuthorize("hasRole('ANFITRION')")
    @PostMapping("/{id}/pausar")
    public ResponseEntity<Object> pausar(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.pausar(id, RespuestaUtil.idUsuario(authentication)));
    }

    @Operation(summary = "Reanudar: PAUSADA → ACTIVA (ANFITRION)")
    @PreAuthorize("hasRole('ANFITRION')")
    @PostMapping("/{id}/reanudar")
    public ResponseEntity<Object> reanudar(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.reanudar(id, RespuestaUtil.idUsuario(authentication)));
    }

    @Operation(summary = "Agregar servicio adicional con costo (propietario o ADMIN)")
    @PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
    @PostMapping("/{id}/servicios")
    public ResponseEntity<Object> agregarServicio(@PathVariable Long id, @RequestBody ServicioAdicionalDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.agregarServicio(id, datos,
                RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)), HttpStatus.CREATED);
    }

    @Operation(summary = "Modificar servicio adicional (propietario o ADMIN)")
    @PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
    @PutMapping("/{id}/servicios/{idServicio}")
    public ResponseEntity<Object> actualizarServicio(@PathVariable Long id, @PathVariable Long idServicio,
            @RequestBody ServicioAdicionalDTO datos, Authentication authentication) {
        return RespuestaUtil.desde(publicacionService.actualizarServicio(id, idServicio, datos,
                RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)));
    }

    @Operation(summary = "Eliminar servicio adicional (propietario o ADMIN)")
    @PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
    @DeleteMapping("/{id}/servicios/{idServicio}")
    public ResponseEntity<Object> eliminarServicio(@PathVariable Long id, @PathVariable Long idServicio,
            Authentication authentication) {
        Resultado<Void> resultado = publicacionService.eliminarServicio(id, idServicio,
                RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication));
        return resultado.esExito() ? RespuestaUtil.mensaje(HttpStatus.OK, "Servicio adicional eliminado.")
                : RespuestaUtil.desde(resultado);
    }
}
