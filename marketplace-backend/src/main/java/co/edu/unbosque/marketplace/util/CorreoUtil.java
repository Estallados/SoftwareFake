package co.edu.unbosque.marketplace.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Utilidad para el envío de correos electrónicos utilizando Spring Mail.
 * Proporciona un método sencillo para enviar correos electrónicos de texto plano.
 */
@Service
public class CorreoUtil {

    /** Inyecta el servicio de Spring para el envío de correos electrónicos. */
    @Autowired
    private JavaMailSender mailSender;

    /**
     * Constructor por defecto de la clase.
     */
    public CorreoUtil() {
        // Constructor por defecto
    }

    /**
     * Envía un correo electrónico a un destinatario específico.
     *
     * @param to Destinatario del correo electrónico.
     * @param subject Asunto del correo electrónico.
     * @param body Cuerpo del correo electrónico.
     * @return {@code true} si el correo se envió correctamente, {@code false} si ocurrió un error durante el envío.
     */
    public boolean enviarCorreo(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("Error enviando correo a " + to + ": " + e.getMessage());
            return false;
        }
    }
}
