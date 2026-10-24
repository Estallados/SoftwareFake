package co.edu.unbosque.marketplace.controller;

import java.time.LocalDate;

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

import co.edu.unbosque.marketplace.dto.PeriodoDisponibilidadDTO;
import co.edu.unbosque.marketplace.service.CalendarioService;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de disponibilidad. Ruta base:
 * {@code /alojamientos/{idAlojamiento}/disponibilidad}. El calendario y la
 * consulta por fechas son públicos para alojamientos ACTIVA.
 *
 * @see "RF-020, RF-025, RF-026, RNF-027"
 */
@RestController
@RequestMapping("/alojamientos/{idAlojamiento}/disponibilidad")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Disponibilidad", description = "Períodos ofrecidos, fechas reservadas y consulta por fechas")
public class DisponibilidadController {

    private final CalendarioService calendarioService;

    public DisponibilidadController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @Operation(summary = "Calendario: períodos disponibles y rangos reservados")
    @GetMapping
    public ResponseEntity<Object> calendario(@PathVariable Long idAlojamiento, Authentication authentication) {
        return RespuestaUtil.desde(calendarioService.calendario(idAlojamiento, RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.esAdmin(authentication)));
    }

    @Operation(summary = "Consultar disponibilidad en unas fechas (público)",
            description = "La salida debe ser posterior a la llegada.")
    @GetMapping("/consultar")
    public ResponseEntity<Object> consultar(@PathVariable Long idAlojamiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate llegada,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida) {
        return RespuestaUtil.desde(calendarioService.consultar(idAlojamiento, llegada, salida));
    }

    @Operation(summary = "Registrar período disponible (propietario o ADMIN)")
    @PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
    @PostMapping
    public ResponseEntity<Object> agregar(@PathVariable Long idAlojamiento, @RequestBody PeriodoDisponibilidadDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(calendarioService.agregarPeriodo(idAlojamiento, datos,
                RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)), HttpStatus.CREATED);
    }

    @Operation(summary = "Modificar período (no puede afectar reservas existentes)")
    @PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
    @PutMapping("/{idPeriodo}")
    public ResponseEntity<Object> actualizar(@PathVariable Long idAlojamiento, @PathVariable Long idPeriodo,
            @RequestBody PeriodoDisponibilidadDTO datos, Authentication authentication) {
        return RespuestaUtil.desde(calendarioService.actualizarPeriodo(idAlojamiento, idPeriodo, datos,
                RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)));
    }

    @Operation(summary = "Eliminar período sin reservas")
    @PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
    @DeleteMapping("/{idPeriodo}")
    public ResponseEntity<Object> eliminar(@PathVariable Long idAlojamiento, @PathVariable Long idPeriodo,
            Authentication authentication) {
        Resultado<Void> resultado = calendarioService.eliminarPeriodo(idAlojamiento, idPeriodo,
                RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication));
        return resultado.esExito() ? RespuestaUtil.mensaje(HttpStatus.OK, "Período eliminado.")
                : RespuestaUtil.desde(resultado);
    }
}
