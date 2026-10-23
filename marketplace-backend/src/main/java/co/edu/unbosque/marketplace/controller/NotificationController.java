package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.NotificationDTO;
import co.edu.unbosque.marketplace.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notificaciones", description = "Notificaciones dentro de la plataforma")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public List<NotificationDTO> mine(Principal principal) {
    return notificationService.mine(principal.getName());
  }

  @PatchMapping("/{id}/read")
  public NotificationDTO markRead(@PathVariable Long id, Principal principal) {
    return notificationService.markRead(id, principal.getName());
  }
}
