package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.AuthResponse;
import co.edu.unbosque.marketplace.dto.ForgotPasswordRequest;
import co.edu.unbosque.marketplace.dto.LoginRequest;
import co.edu.unbosque.marketplace.dto.RegisterRequest;
import co.edu.unbosque.marketplace.dto.ResetPasswordRequest;
import co.edu.unbosque.marketplace.dto.UserDTO;
import co.edu.unbosque.marketplace.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro, login (JWT) y recuperación de contraseña")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "Registrar usuario (rol GUEST o HOST)")
  @PostMapping("/register")
  public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @Operation(summary = "Iniciar sesión y obtener token JWT")
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @Operation(summary = "Recuperación simulada de contraseña (genera token, no envía correo)")
  @PostMapping("/forgot-password")
  public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    return ResponseEntity.ok(authService.forgotPassword(request.email()));
  }

  @Operation(summary = "Restablecer contraseña con el token de recuperación")
  @PostMapping("/reset-password")
  public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(Map.of("message", "Contraseña actualizada"));
  }
}
