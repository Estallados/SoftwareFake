package co.edu.unbosque.marketplace.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.security.JwtUtil;
import co.edu.unbosque.marketplace.service.UsuarioService;

/**
 * Controlador REST de autenticación del marketplace. Ruta base: {@code /auth}.
 * Expone el inicio de sesión (genera un token JWT) y el registro de usuarios.
 * El CORS se configura de forma global en SecurityConfig, por eso no se usa
 * {@code @CrossOrigin} aquí.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

	/** Valida las credenciales contra la base de datos (BCrypt). */
	private final AuthenticationManager authenticationManager;

	/** Genera el token JWT del usuario autenticado. */
	private final JwtUtil jwtUtil;

	/** Reglas de negocio de usuarios (registro, normalización del correo, rol). */
	private final UsuarioService usuarioService;

	/**
	 * Constructor con inyección de dependencias.
	 *
	 * @param authenticationManager gestor de autenticación de Spring Security
	 * @param jwtUtil               utilidad para generar tokens JWT
	 * @param usuarioService        servicio de usuarios
	 */
	public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
			UsuarioService usuarioService) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.usuarioService = usuarioService;
	}

	/**
	 * Inicia sesión. Ruta: {@code POST /auth/login}.
	 *
	 * @param loginRequest DTO con {@code correo} y {@code contrasenia}
	 * @return 200 con un JSON {token, rol, correo}; 400 si faltan
	 *         datos; 401 si las credenciales son inválidas; 403 si la cuenta está
	 *         restringida o desactivada
	 */
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody UsuarioDTO loginRequest) {
		if (loginRequest.getCorreo() == null || loginRequest.getCorreo().isBlank()
				|| loginRequest.getContrasenia() == null || loginRequest.getContrasenia().isBlank()) {
			return mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo y la contraseña.");
		}
		try {
			String correo = loginRequest.getCorreo().trim().toLowerCase();
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(correo, loginRequest.getContrasenia()));

			UserDetails userDetails = (UserDetails) authentication.getPrincipal();
			String jwt = jwtUtil.generateToken(userDetails);
			String rol = userDetails.getAuthorities().stream().findFirst()
					.map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).orElse(null);

			Map<String, String> respuesta = new LinkedHashMap<>();
			respuesta.put("token", jwt);
			respuesta.put("rol", rol);
			respuesta.put("correo", userDetails.getUsername());
			return ResponseEntity.ok(respuesta);
		} catch (LockedException | DisabledException e) {
			return mensaje(HttpStatus.FORBIDDEN, "La cuenta está restringida o desactivada.");
		} catch (AuthenticationException e) {
			// Mensaje genérico: no revela si el correo existe ni el detalle técnico del error
			return mensaje(HttpStatus.UNAUTHORIZED, "Correo o contraseña inválidos.");
		}
	}

	/**
	 * Registra un usuario nuevo. Ruta: {@code POST /auth/register}. La cuenta queda
	 * activa de inmediato (ya no hay código de verificación por correo). El rol
	 * permitido (HUESPED o ANFITRION) lo valida {@link UsuarioService#create}; nunca
	 * se acepta ADMINISTRADOR por esta vía.
	 *
	 * @param registerRequest DTO con los datos del nuevo usuario
	 * @return 201 si se creó; 409 si el correo ya existe; 400 si los datos son
	 *         inválidos; 500 si ocurre un error inesperado
	 */
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody UsuarioDTO registerRequest) {
		int result = usuarioService.create(registerRequest);

		switch (result) {
		case 0:
			return mensaje(HttpStatus.CREATED, "Usuario registrado correctamente. Ya puedes iniciar sesión.");
		case 1:
			return mensaje(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo electrónico.");
		case 2:
			return mensaje(HttpStatus.BAD_REQUEST, "Datos incompletos o inválidos para registrar el usuario.");
		default:
			return mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "Error desconocido al registrar el usuario.");
		}
	}

	/** Respuesta JSON uniforme: {"message": "..."} para que el front la lea siempre igual. */
	private ResponseEntity<Map<String, String>> mensaje(HttpStatus status, String texto) {
		return ResponseEntity.status(status).body(Map.of("message", texto));
	}
}