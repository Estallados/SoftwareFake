package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.UpdateProfileRequest;
import co.edu.unbosque.marketplace.dto.UserDTO;
import co.edu.unbosque.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Perfil", description = "Consulta y actualización del perfil propio")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public UserDTO me(Principal principal) {
    return userService.me(principal.getName());
  }

  @PutMapping("/me")
  public UserDTO update(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(principal.getName(), request);
  }

  @PatchMapping("/me/deactivate")
  public ResponseEntity<Map<String, String>> deactivate(Principal principal) {
    userService.deactivateOwnAccount(principal.getName());
    return ResponseEntity.ok(Map.of("message", "Cuenta desactivada. Un administrador puede reactivarla."));
  }
}
