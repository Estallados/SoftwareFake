
package co.edu.unbosque.marketplace.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.exception.CharacterException;
import co.edu.unbosque.marketplace.exception.ExceptionCheker;
import co.edu.unbosque.marketplace.exception.MailException;
import co.edu.unbosque.marketplace.exception.NumberException;
import co.edu.unbosque.marketplace.exception.SymbolException;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;
import co.edu.unbosque.marketplace.util.CorreoUtil;

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

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CorreoUtil correoUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

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
     * Registra un usuario nuevo. La cuenta queda ACTIVA de inmediato. Solo se
     * permiten los roles HUESPED (por defecto) y ANFITRION; nunca ADMIN.
     *
     * @return {@link #CREADO}, {@link #CORREO_DUPLICADO}, {@link #DATOS_INVALIDOS},
     *         {@link #CONTRASENIA_DEBIL}, {@link #ROL_NO_PERMITIDO} o
     *         {@link #ERROR_INTERNO}
     */
    @Override
    public int create(UsuarioDTO data) {
        if (data == null || esVacio(data.getNombre()) || esVacio(data.getCorreo())
                || esVacio(data.getContrasenia())) {
            return DATOS_INVALIDOS;
        }

        String correo = normalizarCorreo(data.getCorreo());

        try {
            ExceptionCheker.checkerMail(correo);
        } catch (MailException e) {
            return DATOS_INVALIDOS;
        }

        try {
            ExceptionCheker.checkerPasword(data.getContrasenia());
        } catch (CharacterException | NumberException | SymbolException e) {
            return CONTRASENIA_DEBIL;
        }

        Role rol = data.getRol() == null ? Role.HUESPED : data.getRol();
        if (rol == Role.ADMIN) {
            return ROL_NO_PERMITIDO;
        }

        try {
            String correoHash = AESUtil.hash(correo);
            if (usuarioRepo.existsByCorreoHash(correoHash)) {
                return CORREO_DUPLICADO;
            }

            // Se arma la entidad campo por campo (sin ModelMapper) para que el
            // cliente no pueda fijar id, estado ni otros campos internos.
            Usuario usuario = new Usuario();
            usuario.setNombre(AESUtil.encrypt(data.getNombre().trim()));
            usuario.setCorreo(AESUtil.encrypt(correo));
            usuario.setCorreoHash(correoHash);
            usuario.setTelefono(esVacio(data.getTelefono()) ? null : data.getTelefono().trim());
            usuario.setContrasenia(passwordEncoder.encode(data.getContrasenia()));
            usuario.setRol(rol);
            usuario.setEstado(EstadoUsuario.ACTIVO);

            usuarioRepo.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: lo frena el índice único
            return CORREO_DUPLICADO;
        } catch (Exception e) {
            LOG.error("Error al registrar el usuario", e);
            return ERROR_INTERNO;
        }

        String cuerpo = String.format(
                "Hola %s,\n\n"
                + "¡Bienvenido al Marketplace de Alojamientos de Inversiones LR!\n\n"
                + "Nos alegra que formes parte de nuestra plataforma, "
                + "donde podrás descubrir, comparar y reservar alojamientos, "
                + "o publicar tus propiedades como anfitrión.\n\n"
                + "Gracias por confiar en nosotros.\n\n"
                + "Saludos,\n"
                + "Equipo Marketplace Inversiones LR",
                data.getNombre().trim());

        correoUtil.enviarCorreo(
                correo,
                "Bienvenido al Marketplace de Inversiones LR",
                cuerpo);

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
        dto.setNombre(AESUtil.decrypt(usuario.getNombre()));
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
                usuario.setNombre(AESUtil.encrypt(newData.getNombre()));
            }

            if (!esVacio(newData.getCorreo())) {
                String correo = normalizarCorreo(newData.getCorreo());
                String correoHash = AESUtil.hash(correo);

                Optional<Usuario> existente =
                        usuarioRepo.findByCorreoHash(correoHash);

                if (existente.isPresent()
                        && !existente.get().getIdUsuario().equals(id)) {
                    return 2;
                }

                usuario.setCorreo(AESUtil.encrypt(correo));
                usuario.setCorreoHash(correoHash);
            }

            if (!esVacio(newData.getContrasenia())) {
                usuario.setContrasenia(
                        passwordEncoder.encode(newData.getContrasenia()));
            }

            if (newData.getRol() != null) {
                usuario.setRol(newData.getRol());
            }

            usuarioRepo.save(usuario);
            return 0;

        } catch (Exception e) {
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
        return newUser.getCorreoHash() != null
                && usuarioRepo.existsByCorreoHash(newUser.getCorreoHash());
    }

    @Override
    public boolean encontrarCorreoExitente(String username) {
        return !esVacio(username)
                && usuarioRepo.existsByCorreoHash(hashCorreo(username));
    }

    public UsuarioDTO obtenerUsuarioPorCorreo(String correo) {
        if (esVacio(correo)) {
            return null;
        }

        return usuarioRepo.findByCorreoHash(hashCorreo(correo))
                .map(this::convertirDTO)
                .orElse(null);
    }

	public int desactivarCuenta(String correo) {
	    try {
	        if (esVacio(correo)) {
	            return 1;
	        }

	        Optional<Usuario> usuarioOpt =
	                usuarioRepo.findByCorreoHash(hashCorreo(correo));

	        if (usuarioOpt.isEmpty()) {
	            return 1;
	        }

	        Usuario usuario = usuarioOpt.get();

	        if (usuario.getRol() == Role.ADMIN) {
	            return 2;
	        }

	        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
	            return 2;
	        }

	        usuario.setEstado(EstadoUsuario.INACTIVO);
	        usuarioRepo.save(usuario);

	        return 0;

	    } catch (Exception e) {
			LOG.error("Error al desactivar la cuenta", e);
			return 3;
		}
	}

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
