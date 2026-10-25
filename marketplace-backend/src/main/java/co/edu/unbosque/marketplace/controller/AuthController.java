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

import co.edu.unbosque.marketplace.dto.LoginDTO;
import co.edu.unbosque.marketplace.dto.RestablecerContraseniaDTO;
import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.security.JwtUtil;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import co.edu.unbosque.marketplace.service.AuthService;
import co.edu.unbosque.marketplace.service.SegundoFactorService;
import co.edu.unbosque.marketplace.service.UsuarioService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de autenticación del marketplace. Ruta base: {@code /auth}.
 * Expone el inicio de sesión y el registro de usuarios (ambos devuelven un
 * token JWT listo para usar), la recuperación de contraseña simulada y la
 * reactivación de una cuenta desactivada. Todas sus rutas son públicas.
 *
 * @see "RF-001"
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión, recuperación y reactivación de cuenta")
public class AuthController {

	private static final String MENSAJE_CONTRASENIA_DEBIL = "La contraseña debe tener al menos 8 caracteres, "
			+ "una mayúscula, una minúscula, un número y un símbolo.";

	/** Valida las credenciales contra la base de datos (BCrypt). */
	private final AuthenticationManager authenticationManager;

	/** Genera el token JWT del usuario autenticado. */
	private final JwtUtil jwtUtil;

	/** Reglas de negocio de usuarios (registro, normalización del correo, rol). */
	private final UsuarioService usuarioService;

	/** Carga el usuario recién registrado para emitir su token. */
	private final UserDetailsService userDetailsService;

	/** Recuperación de contraseña y reactivación de cuenta. */
	private final AuthService authService;

	/** Registro de auditoría de los inicios de sesión. */
	private final AuditoriaService auditoriaService;

	/** Verificación del segundo factor (TOTP). */
	private final SegundoFactorService segundoFactorService;

