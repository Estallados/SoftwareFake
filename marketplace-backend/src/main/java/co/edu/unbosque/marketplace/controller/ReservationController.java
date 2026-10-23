package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CancelRequest;
import co.edu.unbosque.marketplace.dto.QuoteRequest;
import co.edu.unbosque.marketplace.dto.QuoteResponse;
import co.edu.unbosque.marketplace.dto.ReservationDTO;
import co.edu.unbosque.marketplace.dto.ReservationRequest;
import co.edu.unbosque.marketplace.exception.ApiException;
import co.edu.unbosque.marketplace.service.QuoteService;
import co.edu.unbosque.marketplace.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Cotización y reservas", description = "Cotizar, reservar (idempotente) y cancelar")
public class ReservationController {

  private final ReservationService reservationService;
  private final QuoteService quoteService;

  public ReservationController(ReservationService reservationService, QuoteService quoteService) {
    this.reservationService = reservationService;
    this.quoteService = quoteService;
  }

  @Operation(summary = "Cotizar una estancia")
  @PostMapping("/quotes")
  public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
    return quoteService.quote(request);
  }

  @Operation(summary = "Crear reserva inmediata", description = "Requiere el encabezado Idempotency-Key (único por intento de reserva).")
  @PostMapping("/reservations")
  @PreAuthorize("hasAnyRole('GUEST','HOST')")
  public ResponseEntity<ReservationDTO> create(
      Principal principal,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody ReservationRequest request) {
    if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
      throw ApiException.badRequest("Idempotency-Key debe tener entre 1 y 100 caracteres");
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(principal.getName(), idempotencyKey, request));
  }

  @Operation(summary = "Mis reservas como huésped")
  @GetMapping("/reservations/mine")
  public List<ReservationDTO> mine(Principal principal) {
    return reservationService.mine(principal.getName());
  }

  @Operation(summary = "Reservas de mis publicaciones (anfitrión)")
  @GetMapping("/reservations/host")
  @PreAuthorize("hasRole('HOST')")
  public List<ReservationDTO> forHost(Principal principal) {
    return reservationService.forHost(principal.getName());
  }

  @GetMapping("/reservations/{id}")
  public ReservationDTO detail(@PathVariable Long id, Principal principal) {
    return reservationService.getById(id, principal.getName());
  }

  @Operation(summary = "Cancelar reserva (aplica la política de cancelación)")
  @PostMapping("/reservations/{id}/cancel")
  public ReservationDTO cancel(@PathVariable Long id, Principal principal, @Valid @RequestBody CancelRequest request) {
    return reservationService.cancel(id, principal.getName(), request.reason());
  }
}
