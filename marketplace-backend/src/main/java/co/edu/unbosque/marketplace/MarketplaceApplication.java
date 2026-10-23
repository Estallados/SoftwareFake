package co.edu.unbosque.marketplace;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Punto de entrada del backend del Marketplace de alojamientos. */
@SpringBootApplication
public class MarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketplaceApplication.class, args);
	}

	/** constructor vacio */
	public MarketplaceApplication() {
		// TODO Auto-generated constructor stub
	}
	
	/** Model Mapper */
	@Bean
	public ModelMapper getModelMapper() {
		return new ModelMapper();
	}
	
}
