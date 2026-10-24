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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.security.JwtUtil;
import co.edu.unbosque.marketplace.service.UsuarioService;

/**
 * Controlador REST de autenticación del marketplace. Ruta base: {@code /auth}.
 * Expone el inicio de sesión y el registro de usuarios; ambos devuelven un
 * token JWT listo para usar.
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

	/** Carga el usuario recién registrado para emitir su token. */
	private final UserDetailsService userDetailsService;

	/**
	 * Constructor con inyección de dependencias.
	 *
	 * @param authenticationManager gestor de autenticación de Spring Security
	 * @param jwtUtil               utilidad para generar tokens JWT
	 * @param usuarioService        servicio de usuarios
	 * @param userDetailsService    servicio que carga usuarios por correo
	 */
	public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
			UsuarioService usuarioService, UserDetailsService userDetailsService) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.usuarioService = usuarioService;
		this.userDetailsService = userDetailsService;
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
			String correo = UsuarioService.normalizarCorreo(loginRequest.getCorreo());
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(correo, loginRequest.getContrasenia()));

			return respuestaConToken(HttpStatus.OK, (UserDetails) authentication.getPrincipal(), null);
		} catch (LockedException | DisabledException e) {
			return mensaje(HttpStatus.FORBIDDEN, "La cuenta está restringida o desactivada.");
		} catch (AuthenticationException e) {
			// Mensaje genérico: no revela si el correo existe ni el detalle técnico del error
			return mensaje(HttpStatus.UNAUTHORIZED, "Correo o contraseña inválidos.");
		}
	}

	/**
	 * Registra un usuario nuevo e inicia su sesión. Ruta: {@code POST /auth/register}.
	 * La cuenta queda activa de inmediato y la respuesta ya trae el token JWT,
	 * así el front no necesita llamar a {@code /auth/login} después. El rol
	 * permitido (HUESPED por defecto, o ANFITRION) lo valida
	 * {@link UsuarioService#create}; nunca se acepta ADMIN por esta vía.
	 *
	 * @param registerRequest DTO con nombre, correo, contrasenia, y opcionalmente
	 *                        telefono y rol
	 * @return 201 con {message, token, rol, correo}; 409 si el correo ya existe;
	 *         400 si los datos o la contraseña son inválidos; 403 si se pide un
	 *         rol no permitido; 500 si ocurre un error inesperado
	 */
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody UsuarioDTO registerRequest) {
		int result = usuarioService.create(registerRequest);

		switch (result) {
		case UsuarioService.CREADO:
			UserDetails usuario = userDetailsService
					.loadUserByUsername(UsuarioService.normalizarCorreo(registerRequest.getCorreo()));
			return respuestaConToken(HttpStatus.CREATED, usuario, "Usuario registrado correctamente.");
		case UsuarioService.CORREO_DUPLICADO:
			return mensaje(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo electrónico.");
		case UsuarioService.DATOS_INVALIDOS:
			return mensaje(HttpStatus.BAD_REQUEST,
					"Datos incompletos o inválidos: nombre, correo válido y contraseña son obligatorios.");
		case UsuarioService.CONTRASENIA_DEBIL:
			return mensaje(HttpStatus.BAD_REQUEST,
					"La contraseña debe tener al menos 8 caracteres, un número y un símbolo.");
		case UsuarioService.ROL_NO_PERMITIDO:
			return mensaje(HttpStatus.FORBIDDEN, "Solo puedes registrarte como HUESPED o ANFITRION.");
		default:
			return mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "Error desconocido al registrar el usuario.");
		}
	}

	/**
	 * Respuesta de sesión iniciada: {message?, token, rol, correo}. La usan
	 * login y registro para que el front guarde el token igual en ambos casos.
	 */
	private ResponseEntity<Map<String, String>> respuestaConToken(HttpStatus status, UserDetails userDetails,
			String texto) {
		String rol = userDetails.getAuthorities().stream().findFirst()
				.map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).orElse(null);

		Map<String, String> respuesta = new LinkedHashMap<>();
		if (texto != null) {
			respuesta.put("message", texto);
		}
		respuesta.put("token", jwtUtil.generateToken(userDetails));
		respuesta.put("rol", rol);
		respuesta.put("correo", userDetails.getUsername());
		return ResponseEntity.status(status).body(respuesta);
	}

	/** Respuesta JSON uniforme: {"message": "..."} para que el front la lea siempre igual. */
	private ResponseEntity<Map<String, String>> mensaje(HttpStatus status, String texto) {
		return ResponseEntity.status(status).body(Map.of("message", texto));
	}
}
