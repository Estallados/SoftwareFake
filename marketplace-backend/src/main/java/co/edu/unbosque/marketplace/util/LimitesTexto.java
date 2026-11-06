package co.edu.unbosque.marketplace.util;

/**
 * Largos máximos de los textos, tomados de las columnas de la base de datos.
 * <p>
 * Los servicios validan con estos valores antes de guardar, para responder un
 * 400 con un mensaje claro en lugar de que la base rechace el dato y la API
 * termine en un 500.
 * </p>
 */
public final class LimitesTexto {

	/** Columnas varchar(255): títulos, nombres, ciudades y tipos. */
	public static final int CORTO = 255;

	/** Motivos escritos por usuarios o administradores. */
	public static final int MOTIVO = 500;

	/** Mensaje que se responde cuando un motivo supera {@link #MOTIVO}. */
	public static final String MOTIVO_LARGO = "El motivo puede tener máximo " + MOTIVO + " caracteres.";

	/** Columnas varchar(1000): notificaciones y motivos que se guardan con prefijo. */
	public static final int MEDIO = 1000;

	/** Columnas varchar(2000): descripciones, reglas, reseñas y mensajes. */
	public static final int LARGO = 2000;

	/**
	 * Largo máximo del correo. Se guarda cifrado con AES-GCM y en Base64, y esa
	 * versión cifrada tiene que caber en una columna de 255.
	 */
	public static final int CORREO = 150;

	/** Clase de constantes: no se instancia. */
	private LimitesTexto() {
	}

	/**
	 * Indica si un texto supera el máximo, sin contar los espacios a los lados.
	 *
	 * @param texto  texto a revisar; {@code null} nunca excede
	 * @param maximo largo máximo permitido
	 * @return {@code true} si el texto es más largo que el máximo
	 */
	public static boolean excede(String texto, int maximo) {
		return texto != null && texto.trim().length() > maximo;
	}

	/**
	 * Corta un texto al largo máximo. Lo uso para textos que arma el propio
	 * sistema (por ejemplo, el detalle de la auditoría), donde no tiene sentido
	 * rechazar la operación.
	 *
	 * @param texto  texto a recortar; puede ser {@code null}
	 * @param maximo largo máximo
	 * @return el mismo texto si cabe, o sus primeros {@code maximo} caracteres
	 */
	public static String recortar(String texto, int maximo) {
		return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
	}
}
