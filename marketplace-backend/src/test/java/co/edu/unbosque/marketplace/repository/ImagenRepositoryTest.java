package co.edu.unbosque.marketplace.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import co.edu.unbosque.marketplace.entity.Imagen;
import co.edu.unbosque.marketplace.exception.AlmacenamientoNoDisponibleException;
import co.edu.unbosque.marketplace.service.AlmacenamientoService;
import co.edu.unbosque.marketplace.service.CloudinaryService.ImagenGuardada;

/** Pruebas del almacén de imágenes que usa Cloudinary en lugar de la base de datos. */
class ImagenRepositoryTest {

	private AlmacenamientoService almacenamiento;
	private ImagenRepository repo;

	@BeforeEach
	void preparar() {
		almacenamiento = mock(AlmacenamientoService.class);
		repo = new ImagenRepository(almacenamiento);
	}

	private static Imagen imagen(String ruta, boolean principal, long orden) {
		Imagen imagen = new Imagen();
		imagen.setIdAlojamiento(1L);
		imagen.setRutaArchivo(ruta);
		imagen.setUrl("https://cdn/" + ruta);
		imagen.setPrincipal(principal);
		imagen.setOrden(orden);
		return imagen;
	}

	private static void terminarTransaccion(int estado) {
		List<TransactionSynchronization> pendientes = TransactionSynchronizationManager.getSynchronizations();
		TransactionSynchronizationManager.clearSynchronization();
		pendientes.forEach(sincronizacion -> sincronizacion.afterCompletion(estado));
	}

	@Test
	void alArrancarRecuperaLasImagenesConSuPortadaOrdenYMismoId() {
		long id = repo.save(imagen("a", true, 1)).getIdImagen();
		when(almacenamiento.listarImagenes()).thenReturn(List.of(new ImagenGuardada("b", "https://cdn/b", 7L, false, 2),
				new ImagenGuardada("a", "https://cdn/a", 7L, true, 1)));
		ImagenRepository reiniciado = new ImagenRepository(almacenamiento);

		reiniciado.alArrancar();

		List<Imagen> imagenes = reiniciado.findByIdAlojamiento(7L);
		assertEquals(List.of("a", "b"), imagenes.stream().map(Imagen::getRutaArchivo).toList());
		assertTrue(imagenes.get(0).getPrincipal());
		assertFalse(imagenes.get(1).getPrincipal());
		assertEquals(id, imagenes.get(0).getIdImagen());
	}

	@Test
	void siCloudinaryFallaAlArrancarLoReintentaHastaLograrlo() {
		when(almacenamiento.listarImagenes()).thenThrow(new AlmacenamientoNoDisponibleException("caído"))
				.thenReturn(List.of(new ImagenGuardada("a", "https://cdn/a", 7L, true, 1)));

		repo.alArrancar();
		assertTrue(repo.findByIdAlojamiento(7L).isEmpty());

		repo.reintentarCarga();
		assertEquals(1, repo.findByIdAlojamiento(7L).size());

		repo.reintentarCarga();
		verify(almacenamiento, times(2)).listarImagenes();
	}

	@Test
	void cambiarLaPortadaActualizaCloudinarySoloAlConfirmar() {
		Imagen guardada = repo.save(imagen("a", false, 1));
		guardada.setPrincipal(true);

		TransactionSynchronizationManager.initSynchronization();
		repo.save(guardada);
		verify(almacenamiento, never()).actualizarImagen(any());

		terminarTransaccion(TransactionSynchronization.STATUS_COMMITTED);
		verify(almacenamiento).actualizarImagen(any());
	}

	@Test
	void guardarSinCambiarLaPortadaNoLlamaACloudinary() {
		repo.save(repo.save(imagen("a", true, 1)));

		verify(almacenamiento, never()).actualizarImagen(any());
	}

	@Test
	void siLaTransaccionFallaSeDeshacenLosCambiosEnMemoria() {
		Imagen existente = repo.save(imagen("a", true, 1));

		TransactionSynchronizationManager.initSynchronization();
		repo.save(imagen("nueva", false, 2));
		repo.delete(existente);
		assertEquals(List.of("nueva"),
				repo.findByIdAlojamiento(1L).stream().map(Imagen::getRutaArchivo).toList());

		terminarTransaccion(TransactionSynchronization.STATUS_ROLLED_BACK);

		assertEquals(List.of("a"), repo.findByIdAlojamiento(1L).stream().map(Imagen::getRutaArchivo).toList());
		verify(almacenamiento, never()).actualizarImagen(any());
	}

	@Test
	void lasLecturasDevuelvenCopias() {
		repo.save(imagen("a", false, 1));

		repo.findByIdAlojamiento(1L).get(0).setPrincipal(true);

		assertFalse(repo.findByIdAlojamiento(1L).get(0).getPrincipal());
	}

	@Test
	void elOrdenSiempreCrece() {
		assertTrue(repo.siguienteOrden() < repo.siguienteOrden());
	}

	@Test
	void elIdEsEstableYSeguroParaJavaScript() {
		long id = ImagenRepository.idDe("marketplace/alojamientos/1/foto");

		assertEquals(id, ImagenRepository.idDe("marketplace/alojamientos/1/foto"));
		assertTrue(id > 0 && id <= (1L << 53) - 1);
	}
}
