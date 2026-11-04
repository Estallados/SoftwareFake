package co.edu.unbosque.marketplace.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import co.edu.unbosque.marketplace.entity.Imagen;
import co.edu.unbosque.marketplace.exception.AlmacenamientoNoDisponibleException;
import co.edu.unbosque.marketplace.service.CloudinaryService.ImagenGuardada;
import co.edu.unbosque.marketplace.service.CloudinaryService.ImagenSubida;
import co.edu.unbosque.marketplace.service.CloudinaryService.MetadatosImagen;

/**
 * Valida las imágenes que suben los usuarios y las guarda en Cloudinary (ver
 * {@link CloudinaryService}); la base solo guarda su id y su URL.
 * <p>
 * El tipo de imagen se reconoce por los primeros bytes del archivo y no por la
 * extensión ni por el Content-Type que manda el cliente, que se pueden
 * falsificar. Solo se aceptan JPEG, PNG y WEBP de hasta 5 MB. El nombre es un
 * UUID, así que el nombre original nunca se usa como ruta.
 * </p>
 * <p>
 * Cloudinary no participa de la transacción de la base: si la transacción se
 * revierte, las imágenes que alcanzaron a subirse se borran, y una imagen
 * eliminada solo se borra de Cloudinary cuando la transacción se confirma.
 * </p>
 *
 * @see "RF-017"
 */
@Service
public class AlmacenamientoService {

	/** Tamaño máximo de una imagen (5 MB). */
	public static final long TAMANIO_MAXIMO = 5L * 1024 * 1024;

	private static final int BYTES_CABECERA = 12;
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
	private static final byte[] RIFF = { 'R', 'I', 'F', 'F' };
	private static final byte[] WEBP = { 'W', 'E', 'B', 'P' };
	private static final int INICIO_WEBP = 8;

	private final CloudinaryService cloudinaryService;
	/** Carpeta raíz en Cloudinary; permite separar ambientes (dev, prod). */
	private final String carpetaRaiz;

	public AlmacenamientoService(CloudinaryService cloudinaryService,
			@Value("${app.cloudinary.carpeta:marketplace}") String carpetaRaiz) {
		this.cloudinaryService = cloudinaryService;
		this.carpetaRaiz = carpetaRaiz;
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
		return esImagen(cabecera(archivo)) ? null : "Solo se aceptan imágenes JPG, PNG o WEBP.";
	}

	/**
	 * Guarda una imagen ya validada con {@link #errorImagen(MultipartFile)}.
	 *
	 * @param archivo       Imagen recibida.
	 * @param idAlojamiento Alojamiento dueño de la imagen.
	 * @param principal     Si es la portada del alojamiento.
	 * @param orden         Posición de la imagen dentro del alojamiento.
	 * @return el id en Cloudinary (va en {@code rutaArchivo}) y la URL pública.
	 * @throws co.edu.unbosque.marketplace.exception.AlmacenamientoNoDisponibleException
	 *         si Cloudinary no la recibe.
	 */
	public ImagenSubida guardarImagen(MultipartFile archivo, Long idAlojamiento, boolean principal, long orden) {
		byte[] datos;
		try {
			datos = archivo.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer la imagen", e);
		}
		if (!esImagen(datos)) {
			throw new IllegalArgumentException("El archivo no es una imagen aceptada");
		}
		ImagenSubida subida = cloudinaryService.subir(datos, carpetaDe(idAlojamiento), UUID.randomUUID().toString(),
				new MetadatosImagen(etiqueta(), idAlojamiento, principal, orden));
		alTerminarTransaccion(TransactionSynchronization.STATUS_ROLLED_BACK, subida.publicId());
		return subida;
	}

	/** @return Las imágenes de alojamientos guardadas en Cloudinary para este ambiente. */
	public List<ImagenGuardada> listarImagenes() {
		return cloudinaryService.listar(etiqueta());
	}

	/** Actualiza en Cloudinary los datos de una imagen; si falla, solo queda en el log. */
	public void actualizarImagen(Imagen imagen) {
		cloudinaryService.actualizarMetadatos(imagen.getRutaArchivo(), new MetadatosImagen(etiqueta(),
				imagen.getIdAlojamiento(), Boolean.TRUE.equals(imagen.getPrincipal()), imagen.getOrden()));
	}

	/**
	 * Pasa a Cloudinary una imagen de la antigua tabla {@code imagen}. Una URL
	 * externa (ruta vacía) se sube como archivo, porque Cloudinary es ahora la
	 * única fuente.
	 *
	 * @return {@code true} si quedó migrada.
	 */
	public boolean migrarImagen(Long idImagen, Long idAlojamiento, String url, String ruta, boolean principal) {
		MetadatosImagen metadatos = new MetadatosImagen(etiqueta(), idAlojamiento, principal, idImagen);
		if (ruta != null && !ruta.isBlank()) {
			return cloudinaryService.actualizarMetadatos(ruta, metadatos);
		}
		try {
			cloudinaryService.subirDesdeUrl(url, carpetaDe(idAlojamiento), "externa-" + idImagen, metadatos);
			return true;
		} catch (AlmacenamientoNoDisponibleException e) {
			return false;
		}
	}

	private String carpetaDe(Long idAlojamiento) {
		return carpetaRaiz + "/alojamientos/" + idAlojamiento;
	}

	/** Etiqueta común de las imágenes; depende de la carpeta para separar ambientes. */
	private String etiqueta() {
		return carpetaRaiz.replace('/', '-') + "-alojamientos";
	}

	/**
	 * Borra una imagen guardada cuando se confirme la transacción; una ruta null
	 * (URL externa) no hace nada.
	 */
	public void eliminar(String ruta) {
		if (ruta != null && !ruta.isBlank()) {
			alTerminarTransaccion(TransactionSynchronization.STATUS_COMMITTED, ruta);
		}
	}

	/**
	 * Borra la imagen de Cloudinary si la transacción termina con el estado
	 * indicado; sin transacción activa, solo la borra cuando se pide eliminar.
	 */
	private void alTerminarTransaccion(int estado, String publicId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			if (estado == TransactionSynchronization.STATUS_COMMITTED) {
				cloudinaryService.borrar(publicId);
			}
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int estadoFinal) {
				if (estadoFinal == estado) {
					cloudinaryService.borrar(publicId);
				}
			}
		});
	}

	private static byte[] cabecera(MultipartFile archivo) {
		try (InputStream entrada = archivo.getInputStream()) {
			return entrada.readNBytes(BYTES_CABECERA);
		} catch (IOException e) {
			return new byte[0];
		}
	}

	/** Si los primeros bytes son de un JPEG, PNG o WEBP. */
	private static boolean esImagen(byte[] datos) {
		return empiezaCon(datos, JPEG, 0) || empiezaCon(datos, PNG, 0)
				|| (empiezaCon(datos, RIFF, 0) && empiezaCon(datos, WEBP, INICIO_WEBP));
	}

	private static boolean empiezaCon(byte[] datos, byte[] firma, int desde) {
		return datos.length >= desde + firma.length
				&& Arrays.equals(datos, desde, desde + firma.length, firma, 0, firma.length);
	}
}
