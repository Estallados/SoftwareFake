package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.exception.CapitalException;
import co.edu.unbosque.marketplace.exception.CharacterException;
import co.edu.unbosque.marketplace.exception.ExceptionCheker;
import co.edu.unbosque.marketplace.exception.MailException;
import co.edu.unbosque.marketplace.exception.NumberException;
import co.edu.unbosque.marketplace.exception.SmallException;
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
	void debeRechazarContraseniaSinMayuscula_CA135() {
		assertThrows(CapitalException.class, () -> ExceptionCheker.checkerPasword("sinmayuscula#1"));
		assertFalse(ExceptionCheker.contraseniaValida("sinmayuscula#1"));
	}

	@Test
	void debeRechazarContraseniaSinMinuscula_CA135() {
		assertThrows(SmallException.class, () -> ExceptionCheker.checkerPasword("SINMINUSCULA#1"));
		assertFalse(ExceptionCheker.contraseniaValida("SINMINUSCULA#1"));
	}

	@Test
	void debeAceptarContraseniaQueCumpleTodasLasReglas_CA135() {
		assertTrue(ExceptionCheker.contraseniaValida("Segura#2026"));
		assertFalse(ExceptionCheker.contraseniaValida(null));
	}

	@Test
	void validaFormatoDeCorreo() {
		assertDoesNotThrow(() -> ExceptionCheker.checkerMail("ana.perez@correo.com"));
		assertThrows(MailException.class, () -> ExceptionCheker.checkerMail("sin-arroba.com"));
		assertThrows(MailException.class, () -> ExceptionCheker.checkerMail("ana@correo"));
	}
}