	/**
	 * Constructor con inyección de dependencias.
	 *
	 * @param authenticationManager gestor de autenticación de Spring Security
	 * @param jwtUtil               utilidad para generar tokens JWT
	 * @param usuarioService        servicio de usuarios
	 * @param userDetailsService    servicio que carga usuarios por correo
	 * @param authService           servicio de recuperación y reactivación
	 * @param auditoriaService      servicio de auditoría
	 * @param segundoFactorService  servicio del segundo factor
	 */
	public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
			UsuarioService usuarioService, UserDetailsService userDetailsService, AuthService authService,
			AuditoriaService auditoriaService, SegundoFactorService segundoFactorService) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.usuarioService = usuarioService;
		this.userDetailsService = userDetailsService;
		this.authService = authService;
		this.auditoriaService = auditoriaService;
		this.segundoFactorService = segundoFactorService;
	}

	/**
	 * Inicia sesión. Ruta: {@code POST /auth/login}.
	 *
	 * @param loginRequest DTO con {@code correo} y {@code contrasenia}
	 * @return 200 con un JSON {token, rol, correo}; 400 si faltan
	 *         datos; 401 si las credenciales son inválidas; 403 si la cuenta está
	 *         restringida o desactivada
	 */
	@Operation(summary = "Iniciar sesión", description = "Devuelve {token, rol, correo}. Si la cuenta tiene "
			+ "segundo factor, también hay que enviar codigo; sin él responde 401 con requiereSegundoFactor=true.")
	@PostMapping("/login")
	public ResponseEntity<Object> login(@RequestBody LoginDTO loginRequest) {
		if (loginRequest.getCorreo() == null || loginRequest.getCorreo().isBlank()
				|| loginRequest.getContrasenia() == null || loginRequest.getContrasenia().isBlank()) {
			return mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo y la contraseña.");
		}
		try {
			String correo = UsuarioService.normalizarCorreo(loginRequest.getCorreo());
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(correo, loginRequest.getContrasenia()));

			UserDetails usuario = (UserDetails) authentication.getPrincipal();
			Long idUsuario = usuario instanceof Usuario u ? u.getIdUsuario() : null;

			// Segundo factor (RNF-033): con las credenciales válidas aún no se entrega el token
			if (usuario instanceof Usuario u && SegundoFactorService.estaHabilitado(u)) {
				if (loginRequest.getCodigo() == null || loginRequest.getCodigo().isBlank()) {
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message",
							"Ingresa el código de tu aplicación autenticadora.", "requiereSegundoFactor", true));
				}
				if (!segundoFactorService.verificar(u, loginRequest.getCodigo())) {
					auditoriaService.registrar(idUsuario, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO,
							idUsuario, ResultadoAuditoria.RECHAZADO, "Código del segundo factor inválido");
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message",
							"El código de verificación no es válido.", "requiereSegundoFactor", true));
				}
			}

			auditoriaService.registrar(idUsuario, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO,
					idUsuario, ResultadoAuditoria.EXITOSO, null);
			return respuestaConToken(HttpStatus.OK, usuario, null);
		} catch (LockedException | DisabledException e) {
			auditoriaService.registrar(null, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, null,
					ResultadoAuditoria.RECHAZADO, "Cuenta restringida o desactivada");
			return mensaje(HttpStatus.FORBIDDEN, "La cuenta está restringida o desactivada.");
		} catch (AuthenticationException e) {
			auditoriaService.registrar(null, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, null,
					ResultadoAuditoria.RECHAZADO, "Credenciales inválidas");
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
	@Operation(summary = "Registrarse como HUESPED o ANFITRION",
			description = "Crea la cuenta activa y devuelve {message, token, rol, correo}.")
	@PostMapping("/register")
	public ResponseEntity<Object> register(@RequestBody UsuarioDTO registerRequest) {
		return switch (usuarioService.create(registerRequest)) {
		case UsuarioService.CREADO -> respuestaConToken(HttpStatus.CREATED,
				userDetailsService.loadUserByUsername(UsuarioService.normalizarCorreo(registerRequest.getCorreo())),
				"Usuario registrado correctamente.");
		case UsuarioService.CORREO_DUPLICADO ->
			mensaje(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo electrónico.");
		case UsuarioService.DATOS_INVALIDOS -> mensaje(HttpStatus.BAD_REQUEST, "Datos incompletos o inválidos: "
				+ "nombre, correo válido y contraseña son obligatorios; el teléfono, si se envía, debe tener "
				+ "de 7 a 15 dígitos.");
		case UsuarioService.CONTRASENIA_DEBIL -> mensaje(HttpStatus.BAD_REQUEST, MENSAJE_CONTRASENIA_DEBIL);
		case UsuarioService.ROL_NO_PERMITIDO ->
			mensaje(HttpStatus.FORBIDDEN, "Solo puedes registrarte como HUESPED o ANFITRION.");
		default -> mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "Error desconocido al registrar el usuario.");
		};
	}

	/**
	 * Solicita la recuperación de contraseña. Ruta: {@code POST /auth/recuperar}.
	 * SIMULACIÓN: el token no se envía por correo, se escribe en el log del
	 * backend y vence a los 30 minutos.
	 *
	 * @param solicitud DTO con {@code correo}
	 * @return 200 siempre que se envíe un correo (exista o no, para no revelar
	 *         qué cuentas están registradas); 400 si falta el correo
	 */
	@Operation(summary = "Solicitar recuperación de contraseña (simulada)",
			description = "El token se escribe en el log del backend en lugar de enviarse por correo.")
	@PostMapping("/recuperar")
	public ResponseEntity<Object> recuperar(@RequestBody UsuarioDTO solicitud) {
		if (authService.solicitarRecuperacion(solicitud.getCorreo()) != 0) {
			return mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo.");
		}
		return mensaje(HttpStatus.OK,
				"Si el correo está registrado, se generó un enlace de recuperación válido por 30 minutos.");
	}

	/**
	 * Cambia la contraseña con un token de recuperación. Ruta:
	 * {@code POST /auth/restablecer}.
	 *
	 * @param datos DTO con {@code token} y {@code contrasenia} nueva
	 * @return 200 si se cambió; 400 si faltan datos, la contraseña es débil o el
	 *         token no sirve
	 */
	@Operation(summary = "Restablecer la contraseña con el token de recuperación")
	@PostMapping("/restablecer")
	public ResponseEntity<Object> restablecer(@RequestBody RestablecerContraseniaDTO datos) {
		return switch (authService.restablecerContrasenia(datos.getToken(), datos.getContrasenia())) {
		case 0 -> mensaje(HttpStatus.OK, "Contraseña actualizada. Ya puedes iniciar sesión.");
		case 1 -> mensaje(HttpStatus.BAD_REQUEST, "El enlace de recuperación no es válido o ya venció.");
		case 3 -> mensaje(HttpStatus.BAD_REQUEST, MENSAJE_CONTRASENIA_DEBIL);
		default -> mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el token y la contraseña nueva.");
		};
	}

	/**
	 * Reactiva una cuenta desactivada por su dueño e inicia sesión. Ruta:
	 * {@code POST /auth/reactivar}.
	 *
	 * @param credenciales DTO con {@code correo} y {@code contrasenia}
	 * @return 200 con {message, token, rol, correo}; 400 si faltan datos; 401 si
	 *         las credenciales son inválidas; 403 si un administrador restringió
	 *         la cuenta; 409 si la cuenta ya estaba activa
	 */
	@Operation(summary = "Reactivar la cuenta propia",
			description = "Una cuenta restringida por un administrador no puede reactivarse por esta vía.")
	@PostMapping("/reactivar")
	public ResponseEntity<Object> reactivar(@RequestBody UsuarioDTO credenciales) {
		return switch (authService.reactivarCuenta(credenciales.getCorreo(), credenciales.getContrasenia())) {
		case 0 -> respuestaReactivada(userDetailsService
				.loadUserByUsername(UsuarioService.normalizarCorreo(credenciales.getCorreo())));
		case 1 -> mensaje(HttpStatus.UNAUTHORIZED, "Correo o contraseña inválidos.");
		case 3 -> mensaje(HttpStatus.FORBIDDEN,
				"La cuenta fue restringida por un administrador y no puede reactivarse.");
		case 4 -> mensaje(HttpStatus.CONFLICT, "La cuenta ya está activa.");
		default -> mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo y la contraseña.");
		};
	}

	/**
	 * Respuesta de una cuenta reactivada con su nuevo estado (CA-052). Con
	 * segundo factor activo no se entrega token aquí: debe iniciar sesión con su
	 * código.
	 */
	private ResponseEntity<Object> respuestaReactivada(UserDetails usuario) {
		if (usuario instanceof Usuario u && SegundoFactorService.estaHabilitado(u)) {
			return ResponseEntity.ok(Map.of("message", "Cuenta reactivada correctamente. Inicia sesión con tu código.",
					"estado", EstadoUsuario.ACTIVO.name()));
		}
		Map<String, String> respuesta = cuerpoConToken(usuario, "Cuenta reactivada correctamente.");
		respuesta.put("estado", EstadoUsuario.ACTIVO.name());
		return ResponseEntity.ok(respuesta);
	}

	/**
	 * Respuesta de sesión iniciada: {message?, token, rol, correo}. La usan
	 * login, registro y reactivación para que el front guarde el token igual.
	 */
	private ResponseEntity<Object> respuestaConToken(HttpStatus status, UserDetails userDetails,
			String texto) {
		return ResponseEntity.status(status).body(cuerpoConToken(userDetails, texto));
	}

	private Map<String, String> cuerpoConToken(UserDetails userDetails, String texto) {
		String rol = userDetails.getAuthorities().stream().findFirst()
				.map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).orElse(null);

		Map<String, String> respuesta = new LinkedHashMap<>();
		if (texto != null) {
			respuesta.put("message", texto);
		}
		respuesta.put("token", jwtUtil.generateToken(userDetails));
		respuesta.put("rol", rol);
		respuesta.put("correo", userDetails.getUsername());
		return respuesta;
	}

	/** Respuesta JSON uniforme: {"message": "..."} para que el front la lea siempre igual. */
	private static ResponseEntity<Object> mensaje(HttpStatus status, String texto) {
		return RespuestaUtil.mensaje(status, texto);
	}
}
