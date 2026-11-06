
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
import co.edu.unbosque.marketplace.util.LimitesTexto;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Reglas de negocio de los usuarios: registro, creación de administradores,
 * perfil, desactivación de la cuenta propia y la restricción de cuentas por
 * parte del administrador. Cada operación queda en la auditoría.
 */
@Service
public class UsuarioService implements CRUDOperation<UsuarioDTO> {

	/** Resultado de {@link #create(UsuarioDTO)}: el usuario se creó. */
	public static final int CREADO = 0;

	/** El correo ya está registrado. */
	public static final int CORREO_DUPLICADO = 1;

	/** Faltan datos o no son válidos. */
	public static final int DATOS_INVALIDOS = 2;

	/** La contraseña no cumple las reglas. */
	public static final int CONTRASENIA_DEBIL = 3;

	/** Se pidió un rol que no se puede elegir (ADMIN). */
	public static final int ROL_NO_PERMITIDO = 4;

	/** Falló el guardado. */
	public static final int ERROR_INTERNO = 5;

	/** Log de la clase. */
	private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);

	/**
	 * Forma válida de un teléfono ya sin separadores: 7 a 15 dígitos, con +
	 * opcional.
	 */
	private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\+?\\d{7,15}$");

	/**
	 * Separadores que se aceptan al escribir el teléfono y se quitan antes de
	 * guardarlo.
	 */
	private static final Pattern SEPARADORES_TELEFONO = Pattern.compile("[\\s().-]");

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarioRepo;

	/** Servicio de auditoría. */
	private final AuditoriaService auditoriaService;

	/** Avisa al usuario cuando lo restringen o lo habilitan. */
	private final NotificacionService notificacionService;

	/** Envía el código de verificación al registrarse. */
	private final VerificacionCorreoService verificacionCorreoService;

	/** Codificador de contraseñas. */
	private final PasswordEncoder passwordEncoder;

	/** Copia los datos de la entidad al DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarioRepo repositorio de usuarios
	 * @param auditoriaService servicio de auditoría
	 * @param notificacionService servicio de notificaciones
	 * @param verificacionCorreoService servicio de verificación del correo
	 * @param passwordEncoder codificador de contraseñas
	 * @param modelMapper copiador de entidades a DTO
	 */
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
	 * Forma única de guardar los teléfonos: sin espacios, guiones, puntos ni
	 * paréntesis. Así "+57 300 000 0000" queda "+573000000000".
	 *
	 * @param telefono teléfono como lo escribió el usuario
	 * @return el teléfono normalizado, o {@code null} si viene vacío
	 */
	public static String normalizarTelefono(String telefono) {
		if (esVacio(telefono)) {
			return null;
		}
		return SEPARADORES_TELEFONO.matcher(telefono.trim()).replaceAll("");
	}

	/**
	 * Forma única de comparar correos en todo el sistema: sin espacios a los
	 * lados y en minúsculas. Se usa al registrar, al iniciar sesión y al
	 * buscar.
	 *
	 * @param correo correo como lo escribió el usuario
	 * @return correo normalizado
	 */
	public static String normalizarCorreo(String correo) {
		return correo == null ? null : correo.trim().toLowerCase();
	}

	/**
	 * Hash con el que se busca un correo en la base de datos (el correo se
	 * guarda cifrado y no se puede buscar directamente).
	 *
	 * @param correo correo; se normaliza antes de calcular el hash
	 * @return hash del correo
	 */
	public static String hashCorreo(String correo) {
		return AESUtil.hash(normalizarCorreo(correo));
	}

	/**
	 * Registra un usuario nuevo. La cuenta queda ACTIVA pero con el correo sin
	 * verificar: se le envía un código de verificación en el correo de
	 * bienvenida y no puede iniciar sesión hasta confirmarlo. Solo se permiten
	 * los roles HUESPED (por defecto) y ANFITRION, nunca ADMIN.
	 * <p>
	 * Decisión: el registro público no crea administradores. El primer ADMIN
	 * lo crea {@code AdminInicializador} al arrancar y los siguientes los crea
	 * otro administrador con {@link #crearAdministrador(UsuarioDTO, Long)}.
	 * </p>
	 *
	 * @param data datos del usuario nuevo
	 * @return {@link #CREADO}, {@link #CORREO_DUPLICADO},
	 *         {@link #DATOS_INVALIDOS}, {@link #CONTRASENIA_DEBIL},
	 *         {@link #ROL_NO_PERMITIDO} o {@link #ERROR_INTERNO}
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
	 * Crea otro administrador. Solo lo puede pedir un ADMIN (lo controla
	 * {@code UsuarioController} con {@code @PreAuthorize}).
	 *
	 * @param data datos del nuevo administrador; el rol enviado se ignora
	 * @param idActor administrador que lo crea
	 * @return los mismos códigos de {@link #create(UsuarioDTO)}, excepto
	 *         {@link #ROL_NO_PERMITIDO}
	 */
	public int crearAdministrador(UsuarioDTO data, Long idActor) {
		return registrar(data, Role.ADMIN, idActor, AuditoriaService.CREACION_ADMINISTRADOR);
	}

	/**
	 * Validaciones y guardado comunes a {@link #create(UsuarioDTO)} y
	 * {@link #crearAdministrador(UsuarioDTO, Long)}.
	 *
	 * @param data datos del usuario
	 * @param rol rol con el que se crea
	 * @param idActor usuario que lo crea; {@code null} en el registro público
	 * @param accion acción que se registra en la auditoría
	 * @return código del resultado
	 */
	private int registrar(UsuarioDTO data, Role rol, Long idActor, String accion) {
		if (data == null || esVacio(data.getNombre()) || esVacio(data.getCorreo()) || esVacio(data.getContrasenia())
				|| !telefonoValido(data.getTelefono())
				|| LimitesTexto.excede(data.getNombre(), LimitesTexto.CORTO)
				|| LimitesTexto.excede(data.getCorreo(), LimitesTexto.CORREO)) {
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

		// Correo de bienvenida con el código de verificación
		verificacionCorreoService.enviarCodigo(usuario, correo, data.getNombre().trim(), true);

		return CREADO;
	}

	/**
	 * Todos los usuarios, sin paginar.
	 *
	 * @return lista de usuarios
	 */
	@Override
	public List<UsuarioDTO> getAll() {
		List<Usuario> usuarios = usuarioRepo.findAll();
		List<UsuarioDTO> resultado = new ArrayList<>();

		for (Usuario usuario : usuarios) {
			resultado.add(convertirDTO(usuario));
		}

		return resultado;
	}

	/**
	 * Indica si existe un usuario con ese id.
	 *
	 * @param id identificador
	 * @return {@code true} si existe
	 */
	public boolean existe(Long id) {
		return usuarioRepo.existsById(id);
	}

	/**
	 * Busca un usuario por su id.
	 *
	 * @param id identificador
	 * @return el usuario, o {@code null} si no existe
	 */
	@Override
	public UsuarioDTO getById(Long id) {
		Optional<Usuario> usuario = usuarioRepo.findById(id);

		if (usuario.isEmpty()) {
			return null;
		}

		return convertirDTO(usuario.get());
	}

	/**
	 * Convierte un usuario a DTO: descifra el correo y quita la contraseña, el
	 * hash del correo y el secreto del segundo factor.
	 *
	 * @param usuario usuario
	 * @return usuario para la API
	 */
	private UsuarioDTO convertirDTO(Usuario usuario) {
		UsuarioDTO dto = modelMapper.map(usuario, UsuarioDTO.class);
		dto.setIdUsuario(usuario.getIdUsuario());
		dto.setNombre(usuario.getNombre());
		dto.setCorreo(AESUtil.decrypt(usuario.getCorreo()));
		dto.setContrasenia(null);
		dto.setCorreoHash(null);
		dto.setSecretoTotp(null);

		return dto;
	}

	/**
	 * Borra un usuario.
	 *
	 * @param id identificador
	 * @return 0 si se borró; 1 si no existe
	 */
	@Override
	public int deleteById(Long id) {
		if (!usuarioRepo.existsById(id)) {
			return 1;
		}

		usuarioRepo.deleteById(id);
		return 0;
	}

	/**
	 * Actualiza nombre, correo, contraseña o rol de un usuario; los campos
	 * vacíos no se tocan. Si el correo nuevo ya es de otro usuario, no se
	 * cambia.
	 *
	 * @param id identificador
	 * @param newData datos nuevos
	 * @return 0 si se actualizó; 1 si no existe; 2 si el correo está ocupado o
	 *         falló el guardado
	 */
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

	/**
	 * Cantidad de usuarios.
	 *
	 * @return cantidad
	 */
	@Override
	public long count() {
		return usuarioRepo.count();
	}

	/**
	 * Indica si existe un usuario con ese id.
	 *
	 * @param id identificador
	 * @return {@code true} si existe
	 */
	@Override
	public boolean exist(Long id) {
		return usuarioRepo.existsById(id);
	}

	/**
	 * Indica si el correo del usuario ya está registrado (compara por el
	 * hash).
	 *
	 * @param newUser usuario con el hash del correo
	 * @return {@code true} si el correo ya está registrado
	 */
	@Override
	public boolean findUsernameAlreadyTaken(Usuario newUser) {
		return newUser.getCorreoHash() != null && usuarioRepo.existsByCorreoHash(newUser.getCorreoHash());
	}

	/**
	 * Indica si un correo ya está registrado.
	 *
	 * @param username correo a buscar
	 * @return {@code true} si existe
	 */
	@Override
	public boolean encontrarCorreoExitente(String username) {
		return !esVacio(username) && usuarioRepo.existsByCorreoHash(hashCorreo(username));
	}

	/**
	 * Busca un usuario por su correo.
	 *
	 * @param correo correo a buscar
	 * @return el usuario, o {@code null} si no existe o el correo está vacío
	 */
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
	 * @param correo correo del usuario autenticado (sale del token)
	 * @return 0 si se desactivó; 1 si el usuario no existe; 2 si es ADMIN o la
	 *         cuenta no está ACTIVO
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
	 * Actualiza el perfil propio. Solo se pueden cambiar el nombre y el
	 * teléfono; el correo, el rol, el estado y la contraseña no se tocan por
	 * esta vía. Los campos que lleguen en {@code null} se dejan como están.
	 *
	 * @param correo correo del usuario autenticado (sale del token)
	 * @param nombre nombre nuevo; si se envía no puede estar vacío
	 * @param telefono teléfono nuevo: 7 a 15 dígitos, opcionalmente con + y
	 *                 separadores (espacios, guiones, puntos o paréntesis)
	 * @return 0 si se actualizó; 1 si el usuario no existe; 2 si los datos no
	 *         son válidos o no hay nada que actualizar
	 */
	public int actualizarPerfil(String correo, String nombre, String telefono) {
		if ((nombre == null && telefono == null) || (nombre != null && nombre.isBlank())
				|| LimitesTexto.excede(nombre, LimitesTexto.CORTO)
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
	 * @param pagina página y orden
	 * @return página de usuarios
	 */
	public PaginaDTO<UsuarioDTO> listarPaginado(Pageable pagina) {
		return PaginaDTO.de(usuarioRepo.findAll(pagina), this::convertirDTO);
	}

	/**
	 * Restringe una cuenta con un motivo (administrador). Una cuenta
	 * restringida no puede iniciar sesión, sus tokens dejan de funcionar y no
	 * puede reactivarse sola. No se puede restringir a un administrador.
	 *
	 * @param idUsuario usuario a restringir
	 * @param motivo motivo (obligatorio); se le notifica al usuario
	 * @param idAdmin administrador que restringe
	 * @return el usuario; NO_ENCONTRADO si no existe; DATOS_INVALIDOS si falta
	 *         el motivo; SIN_PERMISO si es administrador
	 */
	@Transactional
	public Resultado<UsuarioDTO> restringir(Long idUsuario, String motivo, Long idAdmin) {
		if (esVacio(motivo)) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El motivo de la restricción es obligatorio.");
		}
		if (LimitesTexto.excede(motivo, LimitesTexto.MOTIVO)) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, LimitesTexto.MOTIVO_LARGO);
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
	 * Activa una cuenta restringida o inactiva (administrador). La cuenta
	 * queda ACTIVA.
	 * <p>
	 * Decisión: el administrador también puede activar una cuenta que su dueño
	 * desactivó (INACTIVO), además de levantar una restricción.
	 * </p>
	 * <p>
	 * Decisión: el motivo es opcional. Las acciones que castigan (restringir,
	 * bloquear, rechazar, ocultar) exigen motivo porque se le comunica al
	 * afectado y respalda la decisión en la auditoría; devolver el acceso no
	 * perjudica a nadie, así que el motivo solo se guarda si se envía.
	 * </p>
	 *
	 * @param idUsuario usuario a activar
	 * @param motivo motivo opcional; si se envía queda en la auditoría
	 * @param idAdmin administrador que la activa
	 * @return el usuario; NO_ENCONTRADO si no existe; ESTADO_INVALIDO si ya
	 *         estaba activa
	 */
	@Transactional
	public Resultado<UsuarioDTO> liberar(Long idUsuario, String motivo, Long idAdmin) {
		Optional<Usuario> usuario = usuarioRepo.findById(idUsuario);
		if (usuario.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "Usuario no encontrado.");
		}
		if (usuario.get().getEstado() == EstadoUsuario.ACTIVO) {
			return Resultado.error(Resultado.ESTADO_INVALIDO, "La cuenta ya está activa.");
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
	 * opcionalmente con + al inicio y con separadores (espacios, guiones,
	 * puntos o paréntesis). Vacío equivale a no enviarlo.
	 *
	 * @param telefono teléfono a revisar
	 * @return {@code true} si es válido o está vacío
	 */
	private static boolean telefonoValido(String telefono) {
		return esVacio(telefono) || PATRON_TELEFONO.matcher(normalizarTelefono(telefono)).matches();
	}

	/**
	 * Registra en la auditoría una operación de usuario rechazada.
	 *
	 * @param idActor usuario que la intentó
	 * @param accion acción auditada
	 * @param motivo causa del rechazo
	 */
	private void auditarRechazo(Long idActor, String accion, String motivo) {
		auditoriaService.registrar(idActor, accion, AuditoriaService.ENTIDAD_USUARIO, null,
				ResultadoAuditoria.RECHAZADO, motivo);
	}

	/**
	 * Indica si un texto es {@code null} o solo tiene espacios.
	 *
	 * @param texto texto a revisar
	 * @return {@code true} si está vacío
	 */
	private static boolean esVacio(String texto) {
		return texto == null || texto.isBlank();
	}
}
