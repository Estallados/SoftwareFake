package co.edu.unbosque.marketplace.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas, como la cancelación automática de las
 * reservas que quedaron sin pagar ({@code ReservaService.cancelarPendientesVencidas}).
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
	
	/**
	 * Constructor de la configuración de programación.
	 */
	public ProgramacionConfig() {
	}
}
