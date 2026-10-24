package co.edu.unbosque.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Igual que application.properties en desarrollo: spring.mail.password apunta a
 * una variable de entorno que no existe. La app debe arrancar igual (el correo
 * es opcional).
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:sincorreo;DB_CLOSE_DELAY=-1",
		"spring.mail.password=${MAIL_PASSWORD_QUE_NO_EXISTE}" })
class ArranqueSinCorreoTest {

	@Test
	void arrancaAunqueFalteLaVariableDelCorreo() {
		// Si el contexto no levanta, este test falla antes de llegar aquí
	}
}
