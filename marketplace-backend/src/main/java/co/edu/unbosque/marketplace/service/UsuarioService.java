
package co.edu.unbosque.marketplace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.exception.CapitalException;
import co.edu.unbosque.marketplace.exception.CharacterException;
import co.edu.unbosque.marketplace.exception.ExceptionCheker;
import co.edu.unbosque.marketplace.exception.MailException;
import co.edu.unbosque.marketplace.exception.NumberException;
import co.edu.unbosque.marketplace.exception.SmallException;
import co.edu.unbosque.marketplace.exception.SymbolException;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Reglas de negocio de usuarios: registro, creación de administradores, perfil
 * y desactivación de la cuenta propia. Cada operación queda en la auditoría.
 *
 * @see "RF-001"
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
@Service
public class UsuarioService implements CRUDOperation<UsuarioDTO> {

	/** Resultados de {@link #create(UsuarioDTO)}. */
	public static final int CREADO = 0;
	public static final int CORREO_DUPLICADO = 1;
	public static final int DATOS_INVALIDOS = 2;
	public static final int CONTRASENIA_DEBIL = 3;
	public static final int ROL_NO_PERMITIDO = 4;
	public static final int ERROR_INTERNO = 5;

	private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);

	private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\+?\\d{7,15}$");

	/**
	 * Separadores que se aceptan al escribir el teléfono y se quitan antes de
	 * guardarlo.
	 */
	private static final Pattern SEPARADORES_TELEFONO = Pattern.compile("[\\s().-]");

	private final UsuarioRepository usuarioRepo;
	private final AuditoriaService auditoriaService;
	private final NotificacionService notificacionService;
	private final VerificacionCorreoService verificacionCorreoService;
	private final PasswordEncoder passwordEncoder;
	private final ModelMapper modelMapper;

	public UsuarioService(UsuarioRepository usuarioRepo, AuditoriaService auditoriaService,
			NotificacionService notificacionService, VerificacionCorreoService verificacionCorreoService,
			PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
		this.usuarioRepo = usuarioRepo;
		this.auditoriaService = auditoriaService;
		this.notificacionService = notificacionService;
		this.verificacionCorreoService = verificacionCorreoService;
		this.passwordEncoder = passwordEncoder;
		this.modelMapper = modelMapper;
	}

	/**
	 * Forma única de guardar teléfonos: sin espacios, guiones, puntos ni
	 * paréntesis. Así "+57 300 000 0000" queda "+573000000000".
	 *
	 * @return el teléfono normalizado, o null si viene vacío.
	 */
	public static String normalizarTelefono(String telefono) {
		if (esVacio(telefono)) {
			return null;
		}
		return SEPARADORES_TELEFONO.matcher(telefono.trim()).replaceAll("");
	}

	/**
	 * Forma única de comparar correos en todo el sistema: sin espacios y en
	 * minúsculas. Se usa al registrar, al iniciar sesión y al buscar.
	 */
	public static String normalizarCorreo(String correo) {
		return correo == null ? null : correo.trim().toLowerCase();
	}

	/** Hash con el que se busca un correo en la base de datos. */
	public static String hashCorreo(String correo) {
		return AESUtil.hash(normalizarCorreo(correo));
	}

	/**
	 * Registra un usuario nuevo. La cuenta queda ACTIVA pero con el correo sin
	 * verificar: se le envía un código de verificación en el correo de bienvenida y
	 * no puede iniciar sesión hasta confirmarlo. Solo se permiten los roles HUESPED
	 * (por defecto) y ANFITRION; nunca ADMIN.
	 * <p>
	 * DECISIÓN (contradice el literal de RF-001): el registro público no crea
	 * administradores. El primer ADMIN lo crea {@code AdminInicializador} al
	 * arrancar y los siguientes los crea otro administrador con
	 * {@link #crearAdministrador(UsuarioDTO, Long)}.
	 * </p>
	 *
	 * @return {@link #CREADO}, {@link #CORREO_DUPLICADO}, {@link #DATOS_INVALIDOS},
	 *         {@link #CONTRASENIA_DEBIL}, {@link #ROL_NO_PERMITIDO} o
	 *         {@link #ERROR_INTERNO}
	 * @see "RF-001"
	 */
	@Override
	public int create(UsuarioDTO data) {
		Role rol = data == null || data.getRol() == null ? Role.HUESPED : data.getRol();
		if (rol == Role.ADMIN) {
			auditoriaService.registrar(null, AuditoriaService.REGISTRO_USUARIO, AuditoriaService.ENTIDAD_USUARIO, null,
					ResultadoAuditoria.RECHAZADO, "Intento de registro público como ADMIN");
			return ROL_NO_PERMITIDO;
		}
		return registrar(data, rol, null, AuditoriaService.REGISTRO_USUARIO);
	}

	/**
	 * Crea otro administrador. Solo lo puede invocar un ADMIN (lo controla
	 * {@code UsuarioController} con {@code @PreAuthorize}).
	 *
	 * @param data    Datos del nuevo administrador; el rol enviado se ignora.
	 * @param idActor Administrador que hace la creación (sale del token JWT).
	 * @return los mismos códigos de {@link #create(UsuarioDTO)}, excepto
	 *         {@link #ROL_NO_PERMITIDO}.
	 * @see "RF-001"
	 */
	public int crearAdministrador(UsuarioDTO data, Long idActor) {
		return registrar(data, Role.ADMIN, idActor, AuditoriaService.CREACION_ADMINISTRADOR);
	}

	/**
	 * Validaciones y guardado comunes a {@link #create(UsuarioDTO)} y
	 * {@link #crearAdministrador(UsuarioDTO, Long)}.
	 */
	private int registrar(UsuarioDTO data, Role rol, Long idActor, String accion) {
		if (data == null || esVacio(data.getNombre()) || esVacio(data.getCorreo()) || esVacio(data.getContrasenia())
				|| !telefonoValido(data.getTelefono())) {
			auditarRechazo(idActor, accion, "Datos incompletos o inválidos");
			return DATOS_INVALIDOS;
		}

		String correo = normalizarCorreo(data.getCorreo());

		try {
			ExceptionCheker.checkerMail(correo);
		} catch (MailException e) {
			auditarRechazo(idActor, accion, "Correo con formato inválido");
			return DATOS_INVALIDOS;
		}

		try {
			ExceptionCheker.checkerPasword(data.getContrasenia());
		} catch (CharacterException | CapitalException | SmallException | NumberException | SymbolException e) {
			auditarRechazo(idActor, accion, "Contraseña débil");
			return CONTRASENIA_DEBIL;
		}

		Usuario usuario;
		try {
			String correoHash = AESUtil.hash(correo);
			if (usuarioRepo.existsByCorreoHash(correoHash)) {
				auditarRechazo(idActor, accion, "Correo ya registrado");
				return CORREO_DUPLICADO;
			}

			// Se arma la entidad campo por campo (sin ModelMapper) para que el
			// cliente no pueda fijar id, estado ni otros campos internos.
			usuario = new Usuario();
			usuario.setNombre(data.getNombre().trim());
			usuario.setCorreo(AESUtil.encrypt(correo));
			usuario.setCorreoHash(correoHash);
			usuario.setTelefono(normalizarTelefono(data.getTelefono()));
			usuario.setContrasenia(passwordEncoder.encode(data.getContrasenia()));
			usuario.setRol(rol);
			usuario.setEstado(EstadoUsuario.ACTIVO);
			usuario.setFechaRegistro(LocalDateTime.now());
			usuario.setCorreoVerificado(false);

			usuario = usuarioRepo.save(usuario);
		} catch (DataIntegrityViolationException e) {
			// Dos registros simultáneos con el mismo correo: lo frena el índice único
			auditarRechazo(idActor, accion, "Correo ya registrado");
			return CORREO_DUPLICADO;
		} catch (RuntimeException e) {
			LOG.error("Error al registrar el usuario", e);
			auditoriaService.registrar(idActor, accion, AuditoriaService.ENTIDAD_USUARIO, null,
					ResultadoAuditoria.ERROR, e.getClass().getSimpleName());
			return ERROR_INTERNO;
		}

		auditoriaService.registrar(idActor == null ? usuario.getIdUsuario() : idActor, accion,
				AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.EXITOSO, "Rol " + rol);

		// Correo de bienvenida con el código de verificación (RF-001)
		verificacionCorreoService.enviarCodigo(usuario, correo, data.getNombre().trim(), true);

		return CREADO;
	}

	@Override
	public List<UsuarioDTO> getAll() {
		List<Usuario> usuarios = usuarioRepo.findAll();
		List<UsuarioDTO> resultado = new ArrayList<>();

		for (Usuario usuario : usuarios) {
			resultado.add(convertirDTO(usuario));
		}

		return resultado;
	}

	@Override
	public UsuarioDTO getById(Long id) {
		Optional<Usuario> usuario = usuarioRepo.findById(id);

		if (usuario.isEmpty()) {
			return null;
		}

		return convertirDTO(usuario.get());
	}

	private UsuarioDTO convertirDTO(Usuario usuario) {
		UsuarioDTO dto = modelMapper.map(usuario, UsuarioDTO.class);
		dto.setIdUsuario(usuario.getIdUsuario());
		dto.setNombre(usuario.getNombre());
		dto.setCorreo(AESUtil.decrypt(usuario.getCorreo()));
		dto.setContrasenia(null);

		return dto;
	}

	@Override
	public int deleteById(Long id) {
		if (!usuarioRepo.existsById(id)) {
			return 1;
		}

		usuarioRepo.deleteById(id);
		return 0;
	}

	@Override
	public int updateById(Long id, UsuarioDTO newData) {
		Optional<Usuario> usuarioOpt = usuarioRepo.findById(id);

		if (usuarioOpt.isEmpty()) {
			return 1;
		}

		try {
			Usuario usuario = usuarioOpt.get();

			if (newData.getNombre() != null) {
				usuario.setNombre(newData.getNombre());
			}

			if (!esVacio(newData.getCorreo())) {
				String correo = normalizarCorreo(newData.getCorreo());
				String correoHash = AESUtil.hash(correo);

				Optional<Usuario> existente = usuarioRepo.findByCorreoHash(correoHash);

				if (existente.isPresent() && !existente.get().getIdUsuario().equals(id)) {
					return 2;
				}

				usuario.setCorreo(AESUtil.encrypt(correo));
				usuario.setCorreoHash(correoHash);
			}

			if (!esVacio(newData.getContrasenia())) {
				usuario.setContrasenia(passwordEncoder.encode(newData.getContrasenia()));
			}

			if (newData.getRol() != null) {
				usuario.setRol(newData.getRol());
			}

			usuarioRepo.save(usuario);
			return 0;

		} catch (RuntimeException e) {
			LOG.error("Error al actualizar el usuario {}", id, e);
			return 2;
		}
	}

	@Override
	public long count() {
		return usuarioRepo.count();
	}

	@Override
	public boolean exist(Long id) {
		return usuarioRepo.existsById(id);
	}

	@Override
	public boolean findUsernameAlreadyTaken(Usuario newUser) {
		return newUser.getCorreoHash() != null && usuarioRepo.existsByCorreoHash(newUser.getCorreoHash());
	}

	@Override
	public boolean encontrarCorreoExitente(String username) {
		return !esVacio(username) && usuarioRepo.existsByCorreoHash(hashCorreo(username));
	}

	public UsuarioDTO obtenerUsuarioPorCorreo(String correo) {
		if (esVacio(correo)) {
			return null;
		}

		return usuarioRepo.findByCorreoHash(hashCorreo(correo)).map(this::convertirDTO).orElse(null);
	}

	/**
	 * Desactiva la cuenta propia (queda INACTIVO). Se reactiva con
	 * {@code POST /auth/reactivar}. Un administrador no puede desactivarse.
	 *
	 * @param correo Correo del usuario autenticado (sale del token JWT).
	 * @return 0 desactivada; 1 usuario no encontrado; 2 es ADMIN o la cuenta no
	 *         está ACTIVO.
	 * @see "RF-012, CA-050, CA-052"
	 */
	public int desactivarCuenta(String correo) {
		if (esVacio(correo)) {
			return 1;
		}

		Optional<Usuario> usuarioOpt = usuarioRepo.findByCorreoHash(hashCorreo(correo));
		if (usuarioOpt.isEmpty()) {
			return 1;
		}

		Usuario usuario = usuarioOpt.get();
		if (usuario.getRol() == Role.ADMIN || usuario.getEstado() != EstadoUsuario.ACTIVO) {
			return 2;
		}

		usuario.setEstado(EstadoUsuario.INACTIVO);
		usuarioRepo.save(usuario);

		auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.CUENTA_DESACTIVADA,
				AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.EXITOSO, null);
		return 0;
	}

	/**
	 * Actualiza el perfil propio. Solo se pueden cambiar el nombre y el teléfono;
	 * correo, rol, estado y contraseña no se tocan por esta vía. Los campos que
	 * lleguen en null se dejan como están.
	 *
	 * @param correo   Correo del usuario autenticado (sale del token JWT).
	 * @param nombre   Nombre nuevo; si se envía no puede estar vacío.
	 * @param telefono Teléfono nuevo: 7 a 15 dígitos, opcionalmente con + y
	 *                 separadores (espacios, guiones, puntos o paréntesis).
	 * @return 0 actualizado; 1 usuario no encontrado; 2 datos inválidos o ningún
	 *         campo para actualizar.
	 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
	 */
	public int actualizarPerfil(String correo, String nombre, String telefono) {
		if ((nombre == null && telefono == null) || (nombre != null && nombre.isBlank())
				|| (telefono != null && !telefonoValido(telefono))) {
			return 2;
		}

		Optional<Usuario> usuarioOpt = esVacio(correo) ? Optional.empty()
				: usuarioRepo.findByCorreoHash(hashCorreo(correo));
		if (usuarioOpt.isEmpty()) {
			return 1;
		}

		Usuario usuario = usuarioOpt.get();
		StringBuilder cambios = new StringBuilder();
		if (nombre != null) {
			usuario.setNombre(nombre.trim());
			cambios.append("nombre ");
		}
		if (telefono != null) {
			usuario.setTelefono(normalizarTelefono(telefono));
			cambios.append("telefono");
		}
		usuarioRepo.save(usuario);

		auditoriaService.registrar(usuario.getIdUsuario(), AuditoriaService.PERFIL_ACTUALIZADO,
				AuditoriaService.ENTIDAD_USUARIO, usuario.getIdUsuario(), ResultadoAuditoria.EXITOSO,
				"Campos: " + cambios.toString().trim());
		return 0;
	}

	/**
	 * Usuarios de forma paginada (vista del administrador).
	 *
	 * @see "RF-014"
	 */
	public PaginaDTO<UsuarioDTO> listarPaginado(Pageable pagina) {
		return PaginaDTO.de(usuarioRepo.findAll(pagina), this::convertirDTO);
	}

	/**
	 * Restringe una cuenta con un motivo (administrador). Una cuenta restringida no
	 * puede iniciar sesión, sus tokens dejan de funcionar y no puede reactivarse
	 * sola. No se puede restringir a un administrador.
	 *
	 * @return 0 con el usuario; 1 no existe; 2 falta el motivo; 3 es administrador.
	 * @see "RF-014, CA-058, CA-059, CA-060"
	 */
	@Transactional
	public Resultado<UsuarioDTO> restringir(Long idUsuario, String motivo, Long idAdmin) {
		if (esVacio(motivo)) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El motivo de la restricción es obligatorio.");
		}
		Optional<Usuario> usuario = usuarioRepo.findById(idUsuario);
		if (usuario.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "Usuario no encontrado.");
		}
		if (usuario.get().getRol() == Role.ADMIN) {
			auditoriaService.registrar(idAdmin, AuditoriaService.USUARIO_RESTRINGIDO, AuditoriaService.ENTIDAD_USUARIO,
					idUsuario, ResultadoAuditoria.RECHAZADO, "No se puede restringir a un administrador");
			return Resultado.error(Resultado.SIN_PERMISO, "No se puede restringir a un administrador.");
		}
		usuario.get().setEstado(EstadoUsuario.RESTRINGIDO);
		usuario.get().setMotivoRestriccion(motivo.trim());
		Usuario guardado = usuarioRepo.save(usuario.get());

		notificacionService.enviar(idUsuario, NotificacionService.CUENTA_RESTRINGIDA,
				"Tu cuenta fue restringida por un administrador. Motivo: " + motivo.trim());
		auditoriaService.registrar(idAdmin, AuditoriaService.USUARIO_RESTRINGIDO, AuditoriaService.ENTIDAD_USUARIO,
				idUsuario, ResultadoAuditoria.EXITOSO, motivo.trim());
		return Resultado.exito(convertirDTO(guardado));
	}

	/**
	 * Libera una cuenta restringida (administrador). Queda ACTIVA.
	 * <p>
	 * DECISIÓN: el motivo es opcional. Las acciones que castigan (restringir,
	 * bloquear, rechazar, ocultar) exigen motivo porque se le comunica al afectado
	 * y respalda la decisión en la auditoría; devolver el acceso no perjudica a
	 * nadie, así que el motivo solo se guarda si se envía.
	 * </p>
	 *
	 * @param motivo Motivo opcional; si se envía queda en la auditoría.
	 * @return 0 con el usuario; 1 no existe; 4 no estaba restringida.
	 * @see "RF-014"
	 */
	@Transactional
	public Resultado<UsuarioDTO> liberar(Long idUsuario, String motivo, Long idAdmin) {
		Optional<Usuario> usuario = usuarioRepo.findById(idUsuario);
		if (usuario.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "Usuario no encontrado.");
		}
		if (usuario.get().getEstado() != EstadoUsuario.RESTRINGIDO) {
			return Resultado.error(Resultado.ESTADO_INVALIDO, "La cuenta no está restringida.");
		}
		usuario.get().setEstado(EstadoUsuario.ACTIVO);
		usuario.get().setMotivoRestriccion(null);
		Usuario guardado = usuarioRepo.save(usuario.get());

		notificacionService.enviar(idUsuario, NotificacionService.CUENTA_LIBERADA,
				"Tu cuenta fue habilitada de nuevo por un administrador.");
		auditoriaService.registrar(idAdmin, AuditoriaService.USUARIO_LIBERADO, AuditoriaService.ENTIDAD_USUARIO,
				idUsuario, ResultadoAuditoria.EXITOSO, esVacio(motivo) ? null : motivo.trim());
		return Resultado.exito(convertirDTO(guardado));
	}

	/**
	 * El teléfono es opcional; si se envía debe tener de 7 a 15 dígitos,
	 * opcionalmente precedidos de + y con separadores (espacios, guiones, puntos o
	 * paréntesis). Vacío equivale a no enviarlo.
	 */
	private static boolean telefonoValido(String telefono) {
		return esVacio(telefono) || PATRON_TELEFONO.matcher(normalizarTelefono(telefono)).matches();
	}

	private void auditarRechazo(Long idActor, String accion, String motivo) {
		auditoriaService.registrar(idActor, accion, AuditoriaService.ENTIDAD_USUARIO, null,
				ResultadoAuditoria.RECHAZADO, motivo);
	}

	private static boolean esVacio(String texto) {
		return texto == null || texto.isBlank();
	}
}
