package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ListingDTO;
import co.edu.unbosque.marketplace.dto.ListingRequest;
import co.edu.unbosque.marketplace.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings")
@Tag(name = "Alojamientos", description = "Publicaciones, ciclo de vida y búsqueda")
public class ListingController {

  private final ListingService listingService;

  public ListingController(ListingService listingService) {
    this.listingService = listingService;
  }

  @Operation(summary = "Buscar publicaciones activas (público)")
  @GetMapping
  public Page<ListingDTO> search(
      @RequestParam(required = false) String city,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
      @RequestParam(required = false) Integer guests,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Set<String> amenities,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending());
    return listingService.search(city, checkIn, checkOut, guests, type, minPrice, maxPrice, amenities, pageable);
  }

  @Operation(summary = "Detalle de una publicación (público si está activa)")
  @GetMapping("/{id}")
  public ListingDTO detail(@PathVariable Long id, Principal principal) {
    return listingService.getVisible(id, principal == null ? null : principal.getName());
  }

  @Operation(summary = "Mis publicaciones (anfitrión)")
  @GetMapping("/mine")
  @PreAuthorize("hasRole('HOST')")
  public List<ListingDTO> mine(Principal principal) {
    return listingService.mine(principal.getName());
  }

  @Operation(summary = "Crear publicación en borrador (anfitrión)")
  @PostMapping
  @PreAuthorize("hasRole('HOST')")
  public ResponseEntity<ListingDTO> create(Principal principal, @Valid @RequestBody ListingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(listingService.create(principal.getName(), request));
  }

  @Operation(summary = "Editar publicación (dueño o administrador)")
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('HOST','ADMIN')")
  public ListingDTO update(@PathVariable Long id, Principal principal, @Valid @RequestBody ListingRequest request) {
    return listingService.update(id, principal.getName(), request);
  }

  @Operation(summary = "Enviar a revisión (BORRADOR -> PENDIENTE_REVISION)")
  @PostMapping("/{id}/submit")
  @PreAuthorize("hasRole('HOST')")
  public ListingDTO submit(@PathVariable Long id, Principal principal) {
    return listingService.submitForReview(id, principal.getName());
  }

  @Operation(summary = "Pausar publicación activa")
  @PostMapping("/{id}/pause")
  @PreAuthorize("hasRole('HOST')")
  public ListingDTO pause(@PathVariable Long id, Principal principal) {
    return listingService.pause(id, principal.getName());
  }

  @Operation(summary = "Reactivar publicación pausada")
  @PostMapping("/{id}/resume")
  @PreAuthorize("hasRole('HOST')")
  public ListingDTO resume(@PathVariable Long id, Principal principal) {
    return listingService.resume(id, principal.getName());
  }
}
