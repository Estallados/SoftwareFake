package co.edu.unbosque.marketplace.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.service.UsuarioService;
import co.edu.unbosque.marketplace.util.AESUtil;

/**
 * Crea el administrador inicial al arrancar. El registro público nunca permite
 * el rol ADMIN, así que esta es la única forma de tener el primero.
 * <p>
 * Solo actúa si no existe ningún ADMIN y la variable ADMIN_PASSWORD está
 * definida; así no queda una contraseña por defecto conocida.
 * </p>
 */
@Component
public class AdminInicializador implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(AdminInicializador.class);

	private final UsuarioRepository usuarioRepo;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.admin.email}")
	private String correo;

	@Value("${app.admin.password:}")
	private String contrasenia;

	@Value("${app.admin.full-name:Administrador}")
	private String nombre;

	public AdminInicializador(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
		this.usuarioRepo = usuarioRepo;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (usuarioRepo.existsByRol(Role.ADMIN)) {
			return;
		}
		if (contrasenia == null || contrasenia.isBlank()) {
			LOG.warn("No existe ningún administrador y ADMIN_PASSWORD no está definida: no se creó el admin inicial.");
			return;
		}

		String correoNormalizado = UsuarioService.normalizarCorreo(correo);
		String correoHash = AESUtil.hash(correoNormalizado);
		if (usuarioRepo.existsByCorreoHash(correoHash)) {
			LOG.warn("El correo {} ya pertenece a un usuario que no es ADMIN: no se creó el admin inicial.",
					correoNormalizado);
			return;
		}

		Usuario admin = new Usuario();
		admin.setNombre(AESUtil.encrypt(nombre));
		admin.setCorreo(AESUtil.encrypt(correoNormalizado));
		admin.setCorreoHash(correoHash);
		admin.setContrasenia(passwordEncoder.encode(contrasenia));
		admin.setRol(Role.ADMIN);
		admin.setEstado(EstadoUsuario.ACTIVO);
		usuarioRepo.save(admin);

		LOG.info("Administrador inicial creado: {}", correoNormalizado);
	}
}
