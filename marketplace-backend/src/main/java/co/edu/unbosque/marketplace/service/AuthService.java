package co.edu.unbosque.marketplace.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.TokenRecuperacion;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.exception.ExceptionCheker;
import co.edu.unbosque.marketplace.repository.TokenRecuperacionRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;

/**
 * Operaciones de cuenta que no requieren sesión: recuperación de contraseña
 * (simulada) y reactivación de una cuenta desactivada por su dueño.
 * <p>
 * Recuperación simulada: en lugar de enviar un correo, el token se escribe en
 * el log de la aplicación. Vence a los 30 minutos y sirve una sola vez.
 * </p>
 *
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private static final Duration VIGENCIA_TOKEN = Duration.ofMinutes(30);
    private static final int BYTES_TOKEN = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepo;
    private final TokenRecuperacionRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public AuthService(UsuarioRepository usuarioRepo, TokenRecuperacionRepository tokenRepo,
            PasswordEncoder passwordEncoder, AuditoriaService auditoriaService) {
        this.usuarioRepo = usuarioRepo;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Genera un token de recuperación para el correo indicado. Invalida los
     * tokens anteriores sin usar del mismo usuario.
     * <p>
     * Siempre responde igual exista o no el correo, para no revelar qué
     * correos están registrados.
     * </p>
     *
     * @param correo Correo de la cuenta.
     * @return 0 solicitud recibida; 2 correo vacío.
     */
    @Transactional
    public int solicitarRecuperacion(String correo) {
        if (correo == null || correo.isBlank()) {
            return 2;
        }

        Optional<Usuario> usuarioOpt = usuarioRepo.findByCorreoHash(UsuarioService.hashCorreo(correo));
        if (usuarioOpt.isEmpty()) {
            auditoriaService.registrar(null, AuditoriaService.RECUPERACION_SOLICITADA,
                    AuditoriaService.ENTIDAD_USUARIO, null, ResultadoAuditoria.RECHAZADO, "Correo no registrado");
            return 0;
        }

        Usuario usuario = usuarioOpt.get();
        for (TokenRecuperacion anterior : tokenRepo.findByIdUsuarioAndUsadoFalse(usuario.getIdUsuario())) {
            anterior.setUsado(true);
            tokenRepo.save(anterior);
        }

        String token = generarToken();
        TokenRecuperacion registro = new TokenRecuperacion();
        registro.setIdUsuario(usuario.getIdUsuario());
        registro.setTokenHash(AESUtil.hash(token));
        registro.setFechaExpiracion(LocalDateTime.now().plus(VIGENCIA_TOKEN));
        registro.setUsado(false);
        tokenRepo.save(registro);

        // SIMULACIÓN: en producción este token se enviaría por correo al usuario
        LOG.warn("[SIMULACION DE CORREO] Token para restablecer la contraseña del usuario {}: {} (vence {})",
                usuario.getIdUsuario(), token, registro.getFechaExpiracion());

        auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.RECUPERACION_SOLICITADA,
                AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.EXITOSO, null);
        return 0;
    }

    /**
     * Cambia la contraseña usando un token de recuperación vigente.
     *
     * @param token            Token recibido (en esta versión, tomado del log).
     * @param nuevaContrasenia Contraseña nueva; debe cumplir RNF-008.
     * @return 0 contraseña cambiada; 1 token inválido, vencido o ya usado;
     *         2 datos incompletos; 3 contraseña débil.
     */
    @Transactional
    public int restablecerContrasenia(String token, String nuevaContrasenia) {
        if (token == null || token.isBlank() || nuevaContrasenia == null || nuevaContrasenia.isBlank()) {
            return 2;
        }
        if (!ExceptionCheker.contraseniaValida(nuevaContrasenia)) {
            return 3;
        }

        Optional<TokenRecuperacion> registroOpt = tokenRepo.findByTokenHash(AESUtil.hash(token));
        if (registroOpt.isEmpty() || Boolean.TRUE.equals(registroOpt.get().getUsado())
                || registroOpt.get().getFechaExpiracion().isBefore(LocalDateTime.now())) {
            auditoriaService.registrar(null, AuditoriaService.CONTRASENIA_RESTABLECIDA,
                    AuditoriaService.ENTIDAD_USUARIO, null, ResultadoAuditoria.RECHAZADO,
                    "Token inválido, vencido o ya usado");
            return 1;
        }

        TokenRecuperacion registro = registroOpt.get();
        Optional<Usuario> usuarioOpt = usuarioRepo.findById(registro.getIdUsuario());
        if (usuarioOpt.isEmpty()) {
            return 1;
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setContrasenia(passwordEncoder.encode(nuevaContrasenia));
        usuarioRepo.save(usuario);

        registro.setUsado(true);
        tokenRepo.save(registro);

        auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.CONTRASENIA_RESTABLECIDA,
                AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.EXITOSO, null);
        return 0;
    }

    /**
     * Reactiva una cuenta que su dueño desactivó, validando sus credenciales.
     * Una cuenta RESTRINGIDA por un administrador no puede reactivarse sola.
     *
     * @param correo      Correo de la cuenta.
     * @param contrasenia Contraseña actual.
     * @return 0 reactivada; 1 credenciales inválidas; 2 datos incompletos;
     *         3 cuenta restringida por un administrador; 4 la cuenta ya está activa.
     */
    @Transactional
    public int reactivarCuenta(String correo, String contrasenia) {
        if (correo == null || correo.isBlank() || contrasenia == null || contrasenia.isBlank()) {
            return 2;
        }

        Optional<Usuario> usuarioOpt = usuarioRepo.findByCorreoHash(UsuarioService.hashCorreo(correo));
        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(contrasenia, usuarioOpt.get().getContrasenia())) {
            auditoriaService.registrar(null, AuditoriaService.CUENTA_REACTIVADA, AuditoriaService.ENTIDAD_USUARIO,
                    null, ResultadoAuditoria.RECHAZADO, "Credenciales inválidas");
            return 1;
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.getEstado() == EstadoUsuario.RESTRINGIDO) {
            auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.CUENTA_REACTIVADA,
                    AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.RECHAZADO,
                    "Cuenta restringida por un administrador");
            return 3;
        }
        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
            return 4;
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepo.save(usuario);

        auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.CUENTA_REACTIVADA,
                AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.EXITOSO, null);
        return 0;
    }

    private static String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
