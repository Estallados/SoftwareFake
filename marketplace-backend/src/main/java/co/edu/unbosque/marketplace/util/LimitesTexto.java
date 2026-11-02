package co.edu.unbosque.marketplace.util;

/**
 * Largos máximos de los textos según las columnas de la base de datos. Los
 * servicios validan con estos valores para responder 400 en vez de que la
 * base rechace el dato con un 500.
 */
public final class LimitesTexto {

	/** Columnas varchar(255): títulos, nombres, ciudades, tipos. */
	public static final int CORTO = 255;

	/** Motivos escritos por usuarios o administradores. */
	public static final int MOTIVO = 500;

	/** Mensaje cuando un motivo supera {@link #MOTIVO}. */
	public static final String MOTIVO_LARGO = "El motivo puede tener máximo " + MOTIVO + " caracteres.";

	/** Columnas varchar(1000): notificaciones y motivos guardados con prefijo. */
	public static final int MEDIO = 1000;

	/** Columnas varchar(2000): descripciones, reglas, reseñas y mensajes. */
	public static final int LARGO = 2000;

	/** Correo: su versión cifrada en AES-GCM y Base64 debe caber en 255. */
	public static final int CORREO = 150;

	private LimitesTexto() {
	}

	/** Indica si el texto (sin espacios a los lados) supera el máximo. */
	public static boolean excede(String texto, int maximo) {
		return texto != null && texto.trim().length() > maximo;
	}

	/** Corta el texto al máximo; para textos que arma el sistema. */
	public static String recortar(String texto, int maximo) {
		return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
	}
}
