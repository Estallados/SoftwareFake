package co.edu.unbosque.marketplace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.ConfiguracionPagoDTO;
import co.edu.unbosque.marketplace.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de pagos. Ruta base: {@code /pagos}. El cobro ocurre al
 * crear la reserva ({@code POST /reservas}); aquí el front consulta cómo
 * cobrar.
 *
 * @see "RF-028"
 */
@RestController
@RequestMapping("/pagos")
@Tag(name = "Pagos", description = "Pagos simulados con Stripe (modo prueba)")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(summary = "Configuración de pagos para el front (público)",
            description = "modo STRIPE_TEST (cobros en el modo de prueba de Stripe) o SIMULADO (sin clave de Stripe), "
                    + "clave publicable para Stripe Elements, moneda y métodos de prueba con su resultado. El "
                    + "metodoPago de POST /reservas es uno de esos métodos o el PaymentMethod (pm_...) que crea "
                    + "Stripe Elements con una tarjeta de prueba, como 4242 4242 4242 4242.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Configuración de pagos")
    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionPagoDTO> configuracion() {
        return ResponseEntity.ok(pagoService.configuracion());
    }
}
