package co.edu.unbosque.marketplace.configuration;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.service.AlmacenamientoService;
import co.edu.unbosque.marketplace.service.CloudinaryService;

/**
 * Migración de un solo uso: copia a Cloudinary los datos de la antigua tabla
 * {@code imagen} (alojamiento, portada y orden), para que las imágenes existentes
 * se sigan viendo. Solo corre con {@code MIGRAR_IMAGENES=true} y no modifica la
 * tabla; volver a ejecutarla no duplica nada. Borra la tabla cuando el log
 * confirme que no hubo fallos.
 */
@Component
@ConditionalOnProperty(name = "app.imagenes.migrar", havingValue = "true")
public class MigracionImagenesInicializador implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(MigracionImagenesInicializador.class);
	private static final String CONSULTA = "SELECT id_imagen, id_alojamiento, url, ruta_archivo, principal "
			+ "FROM imagen ORDER BY id_imagen";

	private final JdbcTemplate jdbc;
	private final AlmacenamientoService almacenamientoService;
	private final CloudinaryService cloudinaryService;

	public MigracionImagenesInicializador(JdbcTemplate jdbc, AlmacenamientoService almacenamientoService,
			CloudinaryService cloudinaryService) {
		this.jdbc = jdbc;
		this.almacenamientoService = almacenamientoService;
		this.cloudinaryService = cloudinaryService;
	}

	@Override
	public void run(String... args) {
		if (!cloudinaryService.estaConfigurado()) {
			LOG.error("Migración de imágenes cancelada: falta CLOUDINARY_URL");
			return;
		}
		List<Map<String, Object>> filas;
		try {
			filas = jdbc.queryForList(CONSULTA);
		} catch (DataAccessException e) {
			LOG.error("Migración de imágenes cancelada: no se pudo leer la tabla imagen", e);
			return;
		}
		int fallidas = 0;
		for (Map<String, Object> fila : filas) {
			Long idImagen = ((Number) fila.get("id_imagen")).longValue();
			boolean migrada = almacenamientoService.migrarImagen(idImagen,
					((Number) fila.get("id_alojamiento")).longValue(), (String) fila.get("url"),
					(String) fila.get("ruta_archivo"), Boolean.TRUE.equals(fila.get("principal")));
			if (!migrada) {
				fallidas++;
				LOG.error("No se migró la imagen {}", idImagen);
			}
		}
		LOG.info("Migración de imágenes terminada: {} leídas, {} fallidas", filas.size(), fallidas);
	}
}
