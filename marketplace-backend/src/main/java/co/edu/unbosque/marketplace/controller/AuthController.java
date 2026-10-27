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

import co.edu.unbosque.marketplace.dto.InicioGoogleDTO;
import co.edu.unbosque.marketplace.dto.LoginDTO;
import co.edu.unbosque.marketplace.dto.RestablecerContraseniaDTO;
import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.dto.VerificacionCorreoDTO;
import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.security.JwtUtil;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import co.edu.unbosque.marketplace.service.AuthService;
import co.edu.unbosque.marketplace.service.InicioGoogleService;
import co.edu.unbosque.marketplace.service.SegundoFactorService;
import co.edu.unbosque.marketplace.service.UsuarioService;
import co.edu.unbosque.marketplace.service.VerificacionCorreoService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de autenticación del marketplace. Ruta base: {@code /auth}.
 * Expone el registro con verificación del correo por código, el inicio de
 * sesión (devuelve un token JWT listo para usar), la recuperación de
 * contraseña simulada, la reactivación de una cuenta desactivada y el inicio
 * de sesión con Google. Todas sus
 * rutas son públicas.
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

	/** Verificación del correo con código al registrarse. */
	private final VerificacionCorreoService verificacionCorreoService;

	/** Inicio de sesión y registro con una cuenta de Google. */
	private final InicioGoogleService inicioGoogleService;

	/**
	 * Constructor con inyección de dependencias.
	 *
	 * @param authenticationManager     gestor de autenticación de Spring Security
	 * @param jwtUtil                   utilidad para generar tokens JWT
	 * @param usuarioService            servicio de usuarios
	 * @param userDetailsService        servicio que carga usuarios por correo
	 * @param authService               servicio de recuperación y reactivación
	 * @param auditoriaService          servicio de auditoría
	 * @param segundoFactorService      servicio del segundo factor
	 * @param verificacionCorreoService servicio de verificación del correo
	 * @param inicioGoogleService       servicio de inicio de sesión con Google
	 */
	public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
			UsuarioService usuarioService, UserDetailsService userDetailsService, AuthService authService,
			AuditoriaService auditoriaService, SegundoFactorService segundoFactorService,
			VerificacionCorreoService verificacionCorreoService, InicioGoogleService inicioGoogleService) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.usuarioService = usuarioService;
		this.userDetailsService = userDetailsService;
		this.authService = authService;
		this.auditoriaService = auditoriaService;
		this.segundoFactorService = segundoFactorService;
		this.verificacionCorreoService = verificacionCorreoService;
		this.inicioGoogleService = inicioGoogleService;
	}

	/**
	 * Inicia sesión. Ruta: {@code POST /auth/login}.
	 *
	 * @param loginRequest DTO con {@code correo} y {@code contrasenia}
	 * @return 200 con un JSON {token, rol, correo}; 400 si faltan
	 *         datos; 401 si las credenciales son inválidas; 403 si la cuenta está
	 *         restringida o desactivada, o si el correo no está verificado
	 *         (con {@code requiereVerificacion=true})
	 */
	@Operation(summary = "Iniciar sesión", description = "Devuelve {token, rol, correo}. Si la cuenta tiene "
			+ "segundo factor, también hay que enviar codigo; sin él responde 401 con requiereSegundoFactor=true. "
			+ "Si el correo no está verificado responde 403 con requiereVerificacion=true.")
	@ApiResponse(responseCode = "200", description = "Sesión iniciada: {token, rol, correo}")
	@ApiResponse(responseCode = "400", description = "Debes enviar el correo y la contraseña.")
	@ApiResponse(responseCode = "401", description = "Correo o contraseña inválidos. | Ingresa el código de tu "
	        + "aplicación autenticadora. | El código de verificación no es válido.")
	@ApiResponse(responseCode = "403", description = "Debes verificar tu correo con el código que te enviamos antes de "
	        + "iniciar sesión. | La cuenta está restringida o desactivada.")
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

			// Correo sin verificar (RF-001): la contraseña es correcta pero aún no se entrega el token
			if (usuario instanceof Usuario u && !u.esCorreoVerificado()) {
				auditoriaService.registrar(idUsuario, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO,
						idUsuario, ResultadoAuditoria.RECHAZADO, "Correo sin verificar");
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
						"Debes verificar tu correo con el código que te enviamos antes de iniciar sesión.",
						"requiereVerificacion", true));
			}

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
	 * Inicia sesión con Google. Ruta: {@code POST /auth/google}. Si el correo
	 * no tiene cuenta, la crea ya verificada con el rol pedido.
	 *
	 * @param datos DTO con {@code idToken} (el credential del botón de Google) y,
	 *              opcionalmente, {@code rol} y {@code codigo}
	 * @return 200 con {message, token, rol, correo, cuentaNueva=false}; 201 igual
	 *         con cuentaNueva=true si se creó la cuenta; 401 si el token no es
	 *         válido o falta el código del segundo factor (con
	 *         requiereSegundoFactor=true); 403 si la cuenta está restringida o se
	 *         pide el rol ADMIN; 503 si no está configurado GOOGLE_CLIENT_ID
	 */
	@Operation(summary = "Iniciar sesión o registrarse con Google",
			description = "Recibe el credential (ID token) del botón de Google. Si el correo ya tiene cuenta inicia "
					+ "sesión; si no, la crea verificada con rol HUESPED o el rol enviado (ANFITRION). Devuelve el "
					+ "token igual que /auth/login, más cuentaNueva.")
	@ApiResponse(responseCode = "200", description = "Sesión iniciada con Google: {token, rol, correo, "
	        + "cuentaNueva=false}")
	@ApiResponse(responseCode = "201", description = "Cuenta creada con Google: {token, rol, correo, cuentaNueva=true}")
	@ApiResponse(responseCode = "400", description = "Debes enviar el idToken que entrega el botón de Google.")
	@ApiResponse(responseCode = "401", description = "No se pudo verificar tu cuenta de Google. Intenta de nuevo. | "
	        + "Ingresa el código de tu aplicación autenticadora. | El código de verificación no es válido.")
	@ApiResponse(responseCode = "403", description = "La cuenta está restringida. | Solo puedes registrarte como "
	        + "HUESPED o ANFITRION.")
	@ApiResponse(responseCode = "503", description = "El inicio de sesión con Google no está configurado.")
	@PostMapping("/google")
	public ResponseEntity<Object> google(@RequestBody InicioGoogleDTO datos) {
		if (datos.getIdToken() == null || datos.getIdToken().isBlank()) {
			return mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el idToken que entrega el botón de Google.");
		}
		InicioGoogleService.Inicio inicio = inicioGoogleService.iniciar(datos.getIdToken(), datos.getRol(),
				datos.getCodigo());
		return switch (inicio.codigo()) {
		case InicioGoogleService.INICIADA -> {
			Map<String, Object> respuesta = new LinkedHashMap<>(cuerpoConToken(inicio.usuario(),
					inicio.cuentaNueva() ? "Cuenta creada con Google." : "Sesión iniciada con Google."));
			respuesta.put("cuentaNueva", inicio.cuentaNueva());
			yield ResponseEntity.status(inicio.cuentaNueva() ? HttpStatus.CREATED : HttpStatus.OK).body(respuesta);
		}
		case InicioGoogleService.NO_CONFIGURADO ->
			mensaje(HttpStatus.SERVICE_UNAVAILABLE, "El inicio de sesión con Google no está configurado.");
		case InicioGoogleService.CUENTA_RESTRINGIDA -> mensaje(HttpStatus.FORBIDDEN, "La cuenta está restringida.");
		case InicioGoogleService.ROL_NO_PERMITIDO ->
			mensaje(HttpStatus.FORBIDDEN, "Solo puedes registrarte como HUESPED o ANFITRION.");
		case InicioGoogleService.REQUIERE_SEGUNDO_FACTOR -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
				"message", "Ingresa el código de tu aplicación autenticadora.", "requiereSegundoFactor", true));
		case InicioGoogleService.SEGUNDO_FACTOR_INVALIDO -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
				"message", "El código de verificación no es válido.", "requiereSegundoFactor", true));
		default -> mensaje(HttpStatus.UNAUTHORIZED, "No se pudo verificar tu cuenta de Google. Intenta de nuevo.");
		};
	}

	/**
	 * Registra un usuario nuevo. Ruta: {@code POST /auth/register}. La cuenta
	 * queda activa pero con el correo sin verificar: se envía un código de 6
	 * dígitos al correo y el token se entrega al confirmarlo con
	 * {@code POST /auth/verificar}. El rol permitido (HUESPED por defecto, o
	 * ANFITRION) lo valida {@link UsuarioService#create}; nunca se acepta ADMIN
	 * por esta vía.
	 *
	 * @param registerRequest DTO con nombre, correo, contrasenia, y opcionalmente
	 *                        telefono y rol
	 * @return 201 con {message, correo, requiereVerificacion}; 409 si el correo
	 *         ya existe; 400 si los datos o la contraseña son inválidos; 403 si
	 *         se pide un rol no permitido; 500 si ocurre un error inesperado
	 */
	@Operation(summary = "Registrarse como HUESPED o ANFITRION",
			description = "Crea la cuenta y envía un código de verificación al correo. Devuelve "
					+ "{message, correo, requiereVerificacion}; el token se obtiene en /auth/verificar.")
	@ApiResponse(responseCode = "201", description = "Usuario registrado: {message, correo, "
	        + "requiereVerificacion=true}; se envió el código")
	@ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos: nombre, correo válido y "
	        + "contraseña son obligatorios; el teléfono, si se envía, debe tener de 7 a 15 dígitos. | La contraseña "
	        + "debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo.")
	@ApiResponse(responseCode = "403", description = "Solo puedes registrarte como HUESPED o ANFITRION.")
	@ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese correo electrónico.")
	@ApiResponse(responseCode = "500", description = "Error desconocido al registrar el usuario.")
	@PostMapping("/register")
	public ResponseEntity<Object> register(@RequestBody UsuarioDTO registerRequest) {
		return switch (usuarioService.create(registerRequest)) {
		case UsuarioService.CREADO -> {
			Map<String, Object> respuesta = new LinkedHashMap<>();
			respuesta.put("message", "Usuario registrado. Te enviamos un código de verificación al correo; "
					+ "vence en " + VerificacionCorreoService.MINUTOS_VIGENCIA + " minutos.");
			respuesta.put("correo", UsuarioService.normalizarCorreo(registerRequest.getCorreo()));
			respuesta.put("requiereVerificacion", true);
			yield ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
		}
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
	 * Confirma el correo con el código de verificación e inicia la sesión.
	 * Ruta: {@code POST /auth/verificar}.
	 *
	 * @param datos DTO con {@code correo} y {@code codigo}
	 * @return 200 con {message, token, rol, correo}; 400 si faltan datos o el
	 *         código no es válido o venció; 409 si el correo ya estaba
	 *         verificado; 429 si se agotaron los intentos del código
	 */
	@Operation(summary = "Verificar el correo con el código recibido",
			description = "El código vence a los 15 minutos y admite 5 intentos. Devuelve el token de sesión.")
	@ApiResponse(responseCode = "200", description = "Correo verificado: {message, token, rol, correo}")
	@ApiResponse(responseCode = "400", description = "El código no es válido o ya venció. Revísalo o solicita uno "
	        + "nuevo. | Debes enviar el correo y el código.")
	@ApiResponse(responseCode = "409", description = "El correo ya está verificado. Inicia sesión.")
	@ApiResponse(responseCode = "429", description = "Superaste los intentos permitidos para este código. Solicita uno "
	        + "nuevo.")
	@PostMapping("/verificar")
	public ResponseEntity<Object> verificar(@RequestBody VerificacionCorreoDTO datos) {
		return switch (verificacionCorreoService.verificar(datos.getCorreo(), datos.getCodigo())) {
		case VerificacionCorreoService.VERIFICADO -> respuestaVerificada(
				userDetailsService.loadUserByUsername(UsuarioService.normalizarCorreo(datos.getCorreo())));
		case VerificacionCorreoService.CODIGO_INVALIDO -> mensaje(HttpStatus.BAD_REQUEST,
				"El código no es válido o ya venció. Revísalo o solicita uno nuevo.");
		case VerificacionCorreoService.INTENTOS_AGOTADOS -> mensaje(HttpStatus.TOO_MANY_REQUESTS,
				"Superaste los intentos permitidos para este código. Solicita uno nuevo.");
		case VerificacionCorreoService.YA_VERIFICADO -> mensaje(HttpStatus.CONFLICT,
				"El correo ya está verificado. Inicia sesión.");
		default -> mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo y el código.");
		};
	}

	/**
	 * Envía un código de verificación nuevo. Ruta: {@code POST /auth/reenviarcodigo}.
	 *
	 * @param solicitud DTO con {@code correo}
	 * @return 200 siempre que se envíe un correo (exista o no la cuenta, para no
	 *         revelarlo); 400 si falta el correo; 429 si el último código se
	 *         pidió hace menos de un minuto
	 */
	@Operation(summary = "Reenviar el código de verificación del correo",
			description = "Invalida el código anterior. Solo se permite una vez por minuto.")
	@ApiResponse(responseCode = "200", description = "Si la cuenta existe y no está verificada, te enviamos un código "
	        + "nuevo.")
	@ApiResponse(responseCode = "400", description = "Debes enviar el correo.")
	@ApiResponse(responseCode = "429", description = "Espera un minuto antes de pedir otro código.")
	@PostMapping("/reenviarcodigo")
	public ResponseEntity<Object> reenviarCodigo(@RequestBody UsuarioDTO solicitud) {
		return switch (verificacionCorreoService.reenviar(solicitud.getCorreo())) {
		case VerificacionCorreoService.ENVIADO -> mensaje(HttpStatus.OK,
				"Si la cuenta existe y no está verificada, te enviamos un código nuevo.");
		case VerificacionCorreoService.ESPERAR -> mensaje(HttpStatus.TOO_MANY_REQUESTS,
				"Espera un minuto antes de pedir otro código.");
		default -> mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo.");
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
	@ApiResponse(responseCode = "200", description = "Si el correo está registrado, se generó un enlace de "
	        + "recuperación válido por 30 minutos.")
	@ApiResponse(responseCode = "400", description = "Debes enviar el correo.")
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
	@ApiResponse(responseCode = "200", description = "Contraseña actualizada. Ya puedes iniciar sesión.")
	@ApiResponse(responseCode = "400", description = "El enlace de recuperación no es válido o ya venció. | La "
	        + "contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo. | "
	        + "Debes enviar el token y la contraseña nueva.")
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
	@ApiResponse(responseCode = "200", description = "Cuenta reactivada: {message, token, rol, correo, estado}")
	@ApiResponse(responseCode = "400", description = "Debes enviar el correo y la contraseña.")
	@ApiResponse(responseCode = "401", description = "Correo o contraseña inválidos.")
	@ApiResponse(responseCode = "403", description = "La cuenta fue restringida por un administrador y no puede "
	        + "reactivarse.")
	@ApiResponse(responseCode = "409", description = "La cuenta ya está activa.")
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
	 * Respuesta de un correo recién verificado. Con segundo factor activo no se
	 * entrega token aquí: debe iniciar sesión con su código.
	 */
	private ResponseEntity<Object> respuestaVerificada(UserDetails usuario) {
		if (usuario instanceof Usuario u && SegundoFactorService.estaHabilitado(u)) {
			return mensaje(HttpStatus.OK, "Correo verificado. Inicia sesión con tu código.");
		}
		return respuestaConToken(HttpStatus.OK, usuario, "Correo verificado correctamente.");
	}

	/**
	 * Respuesta de sesión iniciada: {message?, token, rol, correo}. La usan
	 * login, verificación y reactivación para que el front guarde el token igual.
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
