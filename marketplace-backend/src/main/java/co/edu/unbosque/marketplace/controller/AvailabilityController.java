package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.AvailabilityDTO;
import co.edu.unbosque.marketplace.dto.AvailabilityRequest;
import co.edu.unbosque.marketplace.service.AvailabilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings/{listingId}/availability")
@Tag(name = "Disponibilidad", description = "Calendario de periodos del alojamiento")
public class AvailabilityController {

  private final AvailabilityService availabilityService;

  public AvailabilityController(AvailabilityService availabilityService) {
    this.availabilityService = availabilityService;
  }

  @GetMapping
  public List<AvailabilityDTO> list(@PathVariable Long listingId) {
    return availabilityService.list(listingId);
  }

  @PostMapping
  @PreAuthorize("hasRole('HOST')")
  public ResponseEntity<AvailabilityDTO> add(
      @PathVariable Long listingId, Principal principal, @Valid @RequestBody AvailabilityRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.add(listingId, principal.getName(), request));
  }

  @DeleteMapping("/{periodId}")
  @PreAuthorize("hasRole('HOST')")
  public ResponseEntity<Void> remove(@PathVariable Long listingId, @PathVariable Long periodId, Principal principal) {
    availabilityService.remove(listingId, periodId, principal.getName());
    return ResponseEntity.noContent().build();
  }
}
