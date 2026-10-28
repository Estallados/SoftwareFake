package co.edu.unbosque.marketplace.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.entity.CodigoVerificacion;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.CodigoVerificacionRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;
import co.edu.unbosque.marketplace.util.CorreoUtil;

/**
 * Verificación del correo con un código de 6 dígitos. Al registrarse, el
 * usuario recibe el código en el correo de bienvenida y no puede iniciar sesión
 * hasta confirmarlo con {@code POST /auth/verificar}.
 * <p>
 * Reglas: el código vence a los 15 minutos, sirve una sola vez y admite 5
 * intentos fallidos; pedir uno nuevo invalida el anterior y solo se permite una
 * vez por minuto. Se guarda únicamente el hash del código. Si el correo no se
 * puede enviar (por ejemplo, en desarrollo sin MAIL_PASSWORD), el código se
 * escribe en el log para no dejar la cuenta bloqueada.
 * </p>
 *
 * @see "RF-001"
 */
@Service
public class VerificacionCorreoService {

	/** Resultados de {@link #verificar(String, String)}. */
	public static final int VERIFICADO = 0;
	public static final int CODIGO_INVALIDO = 1;
	public static final int DATOS_INVALIDOS = 2;
	public static final int INTENTOS_AGOTADOS = 3;
	public static final int YA_VERIFICADO = 4;

	/** Resultados de {@link #reenviar(String)}. */
	public static final int ENVIADO = 0;
	public static final int ESPERAR = 3;

	/** Vigencia del código en minutos (se informa en el correo). */
	public static final int MINUTOS_VIGENCIA = 15;

	private static final Duration VIGENCIA_CODIGO = Duration.ofMinutes(MINUTOS_VIGENCIA);
	private static final Duration ESPERA_REENVIO = Duration.ofMinutes(1);
	private static final int MAXIMO_INTENTOS = 5;
	private static final int LIMITE_CODIGO = 1_000_000;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Logger LOG = LoggerFactory.getLogger(VerificacionCorreoService.class);

	private final UsuarioRepository usuarioRepo;
	private final CodigoVerificacionRepository codigoRepo;
	private final CorreoUtil correoUtil;
	private final AuditoriaService auditoriaService;

	public VerificacionCorreoService(UsuarioRepository usuarioRepo, CodigoVerificacionRepository codigoRepo,
			CorreoUtil correoUtil, AuditoriaService auditoriaService) {
		this.usuarioRepo = usuarioRepo;
		this.codigoRepo = codigoRepo;
		this.correoUtil = correoUtil;
		this.auditoriaService = auditoriaService;
	}

