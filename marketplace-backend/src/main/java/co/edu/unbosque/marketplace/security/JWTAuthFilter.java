package co.edu.unbosque.marketplace.security;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

/**
 * Filtro de autenticación JWT que intercepta las solicitudes HTTP para
 * verificar la presencia de un token JWT. Este filtro se ejecuta una vez por
 * cada solicitud y es responsable de:
 * <ul>
 * <li>Excluir rutas públicas del proceso de autenticación.</li>
 * <li>Extraer el token JWT del encabezado de autorización.</li>
 * </ul>
 */
@Component
public class JWTAuthFilter extends OncePerRequestFilter {

	/**
	 * Método que filtra cada solicitud HTTP para verificar la presencia de un token
	 * JWT. Excluye ciertas rutas públicas y permite el paso de la solicitud a la
	 * cadena de filtros.
	 *
	 * @param request     Solicitud HTTP entrante.
	 * @param response    Respuesta HTTP saliente.
	 * @param filterChain Cadena de filtros para continuar el procesamiento de la
	 *                    solicitud.
	 * @throws ServletException Si ocurre un error en el procesamiento del filtro.
	 * @throws IOException      Si ocurre un error de entrada/salida.
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();

		if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/api-docs")
				|| path.startsWith("/swagger-resources") || path.startsWith("/webjars")
				|| path.startsWith("/usuarios/verificar") || path.startsWith("/usuarios/obtenercorreo")
				|| path.startsWith("/usuarios/obtener") || path.startsWith("/h2-console")) {

			filterChain.doFilter(request, response);
			return;
		}

		final String token = getTokenFromRequest(request);

		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Extrae el token JWT del encabezado de autorización de la solicitud HTTP.
	 *
	 * @param request Solicitud HTTP de la que se extraerá el token.
	 * @return El token JWT si está presente y es válido, o {@code null} en caso
	 *         contrario.
	 */
	private String getTokenFromRequest(HttpServletRequest request) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}

		return null;
	}

	/** constructor vacio */
	public JWTAuthFilter() {
		// TODO Auto-generated constructor stub
	}
}
