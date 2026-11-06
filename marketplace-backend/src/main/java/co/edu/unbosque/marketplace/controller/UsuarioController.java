package co.edu.unbosque.marketplace.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.service.UsuarioService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de usuarios. Ruta base: {@code /usuarios}. Todas sus rutas
 * exigen token JWT, y la identidad del usuario sale siempre del token, nunca
 * del cuerpo de la petición.
 */
@RestController
@RequestMapping("/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Perfil propio y administración de usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

	/** Autoridad del rol administrador. */
	private static final String ROL_ADMIN = "ROLE_ADMIN";

	/** Mensaje cuando el usuario no existe. */
	private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado.";

	/** Servicio de usuarios. */
	private final UsuarioService usuarioService;

	/**
	 * Crea el controlador.
	 *
	 * @param usuarioService servicio de usuarios
	 */
	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	// Consultar el perfil propio
	/**
	 * Consulta el perfil propio. Ruta: {@code GET /usuarios/perfil}.
	 *
	 * @param authentication usuario autenticado
	 * @return 200 con el perfil; 404 si no existe
	 */
	@Operation(summary = "Consultar el perfil propio")
	@ApiResponse(responseCode = "200", description = "Perfil del usuario autenticado")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@GetMapping("/perfil")
	public ResponseEntity<Object> miPerfil(Authentication authentication) {
		UsuarioDTO usuario = usuarioService.obtenerUsuarioPorCorreo(authentication.getName());

		if (usuario == null) {
			return mensaje(HttpStatus.NOT_FOUND, USUARIO_NO_ENCONTRADO);
		}

		return ResponseEntity.ok(usuario);
	}

	// Actualizar el perfil propio: solo nombre y teléfono
	/**
	 * Actualiza el perfil propio; solo cambia el nombre y el teléfono, los
	 * demás campos se ignoran. Ruta: {@code PUT /usuarios/perfil}.
	 *
	 * @param cambios DTO con {@code nombre} y/o {@code telefono}
	 * @param authentication usuario autenticado
	 * @return 200 si se actualizó; 400 si los datos no son válidos; 404 si no
	 *         existe
	 */
	@Operation(summary = "Actualizar el perfil propio", description = "Solo cambia nombre y teléfono; los demás campos enviados se ignoran.")
	@ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente.")
	@ApiResponse(responseCode = "400", description = "Envía un nombre no vacío (máximo 255 caracteres) y/o un teléfono de 7 a 15 dígitos.")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@PutMapping("/perfil")
	public ResponseEntity<Object> actualizarPerfil(@RequestBody UsuarioDTO cambios, Authentication authentication) {

		return switch (usuarioService.actualizarPerfil(authentication.getName(), cambios.getNombre(),
				cambios.getTelefono())) {
		case 0 -> mensaje(HttpStatus.OK, "Perfil actualizado correctamente.");
		case 1 -> mensaje(HttpStatus.NOT_FOUND, USUARIO_NO_ENCONTRADO);
		default -> mensaje(HttpStatus.BAD_REQUEST, "Envía un nombre no vacío (máximo 255 caracteres) y/o un teléfono de 7 a 15 dígitos.");
		};
	}

	// Desactivar la cuenta propia; la respuesta trae el nuevo estado
	/**
	 * Desactiva la cuenta propia; se puede reactivar luego con
	 * {@code POST /auth/reactivar}. Ruta:
	 * {@code PATCH /usuarios/perfil/desactivar}.
	 *
	 * @param authentication usuario autenticado
	 * @return 200 con {message, estado}; 403 si la cuenta no se puede
	 *         desactivar; 404 si no existe
	 */
	@Operation(summary = "Desactivar la cuenta propia", description = "Responde {message, estado}. Se puede reactivar luego con POST /auth/reactivar.")
	@ApiResponse(responseCode = "200", description = "Cuenta desactivada correctamente: {message, estado}")
	@ApiResponse(responseCode = "403", description = "Esta cuenta no se puede desactivar.")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@PatchMapping("/perfil/desactivar")
	public ResponseEntity<Object> desactivarCuenta(Authentication authentication) {
		return switch (usuarioService.desactivarCuenta(authentication.getName())) {
		case 0 -> ResponseEntity
				.ok(Map.of("message", "Cuenta desactivada correctamente.", "estado", EstadoUsuario.INACTIVO.name()));
		case 1 -> mensaje(HttpStatus.NOT_FOUND, USUARIO_NO_ENCONTRADO);
		default -> mensaje(HttpStatus.FORBIDDEN, "Esta cuenta no se puede desactivar.");
		};
	}

	// Crear otro administrador: solo administradores (el registro público nunca los crea)
	/**
	 * Crea un administrador (solo administradores). El rol enviado se ignora.
	 * Ruta: {@code POST /usuarios/crearadministrador}.
	 *
	 * @param datos datos del nuevo administrador
	 * @param authentication administrador autenticado
	 * @return 201 si se creó; 400 si los datos o la contraseña no son válidos;
	 *         409 si el correo ya existe; 500 si falló el guardado
	 */
	@Operation(summary = "Crear un administrador (solo ADMIN)", description = "El registro público nunca crea administradores; el rol enviado se ignora.")
	@PreAuthorize("hasRole('ADMIN')")
	@ApiResponse(responseCode = "201", description = "Administrador creado correctamente.")
	@ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos. | La contraseña debe tener al "
			+ "menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo.")
	@ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese correo electrónico.")
	@ApiResponse(responseCode = "500", description = "No se pudo crear el administrador.")
	@PostMapping("/crearadministrador")
	public ResponseEntity<Object> crearAdministrador(@RequestBody UsuarioDTO datos, Authentication authentication) {

		Long idActor = authentication.getPrincipal() instanceof Usuario admin ? admin.getIdUsuario() : null;

		return switch (usuarioService.crearAdministrador(datos, idActor)) {
		case UsuarioService.CREADO -> mensaje(HttpStatus.CREATED, "Administrador creado correctamente.");
		case UsuarioService.CORREO_DUPLICADO ->
			mensaje(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo electrónico.");
		case UsuarioService.CONTRASENIA_DEBIL ->
			mensaje(HttpStatus.BAD_REQUEST, "La contraseña debe tener entre 8 y 64 caracteres, "
					+ "una mayúscula, una minúscula, un número y un símbolo.");
		case UsuarioService.DATOS_INVALIDOS -> mensaje(HttpStatus.BAD_REQUEST, "Datos incompletos o inválidos.");
		default -> mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el administrador.");
		};
	}

	// Listar todos los usuarios: solo administradores
	/**
	 * Lista todos los usuarios (solo administradores). Ruta:
	 * {@code GET /usuarios/listar}.
	 *
	 * @return 200 con todos los usuarios
	 */
	@Operation(summary = "Listar usuarios (solo ADMIN)")
	@PreAuthorize("hasRole('ADMIN')")
	@ApiResponse(responseCode = "200", description = "Todos los usuarios")
	@GetMapping("/listar")
	public ResponseEntity<List<UsuarioDTO>> listarTodos() {
		return ResponseEntity.ok(usuarioService.getAll());
	}

	// Consultar usuario por ID: propio usuario o administrador
	/**
	 * Consulta un usuario por id: el propio o, si es administrador,
	 * cualquiera. Ruta: {@code GET /usuarios/obtener/{id}}.
	 *
	 * @param id usuario a consultar
	 * @param authentication usuario autenticado
	 * @return 200 con el usuario; 403 si no tiene permiso; 404 si no existe
	 */
	@Operation(summary = "Consultar un usuario por id (el propio o, si eres ADMIN, cualquiera)")
	@ApiResponse(responseCode = "200", description = "Usuario")
	@ApiResponse(responseCode = "403", description = "No tienes permisos para consultar este usuario.")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@GetMapping("/obtener/{id}")
	public ResponseEntity<Object> obtenerUsuarioPorId(@PathVariable Long id, Authentication authentication) {

		boolean esAdmin = authentication.getAuthorities().stream()
				.anyMatch(autoridad -> ROL_ADMIN.equals(autoridad.getAuthority()));

		if (!esAdmin) {
			UsuarioDTO propio = usuarioService.obtenerUsuarioPorCorreo(authentication.getName());

			if (propio == null || propio.getIdUsuario() == null || !propio.getIdUsuario().equals(id)) {
				return mensaje(HttpStatus.FORBIDDEN, "No tienes permisos para consultar este usuario.");
			}
		}

		UsuarioDTO usuario = usuarioService.getById(id);

		if (usuario == null) {
			return mensaje(HttpStatus.NOT_FOUND, USUARIO_NO_ENCONTRADO);
		}

		return ResponseEntity.ok(usuario);
	}

	// Consultar usuario por correo: solo administradores
	/**
	 * Consulta un usuario por correo (solo administradores). Ruta:
	 * {@code GET /usuarios/obtenercorreo}.
	 *
	 * @param correo correo a buscar
	 * @return 200 con el usuario; 404 si no existe
	 */
	@Operation(summary = "Consultar un usuario por correo (solo ADMIN)")
	@PreAuthorize("hasRole('ADMIN')")
	@ApiResponse(responseCode = "200", description = "Usuario")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@GetMapping("/obtenercorreo")
	public ResponseEntity<Object> obtenerUsuarioPorCorreo(@RequestParam String correo) {
		UsuarioDTO usuario = usuarioService.obtenerUsuarioPorCorreo(correo.trim().toLowerCase());

		if (usuario == null) {
			return mensaje(HttpStatus.NOT_FOUND, USUARIO_NO_ENCONTRADO);
		}

		return ResponseEntity.ok(usuario);
	}

	/**
	 * Arma una respuesta con el formato de la API.
	 *
	 * @param status estado HTTP
	 * @param texto mensaje para el usuario
	 * @return respuesta {@code {"message": texto}}
	 */
	private static ResponseEntity<Object> mensaje(HttpStatus status, String texto) {
		return RespuestaUtil.mensaje(status, texto);
	}
}
