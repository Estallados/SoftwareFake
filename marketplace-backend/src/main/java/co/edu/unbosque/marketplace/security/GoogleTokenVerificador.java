package co.edu.unbosque.marketplace.security;

import java.security.Key;
import java.security.PublicKey;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.service.GoogleClavesService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.UnsupportedJwtException;

/**
 * Verifica el ID token que entrega el botón "Iniciar sesión con Google" (Google
 * Identity Services) en el front. El token es un JWT firmado por Google; aquí
 * se comprueba sin llamar a otros servicios de Google en cada inicio de sesión:
 * <ul>
 * <li>la firma, con las claves públicas de Google, que descarga y guarda en
 * memoria {@link GoogleClavesService};</li>
 * <li>el emisor ({@code accounts.google.com}) y el vencimiento;</li>
 * <li>la audiencia: el token debe haberse emitido para nuestro
 * {@code app.google.client-id}, así no sirve uno generado para otra app;</li>
 * <li>que Google haya verificado el correo.</li>
 * </ul>
 * Sin {@code GOOGLE_CLIENT_ID} el inicio con Google queda deshabilitado.
 *
 * @see "RF-008"
 */
@Component
public class GoogleTokenVerificador {

	private static final Logger LOG = LoggerFactory.getLogger(GoogleTokenVerificador.class);
	private static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");
	private static final long TOLERANCIA_RELOJ_SEGUNDOS = 60;

	/**
	 * Datos de la cuenta de Google que se usan para iniciar sesión o registrarse.
	 */
	public record CuentaGoogle(String idGoogle, String correo, String nombre) {
	}

	private final String clientId;
	private final Function<String, PublicKey> buscarClave;

	@Autowired
	public GoogleTokenVerificador(@Value("${app.google.client-id:}") String clientId, GoogleClavesService claves) {
		this(clientId, claves::buscar);
	}

	/**
	 * Constructor con otra fuente de claves (identificador de clave → clave
	 * pública); lo usan las pruebas para firmar tokens propios.
	 */
	public GoogleTokenVerificador(String clientId, Function<String, PublicKey> buscarClave) {
		this.clientId = clientId == null ? "" : clientId.trim();
		this.buscarClave = buscarClave;
	}

	/** Indica si se configuró el client ID de Google. */
	public boolean estaConfigurado() {
		return !clientId.isEmpty();
	}

	/**
	 * Client ID con el que el front inicializa el botón de Google; null si no está
	 * configurado. No es secreto.
	 */
	public String getClientId() {
		return estaConfigurado() ? clientId : null;
	}

	/**
	 * Verifica el ID token.
	 *
	 * @return la cuenta de Google, o vacío si el token no es válido, venció, no es
	 *         para esta aplicación o el correo no está verificado.
	 */
	public Optional<CuentaGoogle> verificar(String idToken) {
		if (!estaConfigurado() || idToken == null || idToken.isBlank()) {
			return Optional.empty();
		}
		try {
			Claims claims = Jwts.parserBuilder().setSigningKeyResolver(new SigningKeyResolverAdapter() {
				@Override
				public Key resolveSigningKey(JwsHeader header, Claims cuerpo) {
					PublicKey clave = header.getKeyId() == null ? null : buscarClave.apply(header.getKeyId());
					if (clave == null) {
						throw new UnsupportedJwtException("El token no está firmado con una clave de Google");
					}
					return clave;
				}
			}).requireAudience(clientId).setAllowedClockSkewSeconds(TOLERANCIA_RELOJ_SEGUNDOS).build()
					.parseClaimsJws(idToken.trim()).getBody();

			Object verificado = claims.get("email_verified");
			String correo = claims.get("email", String.class);
			if (!EMISORES.contains(claims.getIssuer()) || correo == null || correo.isBlank()
					|| !(Boolean.TRUE.equals(verificado) || "true".equals(verificado))) {
				return Optional.empty();
			}
			return Optional.of(new CuentaGoogle(claims.getSubject(), correo, claims.get("name", String.class)));
		} catch (JwtException | IllegalArgumentException e) {
			LOG.info("ID token de Google rechazado: {}", e.getMessage());
			return Optional.empty();
		}
	}
}
