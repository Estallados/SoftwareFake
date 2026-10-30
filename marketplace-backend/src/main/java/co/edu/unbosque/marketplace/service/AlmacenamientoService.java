package co.edu.unbosque.marketplace.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.edu.unbosque.marketplace.entity.ArchivoImagen;
import co.edu.unbosque.marketplace.repository.ArchivoImagenRepository;

/**
 * Guarda en la base (tabla {@code archivo_imagen}) las imágenes que suben los
 * usuarios y las expone en {@value #RUTA_PUBLICA} (ver
 * {@code ArchivoController}). No se usa el disco del servidor: el equipo
 * comparte la base pero cada uno corre su backend, y una imagen en disco solo
 * la vería quien la subió.
 * <p>
 * El tipo de imagen se reconoce por los primeros bytes del archivo y no por la
 * extensión ni por el Content-Type que manda el cliente, que se pueden
 * falsificar. Solo se aceptan JPEG, PNG y WEBP de hasta 5 MB. El nombre es un
 * UUID, así que el nombre original nunca se usa como ruta.
 * </p>
 *
 * @see "RF-017"
 */
@Service
public class AlmacenamientoService {

	/** Prefijo de las URL con que se sirven los archivos guardados. */
	public static final String RUTA_PUBLICA = "/archivos/";

	/** Tamaño máximo de una imagen (5 MB). */
	public static final long TAMANIO_MAXIMO = 5L * 1024 * 1024;

	private static final int BYTES_CABECERA = 12;
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
	private static final byte[] RIFF = { 'R', 'I', 'F', 'F' };
	private static final byte[] WEBP = { 'W', 'E', 'B', 'P' };
	private static final int INICIO_WEBP = 8;

	private final ArchivoImagenRepository archivoRepo;

	public AlmacenamientoService(ArchivoImagenRepository archivoRepo) {
		this.archivoRepo = archivoRepo;
	}

	/**
	 * Revisa que el archivo sea una imagen aceptada.
	 *
	 * @return el mensaje para el usuario si no sirve, o null si es válida.
	 */
	public String errorImagen(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			return "El archivo está vacío.";
		}
		if (archivo.getSize() > TAMANIO_MAXIMO) {
			return "Cada imagen puede pesar máximo 5 MB.";
		}
		return extension(cabecera(archivo)) == null ? "Solo se aceptan imágenes JPG, PNG o WEBP." : null;
	}

	/**
	 * Guarda una imagen ya validada con {@link #errorImagen(MultipartFile)}.
	 *
	 * @param archivo Imagen recibida.
	 * @param carpeta Prefijo de la ruta, por ejemplo {@code alojamientos/12}.
	 * @return la ruta relativa del archivo guardado (con / como separador).
	 */
	public String guardarImagen(MultipartFile archivo, String carpeta) {
		byte[] datos;
		try {
			datos = archivo.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer la imagen", e);
		}
		String extension = extension(datos);
		if (extension == null) {
			throw new IllegalArgumentException("El archivo no es una imagen aceptada");
		}
		String ruta = carpeta + "/" + UUID.randomUUID() + "." + extension;
		archivoRepo.save(new ArchivoImagen(ruta, "jpg".equals(extension) ? "image/jpeg" : "image/" + extension, datos));
		return ruta;
	}

	/** URL pública de un archivo guardado. */
	public static String urlPublica(String ruta) {
		return RUTA_PUBLICA + ruta;
	}

	/** Archivo guardado en la ruta indicada, si existe. */
	public Optional<ArchivoImagen> buscar(String ruta) {
		return archivoRepo.findById(ruta);
	}

	/** Borra un archivo guardado; una ruta null o inexistente no hace nada. */
	public void eliminar(String ruta) {
		if (ruta != null && !ruta.isBlank()) {
			archivoRepo.deleteById(ruta);
		}
	}

	private static byte[] cabecera(MultipartFile archivo) {
		try (InputStream entrada = archivo.getInputStream()) {
			return entrada.readNBytes(BYTES_CABECERA);
		} catch (IOException e) {
			return new byte[0];
		}
	}

	/**
	 * Extensión según los primeros bytes; null si no es JPEG, PNG ni WEBP.
	 */
	private static String extension(byte[] datos) {
		if (empiezaCon(datos, JPEG, 0)) {
			return "jpg";
		}
		if (empiezaCon(datos, PNG, 0)) {
			return "png";
		}
		if (empiezaCon(datos, RIFF, 0) && empiezaCon(datos, WEBP, INICIO_WEBP)) {
			return "webp";
		}
		return null;
	}

	private static boolean empiezaCon(byte[] datos, byte[] firma, int desde) {
		return datos.length >= desde + firma.length
				&& Arrays.equals(datos, desde, desde + firma.length, firma, 0, firma.length);
	}
}
