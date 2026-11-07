package co.edu.unbosque.marketplace.exception;

import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;

/**
 * Validaciones de datos de entrada, sobre todo de contraseñas.
 * <p>
 * Cada regla tiene su propio método y su propia excepción, para poder decirle
 * al usuario exactamente qué le falta a su contraseña. Una contraseña válida
 * tiene entre 8 y 64 caracteres, al menos una mayúscula, una minúscula, un
 * número y un símbolo.
 * </p>
 */
public class ExceptionCheker {
	
	/**
	 * Constructor de ExceptionCheker.
	 */
	public ExceptionCheker() {
	}
	

	/**
	 * Verifica que una contraseña cumpla todas las reglas. Se detiene en la
	 * primera que falle.
	 *
	 * @param a contraseña a validar
	 * @throws CharacterException si no tiene entre 8 y 64 caracteres
	 * @throws CapitalException   si no tiene ninguna mayúscula
	 * @throws SmallException     si no tiene ninguna minúscula
	 * @throws NumberException    si no tiene ningún número
	 * @throws SymbolException    si no tiene ningún símbolo
	 */
	public static void checkerPasword(String a)
			throws CharacterException, CapitalException, SmallException, NumberException, SymbolException {

		checkerCharacter(a);
		checkerCapital(a);
		checkerSmall(a);
		checkerNumber(a);
		checkerSymbol(a);

	}

	/**
	 * Indica si una contraseña cumple todas las reglas de
	 * {@link #checkerPasword(String)}, sin lanzar excepciones. Es la que uso en
	 * los servicios, donde solo necesito un sí o un no.
	 *
	 * @param a contraseña a validar; {@code null} no es válida
	 * @return {@code true} si la contraseña es válida
	 */
	public static boolean contraseniaValida(String a) {
		if (a == null) {
			return false;
		}
		try {
			checkerPasword(a);
			return true;
		} catch (CharacterException | CapitalException | SmallException | NumberException | SymbolException e) {
			return false;
		}
	}

	/**
	 * Verifica que la contraseña tenga al menos una letra mayúscula.
	 *
	 * @param a contraseña a validar
	 * @throws CapitalException si no tiene mayúsculas
	 */
	public static void checkerCapital(String a) throws CapitalException {
		if (a.chars().noneMatch(Character::isUpperCase)) {
			throw new CapitalException();
		}
	}

	/**
	 * Verifica que la contraseña tenga al menos una letra minúscula.
	 *
	 * @param a contraseña a validar
	 * @throws SmallException si no tiene minúsculas
	 */
	public static void checkerSmall(String a) throws SmallException {
		if (a.chars().noneMatch(Character::isLowerCase)) {
			throw new SmallException();
		}
	}

	/**
	 * Verifica que un texto tenga el formato de un correo: usuario, arroba,
	 * dominio y una extensión de al menos dos letras.
	 *
	 * @param a correo a validar
	 * @throws InputMismatchException no se lanza; se mantiene por compatibilidad
	 *                                con quienes ya llaman este método
	 * @throws MailException          si el formato no es de correo
	 */
	public static void checkerMail(String a) throws InputMismatchException, MailException {
		if (!a.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
			throw new MailException();
		}
	}

	/**
	 * Verifica que la contraseña tenga entre 8 y 64 caracteres y no pase de 72
	 * bytes. El máximo existe porque BCrypt solo usa los primeros 72 bytes: sin
	 * él, dos contraseñas distintas con el mismo comienzo abrirían la misma
	 * cuenta.
	 *
	 * @param a contraseña a validar
	 * @throws CharacterException si es muy corta o muy larga
	 */
	public static void checkerCharacter(String a) throws CharacterException {
		if (a.length() < 8 || a.length() > 64 || a.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new CharacterException();
		}
	}

	/**
	 * Verifica que la contraseña y su confirmación sean iguales.
	 *
	 * @param a contraseña
	 * @param b confirmación de la contraseña
	 * @throws EqualPasswordException si no coinciden
	 */
	public static void checkerEqualPassword(String a, String b) throws EqualPasswordException {
		if (!a.equals(b)) {
			throw new EqualPasswordException();
		}
	}

	/**
	 * Verifica que la contraseña tenga al menos un número.
	 *
	 * @param a contraseña a validar
	 * @throws NumberException si no tiene números
	 */
	public static void checkerNumber(String a) throws NumberException {
		if (!java.util.regex.Pattern.compile("\\d").matcher(a).find()) {
			throw new NumberException();
		}
	}

	/**
	 * Verifica que la contraseña tenga al menos un símbolo, es decir, un carácter
	 * que no sea letra ni número.
	 *
	 * @param a contraseña a validar
	 * @throws SymbolException si no tiene símbolos
	 */
	public static void checkerSymbol(String a) throws SymbolException {
		if (!java.util.regex.Pattern.compile("[^A-Za-z0-9]").matcher(a).find()) {
			throw new SymbolException();
		}
	}

	/**
	 * Verifica que un número no sea negativo.
	 *
	 * @param a número a validar
	 * @throws NegativeNumberException si es menor que cero
	 */
	public static void checkerNegativeNumber(int a) throws NegativeNumberException {
		if (a < 0) {
			throw new NegativeNumberException();
		}
	}

	/**
	 * Verifica que un texto tenga solo letras (se aceptan tildes y ñ).
	 *
	 * @param a texto a validar
	 * @throws TextException si tiene algo distinto de letras
	 */
	public static void checkerText(String a) throws TextException {
		if (!a.matches("[A-Za-záéíóúÁÉÍÓÚñÑ]+")) {
			throw new TextException();
		}
	}

	/**
	 * Método heredado de la primera versión del validador: siempre lanza
	 * {@link InputMismatchException}, sin importar el número recibido. No lo usa
	 * ningún flujo actual.
	 *
	 * @param a número recibido (no se evalúa)
	 * @throws InputMismatchException siempre
	 */
	public static void checkerNumber(int a) throws InputMismatchException {

		throw new InputMismatchException();

	}

}
