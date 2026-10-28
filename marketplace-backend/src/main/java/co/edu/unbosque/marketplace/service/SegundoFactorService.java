package co.edu.unbosque.marketplace.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.SegundoFactorDTO;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.TotpUtil;

/**
 * Segundo factor de autenticación con TOTP. Activarlo tiene dos pasos: generar
 * el secreto (el usuario lo registra en su app) y confirmarlo con un código
 * válido. El secreto se guarda cifrado con AES y solo pertenece a su cuenta.
 *
 * @see "RNF-033, CA-174 a CA-178"
 */
@Service
public class SegundoFactorService {

	private static final String EMISOR = "MarketplaceInversionesLR";

	private final UsuarioRepository usuarioRepo;
	private final AuditoriaService auditoriaService;

	public SegundoFactorService(UsuarioRepository usuarioRepo, AuditoriaService auditoriaService) {
		this.usuarioRepo = usuarioRepo;
		this.auditoriaService = auditoriaService;
	}

	/** Indica si la cuenta tiene el segundo factor habilitado. */
	public static boolean estaHabilitado(Usuario usuario) {
		return Boolean.TRUE.equals(usuario.getTotpHabilitado());
	}

	/**
	 * Genera un secreto nuevo para el usuario autenticado. Queda pendiente de
	 * confirmación.
	 *
	 * @return 0 con el secreto y la URI otpauth; 1 usuario no existe; 4 ya está
	 *         habilitado.
	 * @see "CA-178"
	 */
	@Transactional
	public Resultado<SegundoFactorDTO> activar(Long idUsuario) {
		Optional<Usuario> usuario = usuarioRepo.findById(idUsuario);
		if (usuario.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "Usuario no encontrado.");
		}
		if (estaHabilitado(usuario.get())) {
			return Resultado.error(Resultado.ESTADO_INVALIDO, "El segundo factor ya está habilitado.");
		}
		String secreto = TotpUtil.generarSecreto();
		usuario.get().setSecretoTotp(AESUtil.encrypt(secreto));
		usuario.get().setTotpHabilitado(false);
		usuarioRepo.save(usuario.get());

		SegundoFactorDTO dto = new SegundoFactorDTO();
		dto.setSecreto(secreto);
		dto.setUri(TotpUtil.uri(EMISOR, usuario.get().getUsername(), secreto));
		return Resultado.exito(dto);
	}

	/**
	 * Confirma la activación con un código de la app.
	 *
	 * @return 0 habilitado; 1 usuario no existe; 2 código inválido; 4 no hay
	 *         activación pendiente.
	 * @see "CA-178"
	 */
	@Transactional
	public Resultado<Void> confirmar(Long idUsuario, String codigo) {
		Optional<Usuario> usuario = usuarioRepo.findById(idUsuario);
		if (usuario.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "Usuario no encontrado.");
		}
		if (usuario.get().getSecretoTotp() == null || estaHabilitado(usuario.get())) {
			return Resultado.error(Resultado.ESTADO_INVALIDO, "Primero solicita la activación del segundo factor.");
		}
		if (!verificar(usuario.get(), codigo)) {
			auditar(idUsuario, ResultadoAuditoria.RECHAZADO, "Código inválido al confirmar");
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El código no es válido.");
		}
		usuario.get().setTotpHabilitado(true);
		usuarioRepo.save(usuario.get());
		auditar(idUsuario, ResultadoAuditoria.EXITOSO, "Segundo factor habilitado");
		return Resultado.exito(null);
	}

	/**
	 * Deshabilita el segundo factor validando un código actual.
	 *
	 * @return 0 deshabilitado; 1 usuario no existe; 2 código inválido; 4 no estaba
	 *         habilitado.
	 * @see "CA-177"
	 */
	@Transactional
	public Resultado<Void> desactivar(Long idUsuario, String codigo) {
		Optional<Usuario> usuario = usuarioRepo.findById(idUsuario);
		if (usuario.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "Usuario no encontrado.");
		}
		if (!estaHabilitado(usuario.get())) {
			return Resultado.error(Resultado.ESTADO_INVALIDO, "El segundo factor no está habilitado.");
		}
		if (!verificar(usuario.get(), codigo)) {
			auditar(idUsuario, ResultadoAuditoria.RECHAZADO, "Código inválido al deshabilitar");
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El código no es válido.");
		}
		usuario.get().setTotpHabilitado(false);
		usuario.get().setSecretoTotp(null);
		usuarioRepo.save(usuario.get());
		auditar(idUsuario, ResultadoAuditoria.EXITOSO, "Segundo factor deshabilitado");
		return Resultado.exito(null);
	}

	/**
	 * Verifica un código contra el secreto de la cuenta.
	 *
	 * @see "CA-175, CA-176"
	 */
	public boolean verificar(Usuario usuario, String codigo) {
		if (usuario.getSecretoTotp() == null) {
			return false;
		}
		return TotpUtil.verificar(AESUtil.decrypt(usuario.getSecretoTotp()), codigo);
	}

	private void auditar(Long idUsuario, ResultadoAuditoria resultado, String detalle) {
		auditoriaService.registrar(idUsuario, AuditoriaService.SEGUNDO_FACTOR, AuditoriaService.ENTIDAD_USUARIO,
				idUsuario, resultado, detalle);
	}
}
