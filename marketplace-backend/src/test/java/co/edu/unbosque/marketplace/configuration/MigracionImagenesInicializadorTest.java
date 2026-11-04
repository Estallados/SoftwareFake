package co.edu.unbosque.marketplace.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.unbosque.marketplace.service.AlmacenamientoService;
import co.edu.unbosque.marketplace.service.CloudinaryService;

/** Pruebas de la migración de la tabla imagen a Cloudinary. */
class MigracionImagenesInicializadorTest {

	private JdbcTemplate jdbc;
	private AlmacenamientoService almacenamiento;
	private CloudinaryService cloudinary;
	private MigracionImagenesInicializador migracion;

	@BeforeEach
	void preparar() {
		jdbc = mock(JdbcTemplate.class);
		almacenamiento = mock(AlmacenamientoService.class);
		cloudinary = mock(CloudinaryService.class);
		when(cloudinary.estaConfigurado()).thenReturn(true);
		migracion = new MigracionImagenesInicializador(jdbc, almacenamiento, cloudinary);
	}

	private static Map<String, Object> fila(long id, String url, String ruta, Boolean principal) {
		Map<String, Object> fila = new HashMap<>();
		fila.put("id_imagen", id);
		fila.put("id_alojamiento", 7);
		fila.put("url", url);
		fila.put("ruta_archivo", ruta);
		fila.put("principal", principal);
		return fila;
	}

	@Test
	void migraCadaFilaConservandoPortadaYTratandoPrincipalNullComoFalse() {
		when(jdbc.queryForList(anyString()))
				.thenReturn(List.of(fila(1, "https://cdn/a", "ruta/a", true), fila(2, "https://externa/b", null, null)));

		migracion.run();

		verify(almacenamiento).migrarImagen(1L, 7L, "https://cdn/a", "ruta/a", true);
		verify(almacenamiento).migrarImagen(2L, 7L, "https://externa/b", null, false);
	}

	@Test
	void sinCloudinaryNoLeeLaTabla() {
		when(cloudinary.estaConfigurado()).thenReturn(false);

		migracion.run();

		verify(jdbc, never()).queryForList(anyString());
	}

	@Test
	void sinLaTablaNoMigraNiDetieneElArranque() {
		when(jdbc.queryForList(anyString())).thenThrow(new DataAccessResourceFailureException("no existe"));

		migracion.run();

		verify(almacenamiento, never()).migrarImagen(anyLong(), anyLong(), any(), any(), anyBoolean());
	}
}
