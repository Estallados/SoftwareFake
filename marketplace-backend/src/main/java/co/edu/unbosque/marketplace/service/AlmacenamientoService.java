package co.edu.unbosque.marketplace.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda en disco los archivos que suben los usuarios (por ahora, las imágenes
 * de los alojamientos) y los expone en {@value #RUTA_PUBLICA} (ver
 * {@code ArchivosConfig}).
 * <p>
 * El tipo de imagen se reconoce por los primeros bytes del archivo y no por la
 * extensión ni por el Content-Type que manda el cliente, que se pueden
 * falsificar. Solo se aceptan JPEG, PNG y WEBP de hasta 5 MB. El nombre en
 * disco es un UUID, así que el nombre original nunca se usa como ruta.
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

	private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoService.class);

	private static final int BYTES_CABECERA = 12;
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
	private static final byte[] RIFF = { 'R', 'I', 'F', 'F' };
	private static final byte[] WEBP = { 'W', 'E', 'B', 'P' };
	private static final int INICIO_WEBP = 8;

	/**
	 * Carpeta raíz donde se guardan los archivos (propiedad
	 * app.archivos.directorio).
	 */
	private final Path raiz;

	public AlmacenamientoService(@Value("${app.archivos.directorio:archivos}") String directorio) {
		this.raiz = Path.of(directorio).toAbsolutePath().normalize();
	}

	/**
	 * Carpeta raíz absoluta; la usa {@code ArchivosConfig} para servir los
	 * archivos.
	 */
	public Path getRaiz() {
		return raiz;
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
		return extension(archivo) == null ? "Solo se aceptan imágenes JPG, PNG o WEBP." : null;
	}

	/**
	 * Guarda una imagen ya validada con {@link #errorImagen(MultipartFile)}.
	 *
	 * @param archivo Imagen recibida.
	 * @param carpeta Subcarpeta relativa, por ejemplo {@code alojamientos/12}.
	 * @return la ruta relativa del archivo guardado (con / como separador).
	 */
	public String guardarImagen(MultipartFile archivo, String carpeta) {
		String extension = extension(archivo);
		if (extension == null) {
			throw new IllegalArgumentException("El archivo no es una imagen aceptada");
		}
		String ruta = carpeta + "/" + UUID.randomUUID() + "." + extension;
		Path destino = resolver(ruta);
		try (InputStream entrada = archivo.getInputStream()) {
			Files.createDirectories(destino.getParent());
			Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo guardar la imagen", e);
		}
		return ruta;
	}

	/** URL pública de un archivo guardado. */
	public static String urlPublica(String ruta) {
		return RUTA_PUBLICA + ruta;
	}

	/**
	 * Borra un archivo guardado. Si falla solo se registra en el log: un archivo
	 * huérfano no debe impedir la operación del usuario.
	 */
	public void eliminar(String ruta) {
		if (ruta == null || ruta.isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(resolver(ruta));
		} catch (IOException | IllegalArgumentException e) {
			LOG.warn("No se pudo borrar el archivo {}: {}", ruta, e.getMessage());
		}
	}

	/**
	 * Ruta absoluta dentro de la raíz; rechaza rutas que intenten salir de ella.
	 */
	private Path resolver(String ruta) {
		Path destino = raiz.resolve(ruta).normalize();
		if (!destino.startsWith(raiz)) {
			throw new IllegalArgumentException("Ruta fuera del almacenamiento: " + ruta);
		}
		return destino;
	}

	/**
	 * Extensión según los primeros bytes del archivo; null si no es JPEG, PNG ni
	 * WEBP.
	 */
	private static String extension(MultipartFile archivo) {
		byte[] cabecera;
		try (InputStream entrada = archivo.getInputStream()) {
			cabecera = entrada.readNBytes(BYTES_CABECERA);
		} catch (IOException e) {
			return null;
		}
		if (empiezaCon(cabecera, JPEG, 0)) {
			return "jpg";
		}
		if (empiezaCon(cabecera, PNG, 0)) {
			return "png";
		}
		if (empiezaCon(cabecera, RIFF, 0) && empiezaCon(cabecera, WEBP, INICIO_WEBP)) {
			return "webp";
		}
		return null;
	}

	private static boolean empiezaCon(byte[] datos, byte[] firma, int desde) {
		return datos.length >= desde + firma.length
				&& Arrays.equals(datos, desde, desde + firma.length, firma, 0, firma.length);
	}
}
