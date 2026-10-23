package co.edu.unbosque.marketplace;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Clase de inicialización para despliegue WAR en contenedores de servlets tradicionales.
 * <p>
 * Esta clase extiende {@link SpringBootServletInitializer} y proporciona la configuración
 * necesaria para ejecutar la aplicación Spring Boot cuando es desplegada como un archivo
 * WAR en un contenedor de servlets externo (como Apache Tomcat, Jetty o WildFly).
 * </p>
 * <p>
 * {@code SpringBootServletInitializer} implementa {@code WebApplicationInitializer} de
 * Spring Framework, lo que permite la configuración de la aplicación utilizando la API
 * de Servlet 3.0 sin necesidad de un archivo {@code web.xml}.
 * </p>
 * <p>
 * <strong>Nota:</strong> Esta clase solo es necesaria si se está construyendo un archivo
 * WAR para despliegue tradicional. Si se prefiere ejecutar un servidor web embebido
 * (como el servidor Tomcat embebido de Spring Boot), esta clase no es requerida.
 * </p>
 * 
 * @author [kevin]
 * @version 1.0
 * @since 2025-11-07
 * @see SpringBootServletInitializer
 * @see SpringApplicationBuilder
 * @see ProyectoIaApplication
 */
public class ServletInitializer extends SpringBootServletInitializer {

	/**
	 * Configura la aplicación Spring Boot para su ejecución en un contenedor de servlets.
	 * <p>
	 * Este método es llamado automáticamente por el contenedor de servlets durante el
	 * proceso de inicio de la aplicación. Sobreescribe el método {@code configure} de
	 * {@link SpringBootServletInitializer} para especificar la clase de configuración
	 * principal de la aplicación.
	 * </p>
	 * <p>
	 * El método utiliza {@link SpringApplicationBuilder#sources(Class...)} para registrar
	 * la clase {@link ProyectoIaApplication} como fuente de configuración, permitiendo
	 * que Spring Boot inicialice el contexto de aplicación con todas las configuraciones
	 * y componentes definidos en dicha clase.
	 * </p>
	 * 
	 * @param application el objeto {@link SpringApplicationBuilder} utilizado para
	 *                    configurar la aplicación Spring Boot. Este builder permite
	 *                    personalizar la configuración antes de que la aplicación sea
	 *                    iniciada por el contenedor de servlets.
	 * @return el {@link SpringApplicationBuilder} configurado con las fuentes de
	 *         configuración de la aplicación, permitiendo encadenamiento de métodos
	 *         para configuraciones adicionales.
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MarketplaceApplication.class);
	}
	
	/** COnstructor */
	public ServletInitializer() {
		// TODO Auto-generated constructor stub
	}

}
