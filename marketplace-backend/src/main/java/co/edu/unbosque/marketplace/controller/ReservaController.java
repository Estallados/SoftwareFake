package co.edu.unbosque.marketplace.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.CotizacionDTO;
import co.edu.unbosque.marketplace.dto.MotivoDTO;
import co.edu.unbosque.marketplace.dto.SolicitudReservaDTO;
import co.edu.unbosque.marketplace.service.CancelacionService;
import co.edu.unbosque.marketplace.service.CotizacionService;
import co.edu.unbosque.marketplace.service.IdempotenciaService;
import co.edu.unbosque.marketplace.service.ReservaService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de cotizaciones, reservas y cancelaciones. Crear y cancelar
 * son idempotentes con la cabecera {@code Idempotency-Key}.
 *
 * @see "RF-002, RF-021, RF-022, RF-028, RNF-013, RNF-014"
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cotización y reservas", description = "Cotizar, reservar con pago de prueba, consultar y cancelar")
public class ReservaController {

    private static final Sort RECIENTES = Sort.by("fechaCreacion").descending();

    private final CotizacionService cotizacionService;
    private final ReservaService reservaService;
    private final CancelacionService cancelacionService;
    private final IdempotenciaService idempotenciaService;

    public ReservaController(CotizacionService cotizacionService, ReservaService reservaService,
            CancelacionService cancelacionService, IdempotenciaService idempotenciaService) {
        this.cotizacionService = cotizacionService;
        this.reservaService = reservaService;
        this.cancelacionService = cancelacionService;
        this.idempotenciaService = idempotenciaService;
    }

    @Operation(summary = "Cotizar una estancia",
            description = "Devuelve noches, precio base, descuento, limpieza, servicio, adicionales y total. "
                    + "La cotización sirve 30 minutos para reservar.")
    @PostMapping("/cotizaciones")
    public ResponseEntity<Object> cotizar(@RequestBody CotizacionDTO solicitud, Authentication authentication) {
        return RespuestaUtil.desde(cotizacionService.cotizar(solicitud, RespuestaUtil.idUsuario(authentication)),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Crear y pagar una reserva (HUESPED)",
            description = "Requiere la cabecera Idempotency-Key. Métodos de pago de prueba: pm_card_visa (aprobado), "
                    + "pm_card_chargeDeclined (rechazado), pm_card_error (error).")
    @PreAuthorize("hasRole('HUESPED')")
    @PostMapping("/reservas")
    public ResponseEntity<Object> crear(@RequestBody SolicitudReservaDTO solicitud,
            @RequestHeader(value = IdempotenciaService.CABECERA, required = false) String clave,
            Authentication authentication) {
        Long idHuesped = RespuestaUtil.idUsuario(authentication);
        return idempotenciaService.ejecutar(idHuesped, "CREAR_RESERVA", clave, solicitud,
                () -> RespuestaUtil.desde(reservaService.crear(solicitud, idHuesped), HttpStatus.CREATED));
    }

    @Operation(summary = "Mis reservas como huésped")
    @GetMapping("/reservas/mias")
    public ResponseEntity<Object> mias(Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(reservaService.listarMias(RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
    }

    @Operation(summary = "Reservas de mis alojamientos (ANFITRION)")
    @PreAuthorize("hasRole('ANFITRION')")
    @GetMapping("/reservas/anfitrion")
    public ResponseEntity<Object> deAnfitrion(Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(reservaService.listarDeAnfitrion(RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
    }

    @Operation(summary = "Detalle de una reserva (huésped, anfitrión o ADMIN)")
    @GetMapping("/reservas/{id}")
    public ResponseEntity<Object> detalle(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(reservaService.obtener(id, RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.esAdmin(authentication)));
    }

    @Operation(summary = "Simular la cancelación de una reserva (HUESPED)",
            description = "Devuelve la política, los días de anticipación, el porcentaje y el valor que se "
                    + "devolvería si se cancelara hoy. No cancela nada.")
    @PreAuthorize("hasRole('HUESPED')")
    @GetMapping("/reservas/{id}/simularcancelacion")
    public ResponseEntity<Object> simularCancelacion(@PathVariable Long id, Authentication authentication) {
        return RespuestaUtil.desde(cancelacionService.simular(id, RespuestaUtil.idUsuario(authentication)));
    }

    @Operation(summary = "Cancelar una reserva futura (HUESPED)",
            description = "Aplica la política de cancelación del alojamiento. Requiere Idempotency-Key y motivo.")
    @PreAuthorize("hasRole('HUESPED')")
    @PostMapping("/reservas/{id}/cancelar")
    public ResponseEntity<Object> cancelar(@PathVariable Long id, @RequestBody MotivoDTO datos,
            @RequestHeader(value = IdempotenciaService.CABECERA, required = false) String clave,
            Authentication authentication) {
        Long idHuesped = RespuestaUtil.idUsuario(authentication);
        return idempotenciaService.ejecutar(idHuesped, "CANCELAR_RESERVA", clave,
                new Object[] { id, datos.getMotivo() },
                () -> RespuestaUtil.desde(cancelacionService.cancelar(id, datos.getMotivo(), idHuesped)));
    }
}
