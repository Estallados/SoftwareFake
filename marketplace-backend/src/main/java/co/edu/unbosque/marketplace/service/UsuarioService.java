
package co.edu.unbosque.marketplace.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.dto.UsuarioDTO;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;
import co.edu.unbosque.marketplace.util.CorreoUtil;

@Service
public class UsuarioService implements CRUDOperation<UsuarioDTO> {

    private final Map<String, String> codigosEnMemoria = new HashMap<>();

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CorreoUtil correoUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    private static final Random RND = new Random();

    @Override
    public int create(UsuarioDTO data) {
        try {
            if (data.getCorreo() == null || data.getCorreo().trim().isEmpty()
                    || data.getContrasenia() == null
                    || data.getContrasenia().trim().isEmpty()) {
                return 2;
            }

            Usuario usuario = modelMapper.map(data, Usuario.class);

            usuario.setNombre(AESUtil.encrypt(data.getNombre()));
            usuario.setCorreo(AESUtil.encrypt(data.getCorreo()));
            usuario.setContrasenia(
                    passwordEncoder.encode(data.getContrasenia()));

            if (findUsernameAlreadyTaken(usuario)) {
                return 1;
            }

            String codigo = generarCodigo(6);
            codigosEnMemoria.put(data.getCorreo(), codigo);

            usuarioRepo.save(usuario);

            String cuerpo = String.format(
                    "Hola %s,\n\n" +
                    "¡Bienvenido al Marketplace de Alojamientos de Inversiones LR!\n\n" +
                    "Nos alegra que formes parte de nuestra plataforma, donde podrás descubrir, comparar y reservar alojamientos, o publicar tus propiedades como anfitrión.\n\n" +
                    "Gracias por confiar en nosotros.\n\n" +
                    "Saludos,\n" +
                    "Equipo Marketplace Inversiones LR",
                    data.getNombre());

            correoUtil.enviarCorreo(
                    data.getCorreo(),
                    "Código de verificación",
                    cuerpo);

            return 0;

        } catch (Exception e) {
            return 2;
        }
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

            if (newData.getCorreo() != null
                    && !newData.getCorreo().trim().isEmpty()) {

                String correoEncriptado =
                        AESUtil.encrypt(newData.getCorreo());

                Optional<Usuario> existente =
                        usuarioRepo.findByCorreo(correoEncriptado);

                if (existente.isPresent()
                        && !existente.get().getIdUsuario().equals(id)) {
                    return 2;
                }

                usuario.setCorreo(correoEncriptado);
            }

            if (newData.getContrasenia() != null
                    && !newData.getContrasenia().trim().isEmpty()) {
                usuario.setContrasenia(
                        passwordEncoder.encode(newData.getContrasenia()));
            }

            if (newData.getRol() != null) {
                usuario.setRol(newData.getRol());
            }

            usuarioRepo.save(usuario);
            return 0;

        } catch (Exception e) {
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

    public String generarCodigo(int longitud) {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder codigo = new StringBuilder();

        for (int i = 0; i < longitud; i++) {
            int index = RND.nextInt(caracteres.length());
            codigo.append(caracteres.charAt(index));
        }

        return codigo.toString();
    }

    public int autenticarUsuario(String correo, String password) {
        try {
            if (correo == null || correo.trim().isEmpty()) {
                return 1;
            }

            if (password == null || password.trim().isEmpty()) {
                return 2;
            }

            String correoEncriptado = AESUtil.encrypt(correo);

            Optional<Usuario> usuarioFound =
                    usuarioRepo.findByCorreo(correoEncriptado);

            if (usuarioFound.isEmpty()) {
                return 1;
            }

            Usuario usuario = usuarioFound.get();

            if (passwordEncoder.matches(
                    password, usuario.getContrasenia())) {
                return 0;
            }

            return 2;

        } catch (Exception e) {
            return 3;
        }
    }

    @Override
    public boolean findUsernameAlreadyTaken(Usuario newUser) {
        try {
            return usuarioRepo.findByCorreo(newUser.getCorreo()).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean encontrarCorreoExitente(String username) {
        return usuarioRepo.findByCorreo(
                AESUtil.encrypt(username)).isPresent();
    }

    public Usuario obtenerUsuarioPorCorreo(String correo) {
        return usuarioRepo.findByCorreo(
                AESUtil.encrypt(correo)).orElse(null);
    }

}
