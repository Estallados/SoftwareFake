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

	/** Log de la clase. */
	private static final Logger LOG = LoggerFactory.getLogger(VerificadorConfiguracion.class);

	/** Propiedades y variables de entorno de la aplicación. */
	private final Environment environment;

	/** Para saber si Cloudinary está configurado. */
	private final CloudinaryService cloudinaryService;

	/**
	 * Crea el verificador.
	 *
	 * @param environment       entorno de Spring
	 * @param cloudinaryService cliente de Cloudinary
	 */
	public VerificadorConfiguracion(Environment environment, CloudinaryService cloudinaryService) {
		this.environment = environment;
		this.cloudinaryService = cloudinaryService;
	}

	/**
	 * Hace las revisiones al crear el bean:
	 * <ul>
	 * <li>{@code AES_SECRET_KEY} es obligatoria: si falta o no es válida, la
	 * aplicación no arranca, porque sin ella no se puede registrar ni iniciar
	 * sesión.</li>
	 * <li>{@code MAIL_PASSWORD} y {@code CLOUDINARY_URL} son opcionales: si
	 * faltan, solo se deja un aviso en el log.</li>
	 * </ul>
	 *
	 * @throws IllegalStateException si {@code AES_SECRET_KEY} falta o no es válida
	 */
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
	 * Indica si hay contraseña de correo configurada.
	 * <p>
	 * No uso {@code @Value} porque {@code spring.mail.password} apunta a
	 * {@code ${MAIL_PASSWORD}}: si esa variable no existe, {@code @Value} lanza
	 * una excepción y la app no arranca. El correo es opcional, así que solo se
	 * avisa.
	 * </p>
	 *
	 * @return {@code true} si la contraseña de correo está definida
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
