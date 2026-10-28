package co.edu.unbosque.marketplace.util;

/**
 * Resultado de una operación de un service: un código int (convención del
 * proyecto), el dato producido si la operación fue exitosa y un mensaje con la
 * causa si fue rechazada. Los controllers traducen el código a HTTP con
 * {@link RespuestaUtil#desde(Resultado)}.
 * <p>
 * Códigos comunes:
 * </p>
 * <ul>
 * <li>{@link #EXITO} 0: operación realizada.</li>
 * <li>{@link #NO_ENCONTRADO} 1: el recurso no existe o no es visible.</li>
 * <li>{@link #DATOS_INVALIDOS} 2: faltan datos o no son válidos.</li>
 * <li>{@link #SIN_PERMISO} 3: el usuario no es dueño ni administrador.</li>
 * <li>{@link #ESTADO_INVALIDO} 4: el estado actual no permite la
 * operación.</li>
 * <li>{@link #NO_DISPONIBLE} 5: las fechas no están disponibles.</li>
 * <li>{@link #PAGO_RECHAZADO} 6: el pago de prueba fue rechazado.</li>
 * <li>{@link #PAGO_ERROR} 7: no se pudo confirmar el pago.</li>
 * </ul>
 *
 * @param <T> tipo del dato producido
 * @see "RNF-022, CA-149"
 */
public final class Resultado<T> {

	public static final int EXITO = 0;
	public static final int NO_ENCONTRADO = 1;
	public static final int DATOS_INVALIDOS = 2;
	public static final int SIN_PERMISO = 3;
	public static final int ESTADO_INVALIDO = 4;
	public static final int NO_DISPONIBLE = 5;
	public static final int PAGO_RECHAZADO = 6;
	public static final int PAGO_ERROR = 7;

	private final int codigo;
	private final T dato;
	private final String mensaje;

	private Resultado(int codigo, T dato, String mensaje) {
		this.codigo = codigo;
		this.dato = dato;
		this.mensaje = mensaje;
	}

	/** Operación exitosa con su dato. */
	public static <T> Resultado<T> exito(T dato) {
		return new Resultado<>(EXITO, dato, null);
	}

	/** Operación rechazada con su código y la causa para el usuario. */
	public static <T> Resultado<T> error(int codigo, String mensaje) {
		return new Resultado<>(codigo, null, mensaje);
	}

	/**
	 * Operación rechazada que igual produce un dato (por ejemplo, la reserva
	 * cancelada cuando el pago fue rechazado).
	 */
	public static <T> Resultado<T> error(int codigo, String mensaje, T dato) {
		return new Resultado<>(codigo, dato, mensaje);
	}

	public boolean esExito() {
		return codigo == EXITO;
	}

	public int getCodigo() {
		return codigo;
	}

	public T getDato() {
		return dato;
	}

	public String getMensaje() {
		return mensaje;
	}
}
