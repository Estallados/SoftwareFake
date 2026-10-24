package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.exception.CharacterException;
import co.edu.unbosque.marketplace.exception.ExceptionCheker;
import co.edu.unbosque.marketplace.exception.MailException;
import co.edu.unbosque.marketplace.exception.NumberException;
import co.edu.unbosque.marketplace.exception.SymbolException;

/** Reglas de validación que usa el registro de usuarios. */
class ExceptionChekerTest {

	@Test
	void aceptaContraseniaSegura() {
		assertDoesNotThrow(() -> ExceptionCheker.checkerPasword("Segura#2026"));
	}

	@Test
	void rechazaContraseniasDebiles() {
		assertThrows(CharacterException.class, () -> ExceptionCheker.checkerPasword("Ab1#"));
		assertThrows(NumberException.class, () -> ExceptionCheker.checkerPasword("SinNumero#abc"));
		assertThrows(SymbolException.class, () -> ExceptionCheker.checkerPasword("SinSimbolo123"));
	}

	@Test
	void validaFormatoDeCorreo() {
		assertDoesNotThrow(() -> ExceptionCheker.checkerMail("ana.perez@correo.com"));
		assertThrows(MailException.class, () -> ExceptionCheker.checkerMail("sin-arroba.com"));
		assertThrows(MailException.class, () -> ExceptionCheker.checkerMail("ana@correo"));
	}
}
