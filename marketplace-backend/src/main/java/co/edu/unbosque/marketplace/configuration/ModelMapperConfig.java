package co.edu.unbosque.marketplace.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el {@link ModelMapper} que usan los servicios para copiar datos
 * entre las entidades y los DTO sin escribir cada campo a mano.
 */
@Configuration
public class ModelMapperConfig {

	/**
	 * Devuelve la instancia de {@link ModelMapper} con la configuración por defecto.
	 *
	 * @return instancia de {@link ModelMapper} con la configuración por defecto
	 */
	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}
}
