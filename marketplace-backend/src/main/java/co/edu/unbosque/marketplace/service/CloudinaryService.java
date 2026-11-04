package co.edu.unbosque.marketplace.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * arranca, pero subir imágenes responde 503. Las validaciones están en
 * {@link AlmacenamientoService}. Además del archivo, Cloudinary guarda los datos de
 * cada imagen: es su única fuente, porque no se guardan en la base de datos.
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
	/** Máximo que admite la API de Cloudinary por página. */
	private static final int TAMANIO_PAGINA = 500;
	private static final String CAMPO_ALOJAMIENTO = "idAlojamiento";
	private static final String CAMPO_PRINCIPAL = "principal";
	private static final String CAMPO_ORDEN = "orden";

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

	/** Datos que se guardan en Cloudinary junto con el archivo de una imagen. */
	public record MetadatosImagen(String etiqueta, Long idAlojamiento, boolean principal, long orden) {
		String contexto() {
			return CAMPO_ALOJAMIENTO + "=" + idAlojamiento + "|" + CAMPO_PRINCIPAL + "=" + principal + "|" + CAMPO_ORDEN
					+ "=" + orden;
		}
	}

	/** Imagen leída de Cloudinary con sus datos. */
	public record ImagenGuardada(String publicId, String url, Long idAlojamiento, boolean principal, long orden) {
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
	public ImagenSubida subir(byte[] datos, String carpeta, String nombre, MetadatosImagen metadatos) {
		return enviar(datos, carpeta, nombre, metadatos);
	}

	/**
	 * Igual que {@link #subir}, pero Cloudinary descarga el archivo de una URL.
	 *
	 * @throws AlmacenamientoNoDisponibleException si no está configurado o Cloudinary
	 *                                             no responde.
	 */
	public ImagenSubida subirDesdeUrl(String url, String carpeta, String nombre, MetadatosImagen metadatos) {
		return enviar(url, carpeta, nombre, metadatos);
	}

	private ImagenSubida enviar(Object archivo, String carpeta, String nombre, MetadatosImagen metadatos) {
		if (cloudinary == null) {
			throw new AlmacenamientoNoDisponibleException(NO_CONFIGURADO);
		}
		Map<?, ?> respuesta;
		try {
			respuesta = cloudinary.uploader().upload(archivo,
					Map.of("folder", carpeta, "public_id", nombre, "overwrite", false, "resource_type", "image",
							"allowed_formats", "jpg,png,webp", "transformation",
							new Transformation<>().width(LADO_MAXIMO).height(LADO_MAXIMO).crop("limit"), "tags",
							metadatos.etiqueta(), "context", metadatos.contexto()));
		} catch (IOException | RuntimeException e) {
			LOG.error("Cloudinary no recibió la imagen {}/{}", carpeta, nombre, e);
			throw new AlmacenamientoNoDisponibleException(NO_DISPONIBLE);
		}
		String publicId = (String) respuesta.get("public_id");
		return new ImagenSubida(publicId, urlDeEntrega((String) respuesta.get("secure_url")));
	}

	private static String urlDeEntrega(String url) {
		return url.replace("/upload/", "/upload/" + ENTREGA + "/");
	}

	/**
	 * Lista las imágenes con una etiqueta y sus datos. Pide hasta
	 * {@value #TAMANIO_PAGINA} por llamada, para gastar pocas consultas de la API.
	 *
	 * @param etiqueta Etiqueta de las imágenes.
	 * @return Las imágenes; vacío si Cloudinary no está configurado.
	 * @throws AlmacenamientoNoDisponibleException si Cloudinary no responde.
	 */
	public List<ImagenGuardada> listar(String etiqueta) {
		List<ImagenGuardada> imagenes = new ArrayList<>();
		if (cloudinary == null) {
			return imagenes;
		}
		try {
			String cursor = null;
			do {
				Map<String, Object> opciones = new HashMap<>(
						Map.of("max_results", TAMANIO_PAGINA, "context", true, "resource_type", "image"));
				if (cursor != null) {
					opciones.put("next_cursor", cursor);
				}
				Map<?, ?> respuesta = cloudinary.api().resourcesByTag(etiqueta, opciones);
				imagenes.addAll(leerRecursos(respuesta));
				cursor = (String) respuesta.get("next_cursor");
			} while (cursor != null);
		} catch (Exception e) {
			LOG.error("Cloudinary no listó las imágenes con la etiqueta {}", etiqueta, e);
			throw new AlmacenamientoNoDisponibleException(NO_DISPONIBLE);
		}
		return imagenes;
	}

	/** Convierte la respuesta de la API; ignora los recursos sin datos de alojamiento. */
	static List<ImagenGuardada> leerRecursos(Map<?, ?> respuesta) {
		List<ImagenGuardada> imagenes = new ArrayList<>();
		if (respuesta.get("resources") instanceof List<?> recursos) {
			for (Object recurso : recursos) {
				if (recurso instanceof Map<?, ?> datos) {
					leerRecurso(datos, imagenes);
				}
			}
		}
		return imagenes;
	}

	private static void leerRecurso(Map<?, ?> recurso, List<ImagenGuardada> imagenes) {
		Object publicId = recurso.get("public_id");
		Map<?, ?> contexto = recurso.get("context") instanceof Map<?, ?> mapa ? mapa : Map.of();
		if (contexto.get("custom") instanceof Map<?, ?> personalizado) {
			contexto = personalizado;
		}
		try {
			imagenes.add(new ImagenGuardada((String) publicId, urlDeEntrega((String) recurso.get("secure_url")),
					Long.valueOf(String.valueOf(contexto.get(CAMPO_ALOJAMIENTO))),
					Boolean.parseBoolean(String.valueOf(contexto.get(CAMPO_PRINCIPAL))),
					Long.parseLong(String.valueOf(contexto.get(CAMPO_ORDEN)))));
		} catch (NumberFormatException | NullPointerException e) {
			LOG.warn("Se ignora la imagen {}: no tiene datos de alojamiento válidos", publicId);
		}
	}

	/**
	 * Actualiza la etiqueta y los datos de una imagen ya guardada.
	 *
	 * @return {@code true} si Cloudinary los actualizó; si falla, solo queda en el
	 *         log.
	 */
	public boolean actualizarMetadatos(String publicId, MetadatosImagen metadatos) {
		if (cloudinary == null) {
			LOG.warn("No se actualizó {} en Cloudinary: falta CLOUDINARY_URL", publicId);
			return false;
		}
		try {
			cloudinary.uploader().explicit(publicId, Map.of("type", "upload", "resource_type", "image", "tags",
					metadatos.etiqueta(), "context", metadatos.contexto()));
			return true;
		} catch (IOException | RuntimeException e) {
			LOG.error("No se pudieron actualizar los datos de {} en Cloudinary", publicId, e);
			return false;
		}
	}

	/**
	 * Borra una imagen y la invalida en la CDN. Si falla solo se registra: la
	 * imagen ya no se muestra y queda como un archivo huérfano.
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
