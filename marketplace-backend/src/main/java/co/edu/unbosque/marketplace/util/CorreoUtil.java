package co.edu.unbosque.marketplace.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Utilidad para el envío de correos electrónicos utilizando Spring Mail.
 * Proporciona un método sencillo para enviar correos electrónicos de texto plano.
 */
@Service
public class CorreoUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoUtil.class);

    /** Inyecta el servicio de Spring para el envío de correos electrónicos. */
    @Autowired
    private JavaMailSender mailSender;

    /** Cuenta que envía los correos; Gmail exige que coincida con la autenticada. */
    @Value("${spring.mail.username:}")
    private String remitente;

    /**
     * Constructor por defecto de la clase.
     */
    public CorreoUtil() {
        // Constructor por defecto
    }

    /**
     * Envía un correo electrónico a un destinatario específico. Si falla, lo
     * registra en el log y devuelve {@code false}; nunca lanza excepción, para
     * que un problema de correo no impida, por ejemplo, crear la cuenta.
     *
     * @param to Destinatario del correo electrónico.
     * @param subject Asunto del correo electrónico.
     * @param body Cuerpo del correo electrónico.
     * @return {@code true} si el correo se envió correctamente, {@code false} si ocurrió un error durante el envío.
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
        } catch (Exception e) {
            LOG.error("Error enviando correo a {}", to, e);
            return false;
        }
    }
}
