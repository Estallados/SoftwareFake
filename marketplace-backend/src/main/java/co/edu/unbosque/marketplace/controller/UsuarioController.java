package co.edu.unbosque.marketplace.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.service.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Consultar el perfil propio
    @GetMapping("/perfil")
    public ResponseEntity<?> miPerfil(Authentication authentication) {
        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorCorreo(authentication.getName());

        if (usuario == null) {
            return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        }

        return ResponseEntity.ok(usuario);
    }

    // Actualizar el nombre del perfil propio
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @RequestBody UsuarioDTO cambios,
            Authentication authentication) {

        if (cambios.getNombre() == null || cambios.getNombre().isBlank()) {
            return mensaje(HttpStatus.BAD_REQUEST, "El nombre es obligatorio.");
        }

        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorCorreo(authentication.getName());

        if (usuario == null) {
            return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        }

        UsuarioDTO datos = new UsuarioDTO();
        datos.setNombre(cambios.getNombre().trim());

        switch (usuarioService.updateById(usuario.getIdUsuario(), datos)) {
            case 0:
                return mensaje(HttpStatus.OK, "Perfil actualizado correctamente.");
            case 1:
                return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
            default:
                return mensaje(HttpStatus.BAD_REQUEST, "No se pudo actualizar el perfil.");
        }
    }

    // Desactivar la cuenta propia
    @PatchMapping("/perfil/desactivar")
    public ResponseEntity<?> desactivarCuenta(Authentication authentication) {
        switch (usuarioService.desactivarCuenta(authentication.getName())) {
            case 0:
                return mensaje(HttpStatus.OK, "Cuenta desactivada correctamente.");
            case 1:
                return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
            default:
                return mensaje(HttpStatus.FORBIDDEN, "Esta cuenta no se puede desactivar.");
        }
    }

    // Listar todos los usuarios: solo administradores
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/listar")
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.getAll());
    }

    // Consultar usuario por ID: propio usuario o administrador
    @GetMapping("/obtener/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(
            @PathVariable Long id,
            Authentication authentication) {

        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(autoridad -> ROL_ADMIN.equals(autoridad.getAuthority()));

        if (!esAdmin) {
            UsuarioDTO propio = usuarioService.obtenerUsuarioPorCorreo(authentication.getName());

            if (propio == null || propio.getIdUsuario() == null
                    || !propio.getIdUsuario().equals(id)) {
                return mensaje(
                        HttpStatus.FORBIDDEN,
                        "No tienes permisos para consultar este usuario.");
            }
        }

        UsuarioDTO usuario = usuarioService.getById(id);

        if (usuario == null) {
            return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        }

        return ResponseEntity.ok(usuario);
    }

    // Consultar usuario por correo: solo administradores
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/obtenercorreo")
    public ResponseEntity<?> obtenerUsuarioPorCorreo(@RequestParam String correo) {
        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorCorreo(correo.trim().toLowerCase());

        if (usuario == null) {
            return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        }

        return ResponseEntity.ok(usuario);
    }

    private ResponseEntity<Map<String, String>> mensaje(
            HttpStatus status,
            String texto) {
        return ResponseEntity.status(status)
                .body(Map.of("message", texto));
    }
}
