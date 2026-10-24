package co.edu.unbosque.marketplace.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad stateless con JWT. Roles esperados en la entidad Usuario: HUESPED,
 * ANFITRION y ADMINISTRADOR (authorities ROLE_HUESPED, etc.). Ajusta las rutas
 * a los nombres de tus controladores.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;

	private final UserDetailsService userDetailsService;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.userDetailsService = userDetailsService;
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
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/alojamientos/mios").hasRole("ANFITRION")
						// búsqueda, detalle, disponibilidad y reseñas son públicos
						.requestMatchers(HttpMethod.GET, "/alojamientos", "/alojamientos/{id}",
								"/alojamientos/{id}/disponibilidad", "/alojamientos/{id}/resenas")
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