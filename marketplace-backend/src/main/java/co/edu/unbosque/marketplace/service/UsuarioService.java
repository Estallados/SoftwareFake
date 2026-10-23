package co.edu.unbosque.marketplace.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.dto.UsuarioDTO;

/**
 * Servicio para la gestión de usuarios en el sistema. Implementa operaciones
 * CRUD y lógica de negocio relacionada con usuarios, como registro,
 * autenticación, verificación de correos y generación de códigos.
 */
@Service
public class UsuarioService implements CRUDOperation<UsuarioDTO> {

	/**
	 * Mapa en memoria para almacenar temporalmente los códigos de verificación
	 * generados para los usuarios. La clave es el correo del usuario y el valor es
	 * el código de verificación.
	 */
	private Map<String, String> codigosEnMemoria = new HashMap<>();

	/**
	 * Repositorio de Spring Data JPA para realizar operaciones CRUD y consultas
	 * sobre la entidad Usuario.
	 */
	@Autowired
	private UsuarioRepository usuarioRepo;

	/**
	 * Utilidad para el envío de correos electrónicos.
	 */
	@Autowired
	private CorreoUtil correoUtil;

	/**
	 * Codificador de contraseñas para almacenar de forma segura las contraseñas de
	 * los usuarios.
	 */
	@Autowired
	private PasswordEncoder passwordEncoder;

	/** Constructor */
	public UsuarioService() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Crea un nuevo usuario en el sistema.
	 *
	 * @param data Datos del usuario a crear.
	 * @return 0 si el usuario fue creado exitosamente, 1 si el correo ya está
	 *         registrado, 2 si hay un error en los datos o en el proceso.
	 */
	@Override
	public int create(UsuarioDTO data) {
		try {
			if (data.getCorreo() == null || data.getCorreo().trim().isEmpty()) {
				return 2;
			}
			if (data.getContrasenia() == null || data.getContrasenia().trim().isEmpty()) {
				return 2;
			}
			String codigo = generarCodigo(6);
			Usuario usuario = new Usuario();
			usuario.setNombre(AESUtil.encrypt(data.getNombre()));
			usuario.setCorreo(AESUtil.encrypt(data.getCorreo()));
			usuario.setContrasenia(passwordEncoder.encode(data.getContrasenia()));
			usuario.setValidarCodigo(false);

			if (data.getRole() != null) {
				usuario.setRole(data.getRole());
			}
			if (findUsernameAlreadyTaken(usuario)) {
				return 1;
			} else {
				codigosEnMemoria.put(data.getCorreo(), codigo);
				usuarioRepo.save(usuario);
				String cuerpo = String.format("Hola %s,\n\n"
						+ "Gracias por registrarte en la aplicación Detector de IA.\n"
						+ "Para completar tu registro y activar tu cuenta, utiliza el siguiente código de verificación:\n\n"
						+ "   CÓDIGO: %s\n\n" + "El equipo de Detector de IA", data.getNombre(), codigo);
				correoUtil.enviarCorreo(data.getCorreo(), "Código de verificación de la app detector de IA", cuerpo);
				return 0;
			}
		} catch (Exception e) {
			return 2;
		}
	}

	/**
	 * Obtiene la lista de todos los usuarios registrados.
	 *
	 * @return Lista de usuarios.
	 */
	@Override
	public List<UsuarioDTO> getAll() {
		List<Usuario> entityList = usuarioRepo.findAll();
		List<UsuarioDTO> dtoList = new ArrayList<>();
		for (Usuario entity : entityList) {
			UsuarioDTO dto = new UsuarioDTO();
			dto.setId(entity.getId());
			dto.setNombre(AESUtil.decrypt(entity.getNombre()));
			dto.setCorreo(AESUtil.decrypt(entity.getCorreo()));
			dto.setRole(entity.getRole());

			dtoList.add(dto);
		}
		return dtoList;
	}

	/**
	 * Obtiene un usuario por su ID.
	 *
	 * @param id ID del usuario.
	 * @return DTO del usuario encontrado.
	 */
	@Override
	public UsuarioDTO getById(Long id) {
		Optional<Usuario> optionalUsuario = usuarioRepo.findById(id);
		if (optionalUsuario.isEmpty()) {
			return null;
		}
		Usuario entity = optionalUsuario.get();
		UsuarioDTO dto = new UsuarioDTO();
		dto.setId(entity.getId());
		dto.setNombre(AESUtil.decrypt(entity.getNombre()));
		dto.setCorreo(AESUtil.decrypt(entity.getCorreo()));
		dto.setRole(entity.getRole());

		return dto;
	}

	/**
	 * Elimina un usuario del sistema por su ID.
	 *
	 * @param id ID del usuario a eliminar.
	 * @return 0 si el usuario fue eliminado exitosamente, 1 si el usuario no
	 *         existe.
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
	 * Actualiza los datos de un usuario por su ID.
	 *
	 * @param id      ID del usuario a actualizar.
	 * @param newData Nuevos datos del usuario.
	 * @return 0 si el usuario fue actualizado exitosamente, 1 si el usuario no
	 *         existe, 2 si hay un error en los datos.
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
				usuario.setNombre(AESUtil.encrypt(newData.getNombre()));
			}
			if (newData.getCorreo() != null) {
				usuario.setCorreo(AESUtil.encrypt(newData.getCorreo()));
			}
			if (newData.getContrasenia() != null) {
				usuario.setContrasenia(passwordEncoder.encode(newData.getContrasenia()));
			}
			if (newData.getRole() != null) {
				usuario.setRole(newData.getRole());
			}
			usuarioRepo.save(usuario);
			return 0;
		} catch (Exception e) {
			return 2;
		}
	}

	/**
	 * Cuenta el número total de usuarios registrados en el sistema.
	 *
	 * @return Número total de usuarios.
	 */
	@Override
	public long count() {
		return usuarioRepo.count();
	}

