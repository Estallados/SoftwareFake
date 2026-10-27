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
 * ROLE_ADMIN. En {@code hasRole(...)} y {@code @PreAuthorize} se escriben sin el
 * prefijo, por ejemplo {@code hasRole('ANFITRION')}.
 * <p>
 * Las reglas por rol finas están en cada controller con {@code @PreAuthorize};
 * aquí se definen las rutas públicas y {@code /admin/**}.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;

	private final UserDetailsService userDetailsService;

	/** Orígenes del front que pueden llamar a la API (propiedad app.cors.allowed-origins). */
	private final List<String> origenesPermitidos;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService,
			@Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> origenesPermitidos) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.userDetailsService = userDetailsService;
		this.origenesPermitidos = origenesPermitidos;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/auth/**").permitAll()
						// Spring reenvía los errores (400, 500...) a /error; si se protege,
						// cualquier error en una ruta pública se convierte en un 401 engañoso
						.requestMatchers("/error").permitAll()
						// Endpoint de salud para monitoreo y reinicio automático (RNF-001..003)
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						// Imágenes subidas de los alojamientos (ArchivosConfig)
						.requestMatchers(HttpMethod.GET, "/archivos/**").permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/alojamientos/mios").hasRole("ANFITRION")
						// búsqueda, detalle, políticas, disponibilidad y reseñas son públicos;
						// el detalle de una publicación no ACTIVA lo filtra PublicacionService
						.requestMatchers(HttpMethod.GET, "/alojamientos", "/alojamientos/disponibles",
								"/alojamientos/politicas",
								"/alojamientos/{id}", "/alojamientos/{id}/disponibilidad",
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

	private static void writeError(HttpServletResponse res, int status, String message) throws IOException {
		res.setStatus(status);
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		res.setCharacterEncoding(StandardCharsets.UTF_8.name());
		res.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
	}

	/**
	 * CORS para el front (Angular). Lo usa {@code .cors(Customizer.withDefaults())}.
	 * El token viaja en el encabezado Authorization, no en cookies, por eso no se
	 * habilitan credenciales.
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

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}