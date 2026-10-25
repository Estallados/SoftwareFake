package co.edu.unbosque.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del backend del Marketplace de alojamientos. */
@SpringBootApplication
public class MarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketplaceApplication.class, args);
	}

	/** Constructor vacío. */
	public MarketplaceApplication() {
		// Constructor vacío: Spring crea la instancia
	}

	// El bean de ModelMapper se define en configuration/ModelMapperConfig

}
