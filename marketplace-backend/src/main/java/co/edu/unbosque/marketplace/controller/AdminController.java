package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.DashboardDTO;
import co.edu.unbosque.marketplace.dto.ListingDTO;
import co.edu.unbosque.marketplace.dto.ReasonRequest;
import co.edu.unbosque.marketplace.dto.ReviewDTO;
import co.edu.unbosque.marketplace.dto.UserDTO;
import co.edu.unbosque.marketplace.entity.AuditEvent;
import co.edu.unbosque.marketplace.entity.ListingStatus;
import co.edu.unbosque.marketplace.service.AuditService;
import co.edu.unbosque.marketplace.service.DashboardService;
import co.edu.unbosque.marketplace.service.ListingService;
import co.edu.unbosque.marketplace.service.ReviewService;
import co.edu.unbosque.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Moderación, administración de cuentas, auditoría y dashboard (solo ADMIN). */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración", description = "Moderación, cuentas, auditoría y dashboard")
public class AdminController {

  private final UserService userService;
  private final ListingService listingService;
  private final ReviewService reviewService;
  private final AuditService auditService;
  private final DashboardService dashboardService;

  public AdminController(
      UserService userService,
      ListingService listingService,
      ReviewService reviewService,
      AuditService auditService,
      DashboardService dashboardService) {
    this.userService = userService;
    this.listingService = listingService;
    this.reviewService = reviewService;
    this.auditService = auditService;
    this.dashboardService = dashboardService;
  }

  // ----- usuarios -----
  @GetMapping("/users")
  public Page<UserDTO> users(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return userService.list(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id")));
  }

  @PatchMapping("/users/{id}/restricted")
  public UserDTO restrict(@PathVariable Long id, @RequestParam boolean value, Principal principal) {
    return userService.setRestricted(id, value, principal.getName());
  }

  @PatchMapping("/users/{id}/active")
  public UserDTO active(@PathVariable Long id, @RequestParam boolean value, Principal principal) {
    return userService.setActive(id, value, principal.getName());
  }

  // ----- publicaciones -----
  @GetMapping("/listings")
  public Page<ListingDTO> listings(
      @RequestParam(required = false) ListingStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return listingService.adminList(status, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending()));
  }

  @PostMapping("/listings/{id}/approve")
  public ListingDTO approve(@PathVariable Long id, Principal principal) {
    return listingService.approve(id, principal.getName());
  }

  @PostMapping("/listings/{id}/reject")
  public ListingDTO reject(@PathVariable Long id, @RequestBody ReasonRequest body, Principal principal) {
    return listingService.reject(id, principal.getName(), body.reason());
  }

  @PostMapping("/listings/{id}/block")
  public ListingDTO block(@PathVariable Long id, @RequestBody ReasonRequest body, Principal principal) {
    return listingService.block(id, principal.getName(), body.reason());
  }

  @PostMapping("/listings/{id}/unblock")
  public ListingDTO unblock(@PathVariable Long id, Principal principal) {
    return listingService.unblock(id, principal.getName());
  }

  // ----- reseñas -----
  @PatchMapping("/reviews/{id}/hidden")
  public ReviewDTO hideReview(@PathVariable Long id, @RequestParam boolean value, Principal principal) {
    return reviewService.hide(id, value, principal.getName());
  }

  // ----- auditoría y dashboard -----
  @GetMapping("/audit")
  public Page<AuditEvent> audit(
      @RequestParam(required = false) String action,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return auditService.list(action, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200)));
  }

  @GetMapping("/dashboard")
  public DashboardDTO dashboard() {
    return dashboardService.summary();
  }
}
