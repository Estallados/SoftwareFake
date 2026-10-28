package co.edu.unbosque.marketplace.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas, como la cancelación de reservas que quedaron
 * sin pagar ({@code ReservaService.cancelarPendientesVencidas}).
 *
 * @see "RF-020"
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
