package co.edu.unbosque.marketplace.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.service.CloudinaryService;
import co.edu.unbosque.marketplace.util.AESUtil;
import jakarta.annotation.PostConstruct;

/**
 * Revisa la configuración al arrancar para que los errores salgan aquí, con un
 * mensaje claro, y no después como un 500 al registrar un usuario.
 */
@Component
public class VerificadorConfiguracion {

	private static final Logger LOG = LoggerFactory.getLogger(VerificadorConfiguracion.class);

	private final Environment environment;
	private final CloudinaryService cloudinaryService;

	public VerificadorConfiguracion(Environment environment, CloudinaryService cloudinaryService) {
		this.environment = environment;
		this.cloudinaryService = cloudinaryService;
	}

	@PostConstruct
	public void verificar() {
		// Sin AES_SECRET_KEY no se puede registrar ni iniciar sesión: mejor no arrancar
		AESUtil.hash("verificacion");

		if (!correoConfigurado()) {
			LOG.warn("MAIL_PASSWORD no está definida (revisa que el nombre no tenga espacios). "
					+ "La app funciona, pero no se enviarán correos.");
		}
		if (!cloudinaryService.estaConfigurado()) {
			LOG.warn("CLOUDINARY_URL no está definida. La app funciona, pero subir imágenes responderá 503.");
		}
	}

	/**
	 * No se usa {@code @Value} porque spring.mail.password apunta a
	 * ${MAIL_PASSWORD}: si esa variable no existe, {@code @Value} lanza una
	 * excepción y la app no arranca. El correo es opcional, solo se avisa.
	 */
	private boolean correoConfigurado() {
		try {
			String contrasenia = environment.getProperty("spring.mail.password");
			return contrasenia != null && !contrasenia.isBlank();
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
