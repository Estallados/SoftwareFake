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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
import co.edu.unbosque.marketplace.entity.Role;
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
 * Controlador REST de autenticación. Ruta base: {@code /auth}. Expone el
 * registro con verificación del correo por código, el inicio de sesión (que
 * devuelve un token JWT listo para usar), la recuperación de contraseña por
 * correo, la reactivación de una cuenta desactivada y el inicio de sesión con
 * Google. Todas sus rutas son públicas.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión, recuperación y reactivación de cuenta")
@CrossOrigin(origins = "*")
public class AuthController {

	/** Mensaje cuando la contraseña no cumple las reglas. */
	private static final String MENSAJE_CONTRASENIA_DEBIL = "La contraseña debe tener entre 8 y 64 caracteres, "
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
	public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UsuarioService usuarioService,
			UserDetailsService userDetailsService, AuthService authService, AuditoriaService auditoriaService,
			SegundoFactorService segundoFactorService, VerificacionCorreoService verificacionCorreoService,
			InicioGoogleService inicioGoogleService) {
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
	 * @return 200 con un JSON {token, rol, correo}; 400 si faltan datos; 401 si las
	 *         credenciales son inválidas; 403 si la cuenta está restringida o
	 *         desactivada, o si el correo no está verificado (con
	 *         {@code requiereVerificacion=true})
	 */
	@Operation(summary = "Iniciar sesión", description = "Devuelve {token, rol, correo}. Si la cuenta tiene "
			+ "segundo factor, también hay que enviar codigo; sin él responde 401 con requiereSegundoFactor=true. "
			+ "Si no, en cada inicio de sesión se envía un código al correo: sin codigo responde 401 con "
			+ "requiereCodigoCorreo=true y hay que repetir la petición con ese codigo. El administrador no "
			+ "recibe código por correo. "
			+ "Si el correo no está verificado responde 403 con requiereVerificacion=true.")
	@ApiResponse(responseCode = "200", description = "Sesión iniciada: {token, rol, correo}")
	@ApiResponse(responseCode = "400", description = "Debes enviar el correo y la contraseña.")
	@ApiResponse(responseCode = "401", description = "Correo o contraseña inválidos. | Ingresa el código de tu "
			+ "aplicación autenticadora. | El código de verificación no es válido. | Te enviamos un código de "
			+ "verificación a tu correo. | El código del correo no es válido o ya venció.")
	@ApiResponse(responseCode = "429", description = "Superaste los intentos permitidos para este código del correo.")
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

			// Correo sin verificar: la contraseña es correcta pero aún no se
			// entrega el token
			if (usuario instanceof Usuario u && !u.esCorreoVerificado()) {
				auditoriaService.registrar(idUsuario, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO,
						idUsuario, ResultadoAuditoria.RECHAZADO, "Correo sin verificar");
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("message",
								"Debes verificar tu correo con el código que te enviamos antes de iniciar sesión.",
								"requiereVerificacion", true));
			}

			// Segundo factor: con las credenciales válidas aún no se entrega el
			// token
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
			} else if (usuario instanceof Usuario u && u.getRol() != Role.ADMIN) {
				// Código por correo en cada inicio de sesión: sin él aún no se
				// entrega el token. El administrador no lo necesita.
				ResponseEntity<Object> pendiente = codigoInicioPorCorreo(u, correo, loginRequest.getCodigo());
				if (pendiente != null) {
					return pendiente;
				}
			}

			auditoriaService.registrar(idUsuario, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, idUsuario,
					ResultadoAuditoria.EXITOSO, null);
			return respuestaConToken(HttpStatus.OK, usuario, null);
		} catch (LockedException | DisabledException e) {
			auditoriaService.registrar(null, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, null,
					ResultadoAuditoria.RECHAZADO, "Cuenta restringida o desactivada");
			return mensaje(HttpStatus.FORBIDDEN, "La cuenta está restringida o desactivada.");
		} catch (AuthenticationException e) {
			auditoriaService.registrar(null, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, null,
					ResultadoAuditoria.RECHAZADO, "Credenciales inválidas");
			// Mensaje genérico: no revela si el correo existe ni el detalle técnico del
			// error
			return mensaje(HttpStatus.UNAUTHORIZED, "Correo o contraseña inválidos.");
		}
	}

	/**
	 * Paso del código por correo del inicio de sesión. Sin código, envía uno
	 * nuevo al correo; con código, lo comprueba.
	 *
	 * @param usuario cuenta con las credenciales ya validadas
	 * @param correo  correo normalizado de la cuenta
	 * @param codigo  código recibido en el correo, o {@code null}
	 * @return {@code null} si el código es correcto (se entrega el token); si
	 *         no, la respuesta 401/429 con {@code requiereCodigoCorreo=true}
	 */
	private ResponseEntity<Object> codigoInicioPorCorreo(Usuario usuario, String correo, String codigo) {
		Long idUsuario = usuario.getIdUsuario();
		if (codigo == null || codigo.isBlank()) {
			String texto = verificacionCorreoService.enviarCodigoInicio(usuario,
					correo) == VerificacionCorreoService.ESPERAR
							? "Ya te enviamos un código al correo hace menos de un minuto. Revisa tu bandeja de entrada."
							: "Te enviamos un código de verificación a tu correo; vence en "
									+ VerificacionCorreoService.MINUTOS_VIGENCIA + " minutos.";
			return respuestaCodigoCorreo(HttpStatus.UNAUTHORIZED, texto);
		}
		int resultado = verificacionCorreoService.verificarCodigoInicio(usuario, codigo);
		if (resultado == VerificacionCorreoService.VERIFICADO) {
			return null;
		}
		auditoriaService.registrar(idUsuario, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, idUsuario,
				ResultadoAuditoria.RECHAZADO, "Código de inicio de sesión inválido");
		return resultado == VerificacionCorreoService.INTENTOS_AGOTADOS
				? respuestaCodigoCorreo(HttpStatus.TOO_MANY_REQUESTS,
						"Superaste los intentos permitidos para este código del correo. Vuelve a iniciar sesión "
								+ "para recibir uno nuevo.")
				: respuestaCodigoCorreo(HttpStatus.UNAUTHORIZED,
						"El código del correo no es válido o ya venció. Revísalo o vuelve a iniciar sesión.");
	}

	/**
	 * Respuesta que pide el código enviado al correo para terminar de iniciar
	 * sesión.
	 *
	 * @param status estado HTTP
	 * @param texto  mensaje para el usuario
	 * @return {message, requiereSegundoFactor, requiereCodigoCorreo,
	 *         metodoSegundoFactor=CORREO}
	 */
	private static ResponseEntity<Object> respuestaCodigoCorreo(HttpStatus status, String texto) {
		Map<String, Object> respuesta = new LinkedHashMap<>();
		respuesta.put("message", texto);
		respuesta.put("requiereSegundoFactor", true);
		respuesta.put("requiereCodigoCorreo", true);
		respuesta.put("metodoSegundoFactor", "CORREO");
		return ResponseEntity.status(status).body(respuesta);
	}

	/**
	 * Configuración del botón de Google para el front. Ruta:
	 * {@code GET /auth/google/configuracion}. Así el client ID no se escribe a
	 * mano en Angular.
	 *
	 * @return 200 con {habilitado, clientId}; clientId es null si no está
	 *         configurado
	 */
	@Operation(summary = "Configuración del botón de Google", description = "Devuelve {habilitado, clientId}. El "
			+ "front inicializa Google Identity Services con ese clientId y envía el credential a POST /auth/google.")
	@ApiResponse(responseCode = "200", description = "{habilitado, clientId}")
	@GetMapping("/google/configuracion")
	public ResponseEntity<Object> configuracionGoogle() {
		String clientId = inicioGoogleService.clientId();
		Map<String, Object> respuesta = new LinkedHashMap<>();
		respuesta.put("habilitado", clientId != null);
		respuesta.put("clientId", clientId);
		return ResponseEntity.ok(respuesta);
	}

	/**
	 * Inicia sesión con Google. Si el correo no tiene cuenta, la crea ya
	 * verificada con el rol pedido. Ruta: {@code POST /auth/google}.
	 *
	 * @param datos DTO con {@code idToken} (el credential del botón de Google)
	 *              y, opcionalmente, {@code rol} y {@code codigo}
	 * @return 200 con {message, token, rol, correo, cuentaNueva=false}; 201
	 *         igual con cuentaNueva=true si se creó la cuenta; 401 si el token
	 *         no es válido o falta el código del segundo factor (con
	 *         requiereSegundoFactor=true); 403 si la cuenta está restringida o
	 *         se pide el rol ADMIN; 503 si no está configurado GOOGLE_CLIENT_ID
	 */
	@Operation(summary = "Iniciar sesión o registrarse con Google", description = "Recibe el credential (ID token) del botón de Google. Si el correo ya tiene cuenta inicia "
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
		case InicioGoogleService.REQUIERE_SEGUNDO_FACTOR -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
				Map.of("message", "Ingresa el código de tu aplicación autenticadora.", "requiereSegundoFactor", true));
		case InicioGoogleService.SEGUNDO_FACTOR_INVALIDO -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("message", "El código de verificación no es válido.", "requiereSegundoFactor", true));
		default -> mensaje(HttpStatus.UNAUTHORIZED, "No se pudo verificar tu cuenta de Google. Intenta de nuevo.");
		};
	}

	/**
	 * Registra un usuario nuevo. Ruta: {@code POST /auth/register}. La cuenta queda
	 * activa pero con el correo sin verificar: se envía un código de 6 dígitos al
	 * correo y el token se entrega al confirmarlo con {@code POST /auth/verificar}.
	 * El rol permitido (HUESPED por defecto, o ANFITRION) lo valida
	 * {@link UsuarioService#create}; nunca se acepta ADMIN por esta vía.
	 *
	 * @param registerRequest DTO con nombre, correo, contrasenia, y opcionalmente
	 *                        telefono y rol
	 * @return 201 con {message, correo, requiereVerificacion}; 409 si el correo ya
	 *         existe; 400 si los datos o la contraseña son inválidos; 403 si se
	 *         pide un rol no permitido; 500 si ocurre un error inesperado
	 */
	@Operation(summary = "Registrarse como HUESPED o ANFITRION", description = "Crea la cuenta y envía un código de verificación al correo. Devuelve "
			+ "{message, correo, requiereVerificacion}; el token se obtiene en /auth/verificar.")
	@ApiResponse(responseCode = "201", description = "Usuario registrado: {message, correo, "
			+ "requiereVerificacion=true}; se envió el código")
	@ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos: nombre, correo válido y "
			+ "contraseña son obligatorios; el teléfono, si se envía, debe tener de 7 a 15 dígitos. | La contraseña "
			+ "debe tener entre 8 y 64 caracteres, una mayúscula, una minúscula, un número y un símbolo.")
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
		case UsuarioService.DATOS_INVALIDOS -> mensaje(HttpStatus.BAD_REQUEST,
				"Datos incompletos o inválidos: "
						+ "nombre (máximo 255 caracteres), correo válido (máximo 150) y contraseña son obligatorios; "
						+ "el teléfono, si se envía, debe tener de 7 a 15 dígitos.");
		case UsuarioService.CONTRASENIA_DEBIL -> mensaje(HttpStatus.BAD_REQUEST, MENSAJE_CONTRASENIA_DEBIL);
		case UsuarioService.ROL_NO_PERMITIDO ->
			mensaje(HttpStatus.FORBIDDEN, "Solo puedes registrarte como HUESPED o ANFITRION.");
		default -> mensaje(HttpStatus.INTERNAL_SERVER_ERROR, "Error desconocido al registrar el usuario.");
		};
	}

	/**
	 * Confirma el correo con el código de verificación e inicia la sesión. Ruta:
	 * {@code POST /auth/verificar}.
	 *
	 * @param datos DTO con {@code correo} y {@code codigo}
	 * @return 200 con {message, token, rol, correo}; 400 si faltan datos o el
	 *         código no es válido o venció; 409 si el correo ya estaba verificado;
	 *         429 si se agotaron los intentos del código
	 */
	@Operation(summary = "Verificar el correo con el código recibido", description = "El código vence a los 15 minutos y admite 5 intentos. Devuelve el token de sesión.")
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
		case VerificacionCorreoService.CODIGO_INVALIDO ->
			mensaje(HttpStatus.BAD_REQUEST, "El código no es válido o ya venció. Revísalo o solicita uno nuevo.");
		case VerificacionCorreoService.INTENTOS_AGOTADOS -> mensaje(HttpStatus.TOO_MANY_REQUESTS,
				"Superaste los intentos permitidos para este código. Solicita uno nuevo.");
		case VerificacionCorreoService.YA_VERIFICADO ->
			mensaje(HttpStatus.CONFLICT, "El correo ya está verificado. Inicia sesión.");
		default -> mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo y el código.");
		};
	}

	/**
	 * Envía un código de verificación nuevo. Ruta:
	 * {@code POST /auth/reenviarcodigo}.
	 *
	 * @param solicitud DTO con {@code correo}
	 * @return 200 siempre que se envíe un correo (exista o no la cuenta, para no
	 *         revelarlo); 400 si falta el correo; 429 si el último código se pidió
	 *         hace menos de un minuto
	 */
	@Operation(summary = "Reenviar el código de verificación del correo", description = "Invalida el código anterior. Solo se permite una vez por minuto.")
	@ApiResponse(responseCode = "200", description = "Si la cuenta existe y no está verificada, te enviamos un código "
			+ "nuevo.")
	@ApiResponse(responseCode = "400", description = "Debes enviar el correo.")
	@ApiResponse(responseCode = "429", description = "Espera un minuto antes de pedir otro código.")
	@PostMapping("/reenviarcodigo")
	public ResponseEntity<Object> reenviarCodigo(@RequestBody UsuarioDTO solicitud) {
		return switch (verificacionCorreoService.reenviar(solicitud.getCorreo())) {
		case VerificacionCorreoService.ENVIADO ->
			mensaje(HttpStatus.OK, "Si la cuenta existe y no está verificada, te enviamos un código nuevo.");
		case VerificacionCorreoService.ESPERAR ->
			mensaje(HttpStatus.TOO_MANY_REQUESTS, "Espera un minuto antes de pedir otro código.");
		default -> mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo.");
		};
	}

	/**
	 * Solicita la recuperación de contraseña: el token se envía por correo al
	 * usuario y vence a los 30 minutos. Ruta: {@code POST /auth/recuperar}.
	 *
	 * @param solicitud DTO con {@code correo}
	 * @return 200 siempre que el correo venga (exista o no la cuenta, para no
	 *         revelar qué cuentas están registradas); 400 si falta el correo
	 */
	@Operation(summary = "Solicitar recuperación de contraseña", description = "Envía al correo un código de recuperación válido por 30 minutos.")
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
			+ "contraseña debe tener entre 8 y 64 caracteres, una mayúscula, una minúscula, un número y un símbolo. | "
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
	 *         las credenciales son inválidas; 403 si un administrador restringió la
	 *         cuenta; 409 si la cuenta ya estaba activa
	 */
	@Operation(summary = "Reactivar la cuenta propia", description = "Una cuenta restringida por un administrador no puede reactivarse por esta vía.")
	@ApiResponse(responseCode = "200", description = "Cuenta reactivada: {message, token, rol, correo, estado}")
	@ApiResponse(responseCode = "400", description = "Debes enviar el correo y la contraseña.")
	@ApiResponse(responseCode = "401", description = "Correo o contraseña inválidos.")
	@ApiResponse(responseCode = "403", description = "La cuenta fue restringida por un administrador y no puede "
			+ "reactivarse.")
	@ApiResponse(responseCode = "409", description = "La cuenta ya está activa.")
	@PostMapping("/reactivar")
	public ResponseEntity<Object> reactivar(@RequestBody UsuarioDTO credenciales) {
		return switch (authService.reactivarCuenta(credenciales.getCorreo(), credenciales.getContrasenia())) {
		case 0 -> respuestaReactivada(
				userDetailsService.loadUserByUsername(UsuarioService.normalizarCorreo(credenciales.getCorreo())));
		case 1 -> mensaje(HttpStatus.UNAUTHORIZED, "Correo o contraseña inválidos.");
		case 3 ->
			mensaje(HttpStatus.FORBIDDEN, "La cuenta fue restringida por un administrador y no puede reactivarse.");
		case 4 -> mensaje(HttpStatus.CONFLICT, "La cuenta ya está activa.");
		default -> mensaje(HttpStatus.BAD_REQUEST, "Debes enviar el correo y la contraseña.");
		};
	}

	/**
	 * Respuesta de una cuenta reactivada, con su nuevo estado. Si la cuenta
	 * tiene segundo factor no se entrega token aquí: debe iniciar sesión con
	 * su código.
	 *
	 * @param usuario cuenta reactivada
	 * @return 200 con {message, token, rol, correo, estado}, o sin token si
	 *         tiene segundo factor
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
	 * Respuesta de un correo recién verificado. Si la cuenta tiene segundo
	 * factor no se entrega token aquí: debe iniciar sesión con su código.
	 *
	 * @param usuario cuenta verificada
	 * @return 200 con {message, token, rol, correo}, o sin token si tiene
	 *         segundo factor
	 */
	private ResponseEntity<Object> respuestaVerificada(UserDetails usuario) {
		if (usuario instanceof Usuario u && SegundoFactorService.estaHabilitado(u)) {
			return mensaje(HttpStatus.OK, "Correo verificado. Inicia sesión con tu código.");
		}
		return respuestaConToken(HttpStatus.OK, usuario, "Correo verificado correctamente.");
	}

	/**
	 * Respuesta de sesión iniciada: {message?, token, rol, correo}. La usan el
	 * login, la verificación y la reactivación, para que el frontend guarde el
	 * token siempre igual.
	 *
	 * @param status estado HTTP
	 * @param userDetails usuario que inicia sesión
	 * @param texto mensaje opcional
	 * @return respuesta con el token
	 */
	private ResponseEntity<Object> respuestaConToken(HttpStatus status, UserDetails userDetails, String texto) {
		return ResponseEntity.status(status).body(cuerpoConToken(userDetails, texto));
	}

	/**
	 * Arma el cuerpo de la respuesta con el token: genera el JWT y agrega el
	 * rol (sin el prefijo {@code ROLE_}) y el correo.
	 *
	 * @param userDetails usuario que inicia sesión
	 * @param texto mensaje opcional; si es {@code null} no se incluye
	 * @return cuerpo de la respuesta
	 */
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

	/**
	 * Respuesta JSON uniforme {@code {"message": "..."}}, para que el frontend
	 * la lea siempre igual.
	 *
	 * @param status estado HTTP
	 * @param texto mensaje para el usuario
	 * @return respuesta con el mensaje
	 */
	private static ResponseEntity<Object> mensaje(HttpStatus status, String texto) {
		return RespuestaUtil.mensaje(status, texto);
	}
}