	/**
	 * Verifica si existe un usuario con el ID especificado.
	 *
	 * @param id ID del usuario a verificar.
	 * @return true si el usuario existe, false en caso contrario.
	 */
	@Override
	public boolean exist(Long id) {
		return usuarioRepo.existsById(id);
	}

	/**
	 * Genera un código aleatorio de la longitud especificada.
	 *
	 * @param longitud Longitud del código a generar.
	 * @return Código generado.
	 */
	public String generarCodigo(int longitud) {
		String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder codigo = new StringBuilder();
		Random rnd = new Random();
		for (int i = 0; i < longitud; i++) {
			int index = rnd.nextInt(caracteres.length());
			codigo.append(caracteres.charAt(index));
		}
		return codigo.toString();
	}

	/**
	 * Autentica a un usuario en el sistema.
	 *
	 * @param correo   Correo del usuario.
	 * @param password Contraseña del usuario.
	 * @return 0 si la autenticación es exitosa, 1 si el usuario no existe, 2 si la
	 *         contraseña es incorrecta, 3 si hay un error en el proceso.
	 */
	public int autenticarUsuario(String correo, String password) {
		try {
			if (correo == null || correo.trim().isEmpty()) {
				return 1;
			}

			if (password == null || password.trim().isEmpty()) {
				return 2;
			}

			String encryptedUsername = AESUtil.encrypt(correo);
			Optional<Usuario> usuarioFound = usuarioRepo.findByCorreo(encryptedUsername);

			if (usuarioFound.isEmpty()) {
				return 1;
			}

			Usuario usuario = usuarioFound.get();
			String decryptedPassword = AESUtil.decrypt(usuario.getContrasenia());

			if (password.equals(decryptedPassword)) {
				return 0;
			} else {
				return 2;
			}

		} catch (Exception e) {
			return 3;
		}
	}

	/**
	 * Verifica si un correo de usuario ya está registrado.
	 *
	 * @param newUser Usuario con el correo a verificar.
	 * @return true si el correo ya está registrado, false en caso contrario.
	 */
	@Override
	public boolean findUsernameAlreadyTaken(Usuario newUser) {
		try {
			Optional<Usuario> found = usuarioRepo.findByCorreo(newUser.getCorreo());
			return found.isPresent();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Genera un nuevo código de verificación para un correo.
	 *
	 * @param correo Correo del usuario.
	 * @return true si el código fue generado y enviado exitosamente.
	 */
	public boolean generarNuevoCodigo(String correo) {
		String codigo = generarCodigo(6);
		codigosEnMemoria.put(correo, codigo);
		correoUtil.enviarCorreo(correo, "Nuevo código", "Tu nuevo código es: " + codigo);
		return true;
	}

	/**
	 * Verifica si un código de verificación es correcto.
	 *
	 * @param correo Correo del usuario.
	 * @param codigo Código de verificación.
	 * @return 0 si el código es correcto, 1 si el código no ha sido generado o el
	 *         usuario no existe, 2 si el código es incorrecto.
	 */
	public int verificarCodigo(String correo, String codigo) {
		String codigoEsperado = codigosEnMemoria.get(correo);

		if (codigoEsperado == null) {
			return 1;
		}
		if (!codigoEsperado.trim().equals(codigo.trim())) {
			return 2;
		}
		Usuario usuario = usuarioRepo.findByCorreo(AESUtil.encrypt(correo)).orElse(null);
		if (usuario == null) {
			return 1;
		}
		usuario.setValidarCodigo(true);
		usuarioRepo.save(usuario);
		codigosEnMemoria.remove(correo);
		return 0;
	}

	/**
	 * Verifica si un correo ya existe en el sistema.
	 *
	 * @param username Correo a verificar.
	 * @return true si el correo existe, false en caso contrario.
	 */
	@Override
	public boolean encontrarCorreoExitente(String username) {
		Optional<Usuario> found = usuarioRepo.findByCorreo(AESUtil.encrypt(username));
		return found.isPresent();
	}

	/**
	 * Obtiene un usuario por su correo.
	 *
	 * @param correo Correo del usuario.
	 * @return Usuario encontrado o null si no existe.
	 */
	public Usuario obtenerUsuarioPorCorreo(String correo) {
		return usuarioRepo.findByCorreo(AESUtil.encrypt(correo)).orElse(null);
	}

	/**
	 * Verifica si un usuario ha confirmado su cuenta mediante código de
	 * verificación.
	 *
	 * @param correo Correo del usuario a verificar.
	 * @return true si el usuario existe y su cuenta está verificada, false en caso
	 *         contrario.
	 */
	public boolean isVerified(String correo) {
		Usuario usuario = obtenerUsuarioPorCorreo(correo);
		return usuario != null && usuario.isValidarCodigo();
	}

}
