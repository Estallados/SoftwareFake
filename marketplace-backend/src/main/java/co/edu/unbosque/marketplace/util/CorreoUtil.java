package co.edu.unbosque.marketplace.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Utilidad para el envío de correos electrónicos utilizando Spring Mail.
 * Proporciona un método sencillo para enviar correos electrónicos de texto
 * plano.
 */
@Service
public class CorreoUtil {

	private static final Logger LOG = LoggerFactory.getLogger(CorreoUtil.class);

	/** Servicio de Spring para el envío de correos electrónicos. */
	private final JavaMailSender mailSender;

	/**
	 * Cuenta que envía los correos; Gmail exige que coincida con la autenticada.
	 */
	private final String remitente;

	/**
	 * Constructor con inyección de dependencias.
	 *
	 * @param mailSender servicio de envío de correos
	 * @param remitente  cuenta remitente ({@code spring.mail.username})
	 */
	public CorreoUtil(JavaMailSender mailSender, @Value("${spring.mail.username:}") String remitente) {
		this.mailSender = mailSender;
		this.remitente = remitente;
	}

	/**
	 * Envía un correo electrónico a un destinatario específico. Si falla, lo
	 * registra en el log y devuelve {@code false}; nunca lanza excepción, para que
	 * un problema de correo no impida, por ejemplo, crear la cuenta.
	 *
	 * @param to      Destinatario del correo electrónico.
	 * @param subject Asunto del correo electrónico.
	 * @param body    Cuerpo del correo electrónico.
	 * @return {@code true} si el correo se envió correctamente, {@code false} si
	 *         ocurrió un error durante el envío.
	 */
	public boolean enviarCorreo(String to, String subject, String body) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			if (remitente != null && !remitente.isBlank()) {
				message.setFrom(remitente);
			}
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
			return true;
		} catch (MailAuthenticationException e) {
			LOG.error("El servidor de correo rechazó el usuario/contraseña. Revisa la variable de entorno "
					+ "MAIL_PASSWORD (debe ser una contraseña de aplicación de Gmail): {}", e.getMessage());
			return false;
		} catch (MailException e) {
			LOG.error("Error enviando correo a {}", to, e);
			return false;
		}
	}
}
