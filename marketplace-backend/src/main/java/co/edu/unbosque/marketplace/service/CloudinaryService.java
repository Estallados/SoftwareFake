package co.edu.unbosque.marketplace.service;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;

import co.edu.unbosque.marketplace.exception.AlmacenamientoNoDisponibleException;

/**
 * Consumo de la API de Cloudinary, donde se guardan las imágenes subidas. La
 * cuenta se configura con la variable {@code CLOUDINARY_URL}
 * ({@code cloudinary://<api_key>:<api_secret>@<cloud_name>}); sin ella la app
 * arranca, pero subir imágenes responde 503. Las validaciones y la relación con
 * la base están en {@link AlmacenamientoService}.
 *
 * @see "RF-017"
 */
@Service
public class CloudinaryService {

	/**
	 * Transformación de entrega: Cloudinary elige el formato (WEBP/AVIF) y la
	 * calidad según el navegador, así no se descarga el original de hasta 5 MB.
	 */
	public static final String ENTREGA = "f_auto,q_auto";

	private static final Logger LOG = LoggerFactory.getLogger(CloudinaryService.class);
	private static final String NO_CONFIGURADO = "El almacenamiento de imágenes no está configurado.";
	private static final String NO_DISPONIBLE = "No se pudo guardar la imagen. Intenta de nuevo.";
	/** Tiempo máximo de cada petición, para que Cloudinary lento no cuelgue la subida. */
	private static final int TIEMPO_MAXIMO_SEGUNDOS = 30;
	/** Lado máximo con que se guarda una imagen; las más grandes se reducen al subir. */
	private static final int LADO_MAXIMO = 2000;

	/** Null si no se definió {@code CLOUDINARY_URL}. */
	private final Cloudinary cloudinary;

	public CloudinaryService(@Value("${app.cloudinary.url:${CLOUDINARY_URL:}}") String url) {
		if (url == null || url.isBlank()) {
			cloudinary = null;
			return;
		}
		try {
			cloudinary = new Cloudinary(url.trim());
		} catch (RuntimeException e) {
			throw new IllegalStateException(
					"CLOUDINARY_URL no es válida: debe tener la forma cloudinary://<api_key>:<api_secret>@<cloud_name>");
		}
		cloudinary.config.secure = true;
		cloudinary.config.timeout = TIEMPO_MAXIMO_SEGUNDOS;
	}

	/** Imagen guardada: su id en Cloudinary y la URL pública con que se entrega. */
	public record ImagenSubida(String publicId, String url) {
	}

	/** Indica si se configuró la cuenta de Cloudinary. */
	public boolean estaConfigurado() {
		return cloudinary != null;
	}

	/**
	 * Sube una imagen ya validada.
	 *
	 * @param carpeta Carpeta de Cloudinary, por ejemplo
	 *                {@code marketplace/alojamientos/12}.
	 * @param nombre  Nombre único del archivo dentro de la carpeta.
	 * @throws AlmacenamientoNoDisponibleException si no está configurado o
	 *                                             Cloudinary no responde.
	 */
	public ImagenSubida subir(byte[] datos, String carpeta, String nombre) {
		if (cloudinary == null) {
			throw new AlmacenamientoNoDisponibleException(NO_CONFIGURADO);
		}
		Map<?, ?> respuesta;
		try {
			respuesta = cloudinary.uploader().upload(datos,
					Map.of("folder", carpeta, "public_id", nombre, "overwrite", false, "resource_type", "image",
							"allowed_formats", "jpg,png,webp", "transformation",
							new Transformation<>().width(LADO_MAXIMO).height(LADO_MAXIMO).crop("limit")));
		} catch (IOException | RuntimeException e) {
			LOG.error("Cloudinary no recibió la imagen {}/{}", carpeta, nombre, e);
			throw new AlmacenamientoNoDisponibleException(NO_DISPONIBLE);
		}
		String publicId = (String) respuesta.get("public_id");
		String url = (String) respuesta.get("secure_url");
		return new ImagenSubida(publicId, url.replace("/upload/", "/upload/" + ENTREGA + "/"));
	}

	/**
	 * Borra una imagen y la invalida en la CDN. Si falla solo se registra: la
	 * imagen ya no está en la base y queda como un archivo huérfano.
	 */
	public void borrar(String publicId) {
		if (cloudinary == null) {
			LOG.warn("No se borró {} de Cloudinary: falta CLOUDINARY_URL", publicId);
			return;
		}
		try {
			Map<?, ?> respuesta = cloudinary.uploader().destroy(publicId,
					Map.of("resource_type", "image", "invalidate", true));
			Object resultado = respuesta.get("result");
			if (!"ok".equals(resultado) && !"not found".equals(resultado)) {
				LOG.warn("Cloudinary no borró {}: {}", publicId, resultado);
			}
		} catch (IOException | RuntimeException e) {
			LOG.warn("No se pudo borrar {} de Cloudinary", publicId, e);
		}
	}
}