	/**
	 * Genera un código nuevo para el usuario y lo envía en el correo de bienvenida
	 * (registro) o en un correo de reenvío. Invalida los códigos anteriores sin
	 * usar.
	 *
	 * @param usuario    Usuario ya guardado.
	 * @param correo     Correo en claro del usuario.
	 * @param nombre     Nombre en claro, para el saludo.
	 * @param bienvenida true en el registro; false al reenviar el código.
	 * @return {@code true} si el correo se envió.
	 */
	@Transactional
	public boolean enviarCodigo(Usuario usuario, String correo, String nombre, boolean bienvenida) {
		invalidarAnteriores(usuario.getIdUsuario());

		String codigo = generarCodigo();
		LocalDateTime ahora = LocalDateTime.now();
		CodigoVerificacion registro = new CodigoVerificacion();
		registro.setIdUsuario(usuario.getIdUsuario());
		registro.setCodigoHash(AESUtil.hash(codigo));
		registro.setFechaCreacion(ahora);
		registro.setFechaExpiracion(ahora.plus(VIGENCIA_CODIGO));
		registro.setIntentos(0);
		registro.setUsado(false);
		codigoRepo.save(registro);

		boolean enviado = correoUtil.enviarCorreo(correo,
				bienvenida ? "Bienvenido al Marketplace de Inversiones LR: verifica tu correo"
						: "Tu nuevo código de verificación - Marketplace Inversiones LR",
				cuerpoCorreo(nombre, codigo, bienvenida));
		if (!enviado) {
			// Igual que la recuperación simulada: sin servidor de correo (desarrollo) el
			// código queda en el log
			LOG.warn("[SIMULACION DE CORREO] Código de verificación del usuario {}: {} (vence {})",
					usuario.getIdUsuario(), codigo, registro.getFechaExpiracion());
		}
		auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.CODIGO_VERIFICACION_ENVIADO,
				AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(),
				enviado ? ResultadoAuditoria.EXITOSO : ResultadoAuditoria.ERROR,
				enviado ? null : "No se pudo enviar el correo");
		return enviado;
	}

	/**
	 * Confirma el correo con el código recibido. Cada intento fallido cuenta; al
	 * llegar al máximo el código deja de servir y hay que pedir otro.
	 *
	 * @param correo Correo de la cuenta.
	 * @param codigo Código de 6 dígitos.
	 * @return {@link #VERIFICADO}, {@link #CODIGO_INVALIDO} (también si la cuenta
	 *         no existe, para no revelarlo), {@link #DATOS_INVALIDOS},
	 *         {@link #INTENTOS_AGOTADOS} o {@link #YA_VERIFICADO}.
	 */
	@Transactional
	public int verificar(String correo, String codigo) {
		if (esVacio(correo) || esVacio(codigo)) {
			return DATOS_INVALIDOS;
		}
		Optional<Usuario> usuarioOpt = usuarioRepo.findByCorreoHash(UsuarioService.hashCorreo(correo));
		if (usuarioOpt.isEmpty()) {
			return CODIGO_INVALIDO;
		}
		Usuario usuario = usuarioOpt.get();
		if (usuario.esCorreoVerificado()) {
			return YA_VERIFICADO;
		}

		Optional<CodigoVerificacion> registroOpt = codigoRepo
				.findTopByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario());
		if (registroOpt.isEmpty() || registroOpt.get().getFechaExpiracion().isBefore(LocalDateTime.now())) {
			return rechazar(usuario, CODIGO_INVALIDO, "Código vencido o inexistente");
		}
		CodigoVerificacion registro = registroOpt.get();
		if (registro.getIntentos() >= MAXIMO_INTENTOS) {
			return rechazar(usuario, INTENTOS_AGOTADOS, "Intentos agotados");
		}
		if (!AESUtil.hash(codigo.trim()).equals(registro.getCodigoHash())) {
			registro.setIntentos(registro.getIntentos() + 1);
			codigoRepo.save(registro);
			return rechazar(usuario, registro.getIntentos() >= MAXIMO_INTENTOS ? INTENTOS_AGOTADOS : CODIGO_INVALIDO,
					"Código incorrecto");
		}

		registro.setUsado(true);
		codigoRepo.save(registro);
		usuario.setCorreoVerificado(true);
		usuarioRepo.save(usuario);
		auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.CORREO_VERIFICADO,
				AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.EXITOSO, null);
		return VERIFICADO;
	}

	/**
	 * Envía un código nuevo a una cuenta sin verificar. Si la cuenta no existe o ya
	 * está verificada responde igual que si lo hubiera enviado, para no revelar qué
	 * correos están registrados.
	 *
	 * @param correo Correo de la cuenta.
	 * @return {@link #ENVIADO}, {@link #DATOS_INVALIDOS} si falta el correo o
	 *         {@link #ESPERAR} si el último código se pidió hace menos de un
	 *         minuto.
	 */
	@Transactional
	public int reenviar(String correo) {
		if (esVacio(correo)) {
			return DATOS_INVALIDOS;
		}
		Optional<Usuario> usuarioOpt = usuarioRepo.findByCorreoHash(UsuarioService.hashCorreo(correo));
		if (usuarioOpt.isEmpty() || usuarioOpt.get().esCorreoVerificado()) {
			return ENVIADO;
		}
		Usuario usuario = usuarioOpt.get();
		Optional<CodigoVerificacion> ultimo = codigoRepo
				.findTopByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario());
		if (ultimo.isPresent() && ultimo.get().getFechaCreacion().plus(ESPERA_REENVIO).isAfter(LocalDateTime.now())) {
			return ESPERAR;
		}
		enviarCodigo(usuario, UsuarioService.normalizarCorreo(correo), AESUtil.decrypt(usuario.getNombre()), false);
		return ENVIADO;
	}

	private void invalidarAnteriores(Long idUsuario) {
		for (CodigoVerificacion anterior : codigoRepo.findByIdUsuarioAndUsadoFalse(idUsuario)) {
			anterior.setUsado(true);
			codigoRepo.save(anterior);
		}
	}

	private int rechazar(Usuario usuario, int codigo, String motivo) {
		auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.CORREO_VERIFICADO,
				AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.RECHAZADO, motivo);
		return codigo;
	}

	private static String cuerpoCorreo(String nombre, String codigo, boolean bienvenida) {
		String saludo = bienvenida
				? "¡Bienvenido al Marketplace de Alojamientos de Inversiones LR!\n\n"
						+ "Nos alegra que formes parte de nuestra plataforma, "
						+ "donde podrás descubrir, comparar y reservar alojamientos, "
						+ "o publicar tus propiedades como anfitrión.\n\n"
						+ "Para activar tu cuenta, ingresa este código de verificación:\n\n"
				: "Este es tu nuevo código de verificación:\n\n";
		return String.format("Hola %s,%n%n%s    %s%n%n" + "El código vence en %d minutos y solo puede usarse una vez. "
				+ "Si no creaste esta cuenta, ignora este mensaje.%n%n" + "Saludos,%n"
				+ "Equipo Marketplace Inversiones LR", nombre, saludo, codigo, MINUTOS_VIGENCIA);
	}

	/** Código numérico de 6 dígitos, con ceros a la izquierda si hace falta. */
	private static String generarCodigo() {
		return String.format("%06d", RANDOM.nextInt(LIMITE_CODIGO));
	}

	private static boolean esVacio(String texto) {
		return texto == null || texto.isBlank();
	}
}
