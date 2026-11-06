package co.edu.unbosque.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend del Marketplace de alojamientos.
 * <p>
 * El backend está organizado por capas dentro de {@code co.edu.unbosque.marketplace}:
 * </p>
 * <ul>
 * <li>{@code controller}: los endpoints REST; reciben la petición, llaman al
 * servicio y traducen el resultado a HTTP.</li>
 * <li>{@code service}: las reglas de negocio.</li>
 * <li>{@code repository}: el acceso a la base de datos con Spring Data
 * JPA.</li>
 * <li>{@code entity}: las tablas de la base de datos.</li>
 * <li>{@code dto}: los objetos que entran y salen por la API.</li>
 * <li>{@code security}: autenticación con JWT, Google y WebSocket.</li>
 * <li>{@code configuration}: configuración de Spring y tareas que corren al
 * arrancar.</li>
 * <li>{@code exception}: validaciones y manejo global de errores.</li>
 * <li>{@code util}: utilidades compartidas (cifrado, respuestas, reportes,
 * correo).</li>
 * </ul>
 */
@SpringBootApplication
public class MarketplaceApplication {

	/**
	 * Arranca la aplicación con el servidor web embebido.
	 *
	 * @param args argumentos de la línea de comandos
	 */
	public static void main(String[] args) {
		SpringApplication.run(MarketplaceApplication.class, args);
	}

	/** Constructor vacío. */
	public MarketplaceApplication() {
		// Constructor vacío: Spring crea la instancia
	}

	// El bean de ModelMapper se define en configuration/ModelMapperConfig

}
