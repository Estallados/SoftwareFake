package co.edu.unbosque.marketplace.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.service.ReporteService;
import co.edu.unbosque.marketplace.util.ArchivoReporte;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST del dashboard y los reportes (solo ADMIN).
 *
 * @see "RF-005, RF-006"
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard y reportes", description = "Indicadores y exportación en PDF o Excel")
@CrossOrigin(origins = "*")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Operation(summary = "Indicadores: usuarios, alojamientos activos, reservas por estado, ingresos y demanda")
    @ApiResponse(responseCode = "200", description = "Indicadores del dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<Object> dashboard() {
        return ResponseEntity.ok(reporteService.dashboard());
    }

    @Operation(summary = "Historial de exportaciones (quién, qué, formato y cuándo)")
    @ApiResponse(responseCode = "200", description = "Página de exportaciones con quién las hizo")
    @GetMapping("/reportes/historial")
    public ResponseEntity<Object> historial(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(reporteService.historial(RespuestaUtil.pagina(pagina, tamanio,
                Sort.by("fecha").descending().and(Sort.by("idRegistro").descending()))));
    }

    @Operation(summary = "Exportar un reporte",
            description = "tipo: usuarios, alojamientos, reservas, ingresos o demanda; formato: pdf o excel.")
    @ApiResponse(responseCode = "200", description = "Archivo PDF o Excel")
    @ApiResponse(responseCode = "400", description = "Reporte no válido. Tipos: usuarios, alojamientos, reservas, "
            + "ingresos, demanda; formatos: pdf, excel.")
    @GetMapping("/reportes/{tipo}")
    public ResponseEntity<Object> exportar(@PathVariable String tipo,
            @RequestParam(defaultValue = "pdf") String formato, Authentication authentication) {
        Resultado<ArchivoReporte> resultado = reporteService.exportar(tipo, formato,
                RespuestaUtil.idUsuario(authentication));
        if (!resultado.esExito()) {
            return RespuestaUtil.desde(resultado);
        }
        ArchivoReporte archivo = resultado.getDato();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.getNombreArchivo()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.getTipoContenido()))
                .body(archivo.getContenido());
    }
}
