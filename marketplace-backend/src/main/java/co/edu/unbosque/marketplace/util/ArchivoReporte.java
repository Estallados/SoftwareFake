package co.edu.unbosque.marketplace.util;

/**
 * Archivo generado por una exportación de reporte.
 *
 * @see "RF-006"
 */
public final class ArchivoReporte {

	private final String nombreArchivo;
	private final String tipoContenido;
	private final byte[] contenido;

	public ArchivoReporte(String nombreArchivo, String tipoContenido, byte[] contenido) {
		this.nombreArchivo = nombreArchivo;
		this.tipoContenido = tipoContenido;
		this.contenido = contenido.clone();
	}

	public String getNombreArchivo() {
		return nombreArchivo;
	}

	public String getTipoContenido() {
		return tipoContenido;
	}

	public byte[] getContenido() {
		return contenido.clone();
	}
}
