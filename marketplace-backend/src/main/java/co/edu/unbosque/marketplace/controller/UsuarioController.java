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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de usuarios. Ruta base: {@code /usuarios}. Todas sus rutas
 * requieren token JWT; la identidad sale siempre del token.
 *
 * @see "RF-001"
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
@RestController
@RequestMapping("/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Perfil propio y administración de usuarios")
public class UsuarioController {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Consultar el perfil propio
    @Operation(summary = "Consultar el perfil propio")
    @GetMapping("/perfil")
    public ResponseEntity<?> miPerfil(Authentication authentication) {
        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorCorreo(authentication.getName());

        if (usuario == null) {
            return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        }

        return ResponseEntity.ok(usuario);
    }

    // Actualizar el perfil propio: solo nombre y teléfono
    @Operation(summary = "Actualizar el perfil propio",
            description = "Solo cambia nombre y teléfono; los demás campos enviados se ignoran.")
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @RequestBody UsuarioDTO cambios,
            Authentication authentication) {

        switch (usuarioService.actualizarPerfil(authentication.getName(), cambios.getNombre(),
                cambios.getTelefono())) {
            case 0:
                return mensaje(HttpStatus.OK, "Perfil actualizado correctamente.");
            case 1:
                return mensaje(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
            default:
                return mensaje(HttpStatus.BAD_REQUEST, "Envía un nombre no vacío y/o un teléfono "
                        + "de 7 a 15 dígitos.");
        }
    }

    // Desactivar la cuenta propia
    @Operation(summary = "Desactivar la cuenta propia",
            description = "Se puede reactivar luego con POST /auth/reactivar.")
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

    // Crear otro administrador: solo administradores (decisión sobre RF-001)
    @Operation(summary = "Crear un administrador (solo ADMIN)",
            description = "El registro público nunca crea administradores; el rol enviado se ignora.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/crearadministrador")
    public ResponseEntity<?> crearAdministrador(
            @RequestBody UsuarioDTO datos,
            Authentication authentication) {

        Long idActor = authentication.getPrincipal() instanceof Usuario admin ? admin.getIdUsuario() : null;

        switch (usuarioService.crearAdministrador(datos, idActor)) {
            case UsuarioService.CREADO:
                return mensaje(HttpStatus.CREATED, "Administrador creado correctamente.");
            case UsuarioService.CORREO_DUPLICADO:
                return mensaje(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo electrónico.");
            case UsuarioService.CONTRASENIA_DEBIL:
                return mensaje(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres, "
                        + "una mayúscula, una minúscula, un número y un símbolo.");
            case UsuarioService.DATOS_INVALIDOS:
                return mensaje(HttpStatus.BAD_REQUEST, "Datos incompletos o inválidos.");
            default:
                return mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el administrador.");
        }
    }

    // Listar todos los usuarios: solo administradores
    @Operation(summary = "Listar usuarios (solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/listar")
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.getAll());
    }

    // Consultar usuario por ID: propio usuario o administrador
    @Operation(summary = "Consultar un usuario por id (el propio o, si eres ADMIN, cualquiera)")
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
    @Operation(summary = "Consultar un usuario por correo (solo ADMIN)")
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
