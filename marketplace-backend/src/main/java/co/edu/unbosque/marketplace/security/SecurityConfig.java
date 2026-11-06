package co.edu.unbosque.marketplace.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad stateless con JWT. Los roles salen del enum {@code Role}: HUESPED,
 * ANFITRION y ADMIN, que Spring ve como ROLE_HUESPED, ROLE_ANFITRION y
 * ROLE_ADMIN. En {@code hasRole(...)} y {@code @PreAuthorize} se escriben sin
 * el prefijo, por ejemplo {@code hasRole('ANFITRION')}.
 * <p>
 * Las reglas por rol finas están en cada controller con {@code @PreAuthorize};
 * aquí se definen las rutas públicas y {@code /admin/**}.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	/** Filtro que lee el JWT de cada petición y autentica al usuario. */
	private final JwtAuthenticationFilter jwtAuthFilter;

	/** Carga los usuarios por correo para el inicio de sesión. */
	private final UserDetailsService userDetailsService;

	/**
	 * Orígenes del front que pueden llamar a la API (propiedad
	 * app.cors.allowed-origins).
	 */
	private final List<String> origenesPermitidos;

	/**
	 * Crea la configuración de seguridad.
	 *
	 * @param jwtAuthFilter      filtro de autenticación por JWT
	 * @param userDetailsService servicio que carga los usuarios
	 * @param origenesPermitidos orígenes permitidos para CORS; por defecto
	 *                           {@code http://localhost:4200}
	 */
	public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService,
			@Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> origenesPermitidos) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.userDetailsService = userDetailsService;
		this.origenesPermitidos = origenesPermitidos;
	}

	/**
	 * Cadena de filtros de seguridad. Define:
	 * <ul>
	 * <li>sin CSRF ni sesión de servidor (cada petición trae su JWT);</li>
	 * <li>las rutas públicas: documentación, autenticación, salud, configuración
	 * de pagos, el handshake del WebSocket y las consultas públicas de
	 * alojamientos;</li>
	 * <li>{@code /admin/**} solo para administradores y {@code /alojamientos/mios}
	 * solo para anfitriones;</li>
	 * <li>respuestas JSON para el 401 (sin token o token inválido) y el 403 (sin
	 * permisos);</li>
	 * <li>el filtro JWT antes del filtro de usuario y contraseña.</li>
	 * </ul>
	 *
	 * @param http configuración de seguridad HTTP de Spring
	 * @return cadena de filtros armada
	 * @throws Exception si Spring no puede construir la configuración
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/auth/**").permitAll()
						// Spring reenvía los errores (400, 500...) a /error; si se protege,
						// cualquier error en una ruta pública se convierte en un 401 engañoso
						.requestMatchers("/error").permitAll()
						// Endpoint de salud para el monitoreo y el reinicio automático del servidor
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						// Configuración de pagos (modo y clave publicable de Stripe) para el front
						.requestMatchers(HttpMethod.GET, "/pagos/configuracion").permitAll()
						// Handshake WebSocket: el navegador no envía Authorization aquí; el token
						// se valida en el frame STOMP CONNECT (WebSocketAuthInterceptor)
						.requestMatchers(HttpMethod.GET, "/ws").permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/alojamientos/mios").hasRole("ANFITRION")
						// búsqueda, detalle, políticas, disponibilidad y reseñas son públicos;
						// el detalle de una publicación no ACTIVA lo filtra PublicacionService
						.requestMatchers(HttpMethod.GET, "/alojamientos", "/alojamientos/disponibles",
								"/alojamientos/politicas", "/alojamientos/tipos", "/alojamientos/{id}", "/alojamientos/{id}/disponibilidad",
								"/alojamientos/{id}/disponibilidad/consultar", "/alojamientos/{id}/resenas")
						.permitAll().anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((req, res, e) -> writeError(res, HttpServletResponse.SC_UNAUTHORIZED,
								"No autenticado: token ausente, inválido o expirado"))
						.accessDeniedHandler((req, res, e) -> writeError(res, HttpServletResponse.SC_FORBIDDEN,
								"No tienes permisos para esta operación")))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class).build();
	}

	/**
	 * Escribe la respuesta de error de seguridad en JSON, con el mismo formato que
	 * el resto de la API.
	 *
	 * @param res     respuesta HTTP
	 * @param status  código HTTP (401 o 403)
	 * @param message mensaje para el usuario
	 * @throws IOException si no se puede escribir la respuesta
	 */
	private static void writeError(HttpServletResponse res, int status, String message) throws IOException {
		res.setStatus(status);
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		res.setCharacterEncoding(StandardCharsets.UTF_8.name());
		res.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
	}

	/**
	 * CORS para el front (Angular). Lo usa
	 * {@code .cors(Customizer.withDefaults())}. El token viaja en el encabezado
	 * Authorization, no en cookies, por eso no se habilitan credenciales.
	 *
	 * @return configuración de CORS para todas las rutas
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origenesPermitidos.stream().map(String::trim).toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	/**
	 * Proveedor que autentica con correo y contraseña: carga el usuario con
	 * {@link UserDetailsService} y compara la contraseña con BCrypt.
	 *
	 * @return proveedor de autenticación
	 */
	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	/**
	 * Codificador de contraseñas. Las contraseñas se guardan con BCrypt, nunca en
	 * claro.
	 *
	 * @return codificador BCrypt
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Expone el {@link AuthenticationManager} para que el controlador de
	 * autenticación pueda validar el correo y la contraseña en el login.
	 *
	 * @param config configuración de autenticación de Spring
	 * @return administrador de autenticación
	 * @throws Exception si Spring no puede crearlo
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}