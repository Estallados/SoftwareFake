package co.edu.unbosque.marketplace;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import co.edu.unbosque.marketplace.service.UsuarioService;

/**
 * Lee los correos que la aplicación "envió" al {@link JavaMailSender} simulado
 * de las pruebas, para sacar el código de verificación del registro.
 */
final class CorreoPrueba {

	/** El código va solo en su línea, con sangría (ver VerificacionCorreoService). */
	private static final Pattern CODIGO = Pattern.compile("(?m)^\\s+(\\d{6})\\s*$");

	private CorreoPrueba() {
	}

	/**
	 * Inicia sesión en dos pasos: con la contraseña se recibe el código por
	 * correo (401 con requiereCodigoCorreo) y con ese código se obtiene el token.
	 * El administrador no recibe código: su primer intento ya responde 200.
	 *
	 * @return el JSON de la respuesta 200 (con token, rol y correo)
	 */
	static String iniciarSesion(MockMvc mvc, JavaMailSender mailSender, String correo, String contrasenia)
			throws Exception {
		String credenciales = "\"correo\":\"" + correo + "\",\"contrasenia\":\"" + contrasenia + "\"";
		MvcResult primera = mvc
				.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{" + credenciales + "}"))
				.andReturn();
		if (primera.getResponse().getStatus() == 200) {
			return primera.getResponse().getContentAsString(StandardCharsets.UTF_8);
		}
		status().isUnauthorized().match(primera);
		jsonPath("$.requiereCodigoCorreo").value(true).match(primera);
		jsonPath("$.token").doesNotExist().match(primera);
		String codigo = codigoEnviadoA(mailSender, correo);
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{" + credenciales + ",\"codigo\":\"" + codigo + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	/** Último código de verificación enviado al correo indicado (sin importar mayúsculas). */
	static String codigoEnviadoA(JavaMailSender mailSender, String correo) {
		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender, atLeastOnce()).send(captor.capture());
		String destino = UsuarioService.normalizarCorreo(correo);
		List<SimpleMailMessage> enviados = captor.getAllValues().stream()
				.filter(m -> m.getTo() != null && Arrays.asList(m.getTo()).contains(destino)).toList();
		if (enviados.isEmpty()) {
			throw new AssertionError("No se envió ningún correo a " + correo);
		}
		Matcher codigo = CODIGO.matcher(enviados.get(enviados.size() - 1).getText());
		if (!codigo.find()) {
			throw new AssertionError("El correo enviado a " + correo + " no trae código de verificación");
		}
		return codigo.group(1);
	}
}
