package co.edu.unbosque.marketplace.exception;

import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;

/**
 * Clase ExceptionCheker. Proporciona métodos para validar contraseñas según
 * diferentes criterios.
 */
public class ExceptionCheker {
	/**
	 * Verifica que una contraseña cumpla con todos los criterios de validación.
	 * 
	 * @param a La contraseña a validar.
	 * @throws CapitalException   Si no contiene al menos una letra mayúscula.
	 * @throws CharacterException Si no tiene entre 8 y 64 caracteres.
	 * @throws NumberException    Si no contiene al menos un número.
	 * @throws SymbolException    Si no contiene al menos un símbolo especial.
	 * @throws SmallException     Si no contiene al menos una letra minúscula.
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
	 * Indica si la contraseña cumple todas las reglas de
	 * {@link #checkerPasword(String)} (RNF-008), sin lanzar excepción.
	 *
	 * @param a La contraseña a validar.
	 * @return {@code true} si es válida.
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
	 * Verifica que la contraseña contenga al menos una letra mayúscula.
	 *
	 * @param a La contraseña a validar.
	 * @throws CapitalException Si no contiene letras mayúsculas.
	 */
	public static void checkerCapital(String a) throws CapitalException {
		if (a.chars().noneMatch(Character::isUpperCase)) {
			throw new CapitalException();
		}
	}

	/**
	 * Verifica que la contraseña contenga al menos una letra minúscula.
	 *
	 * @param a La contraseña a validar.
	 * @throws SmallException Si no contiene letras minúsculas.
	 */
	public static void checkerSmall(String a) throws SmallException {
		if (a.chars().noneMatch(Character::isLowerCase)) {
			throw new SmallException();
		}
	}

	public static void checkerMail(String a) throws InputMismatchException, MailException {
		if (!a.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
			throw new MailException();
		}
	}

	/**
	 * Verifica que la contraseña tenga entre 8 y 64 caracteres. BCrypt solo usa
	 * los primeros 72 bytes: sin el máximo, dos contraseñas distintas con el
	 * mismo comienzo abrirían la misma cuenta.
	 *
	 * @param a La contraseña a validar.
	 * @throws CharacterException Si tiene menos de 8 o más de 64 caracteres.
	 */
	public static void checkerCharacter(String a) throws CharacterException {
		if (a.length() < 8 || a.length() > 64 || a.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new CharacterException();
		}
	}

	/**
	 * Verifica que la contraseña contenga al menos una letra mayúscula.
	 * 
	 * @param a La contraseña a validar.
	 * @throws CapitalException Si no contiene letras mayúsculas.
	 */

	public static void checkerEqualPassword(String a, String b) throws EqualPasswordException {
		if (!a.equals(b)) {
			throw new EqualPasswordException();
		}
	}

	/**
	 * Verifica que la contraseña contenga al menos un número.
	 * 
	 * @param a La contraseña a validar.
	 * @throws NumberException Si no contiene números.
	 */

	public static void checkerNumber(String a) throws NumberException {
		if (!java.util.regex.Pattern.compile("\\d").matcher(a).find()) {
			throw new NumberException();
		}
	}

	/**
	 * Verifica que la contraseña contenga al menos un símbolo especial.
	 * 
	 * @param a La contraseña a validar.
	 * @throws SymbolException Si no contiene símbolos especiales.
	 */

	public static void checkerSymbol(String a) throws SymbolException {
		if (!java.util.regex.Pattern.compile("[^A-Za-z0-9]").matcher(a).find()) {
			throw new SymbolException();
		}
	}

	/**
	 * Verifica que el valor no sea neagtico
	 * 
	 * @param validar si el valor es menor a 0.
	 * @throws checkerNegativeNumber si el usurio no es negativo.
	 */
	public static void checkerNegativeNumber(int a) throws NegativeNumberException {
		if (a < 0) {
			throw new NegativeNumberException();
		}
	}

	/**
	 * Verifica que solo reciba letras
	 * 
	 * @param validar si lo ingresado son solo letras.
	 * @throws checkerText si lo ingresado no tiene letras.
	 */

	public static void checkerText(String a) throws TextException {
		if (!a.matches("[A-Za-záéíóúÁÉÍÓÚñÑ]+")) {
			throw new TextException();
		}
	}

	/**
	 * Verifica que lo ingresado sea solo numeros
	 * 
	 * @throws checkerNumber si lo ingresado es diferente a un numero.
	 */
	public static void checkerNumber(int a) throws InputMismatchException {

		throw new InputMismatchException();

	}

}
