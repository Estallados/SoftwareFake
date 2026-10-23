package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ReviewDTO;
import co.edu.unbosque.marketplace.dto.ReviewRequest;
import co.edu.unbosque.marketplace.service.ReviewService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reseñas", description = "Calificaciones de estancias completadas")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @PostMapping("/reviews")
  @PreAuthorize("hasAnyRole('GUEST','HOST')")
  public ResponseEntity<ReviewDTO> create(Principal principal, @Valid @RequestBody ReviewRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(principal.getName(), request));
  }

  @GetMapping("/listings/{listingId}/reviews")
  public List<ReviewDTO> byListing(@PathVariable Long listingId) {
    return reviewService.byListing(listingId);
  }
}
