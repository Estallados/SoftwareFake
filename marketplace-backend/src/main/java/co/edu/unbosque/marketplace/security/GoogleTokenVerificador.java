package co.edu.unbosque.marketplace.security;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.UnsupportedJwtException;

/**
 * Verifica el ID token que entrega el botón "Iniciar sesión con Google"
 * (Google Identity Services) en el front. El token es un JWT firmado por
 * Google; aquí se comprueba sin llamar a otros servicios de Google en cada
 * inicio de sesión:
 * <ul>
 * <li>la firma, con las claves públicas de Google (se descargan y se guardan
 * en memoria el tiempo que indica Google);</li>
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

    /** Claves públicas con las que Google firma los ID token (formato JWK). */
    public static final String URL_CLAVES = "https://www.googleapis.com/oauth2/v3/certs";

    private static final Logger LOG = LoggerFactory.getLogger(GoogleTokenVerificador.class);
    private static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final long TOLERANCIA_RELOJ_SEGUNDOS = 60;

    /** Datos de la cuenta de Google que se usan para iniciar sesión o registrarse. */
    public record CuentaGoogle(String idGoogle, String correo, String nombre) {
    }

    private final String clientId;
    private final Function<String, PublicKey> buscarClave;

    @Autowired
    public GoogleTokenVerificador(@Value("${app.google.client-id:}") String clientId) {
        this(clientId, new ClavesGoogle());
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
     * Verifica el ID token.
     *
     * @return la cuenta de Google, o vacío si el token no es válido, venció,
     *         no es para esta aplicación o el correo no está verificado.
     */
    public Optional<CuentaGoogle> verificar(String idToken) {
        if (!estaConfigurado() || idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                        @Override
                        public Key resolveSigningKey(JwsHeader header, Claims cuerpo) {
                            PublicKey clave = header.getKeyId() == null ? null : buscarClave.apply(header.getKeyId());
                            if (clave == null) {
                                throw new UnsupportedJwtException("El token no está firmado con una clave de Google");
                            }
                            return clave;
                        }
                    })
                    .requireAudience(clientId)
                    .setAllowedClockSkewSeconds(TOLERANCIA_RELOJ_SEGUNDOS)
                    .build().parseClaimsJws(idToken.trim()).getBody();

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

    /**
     * Claves públicas de Google en memoria. Se vuelven a descargar cuando
     * vence el tiempo indicado por Google (Cache-Control max-age) o cuando
     * llega un token firmado con una clave nueva, como mucho una vez por
     * minuto.
     */
    private static final class ClavesGoogle implements Function<String, PublicKey> {

        private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");
        private static final Duration VIGENCIA_POR_DEFECTO = Duration.ofHours(1);
        private static final Duration ESPERA_ENTRE_DESCARGAS = Duration.ofMinutes(1);

        private final RestClient restClient = RestClient.create();
        private Map<String, PublicKey> claves = Map.of();
        private Instant vence = Instant.EPOCH;
        private Instant ultimaDescarga = Instant.EPOCH;

        @Override
        public synchronized PublicKey apply(String idClave) {
            Instant ahora = Instant.now();
            boolean desconocida = !claves.containsKey(idClave)
                    && ahora.isAfter(ultimaDescarga.plus(ESPERA_ENTRE_DESCARGAS));
            if (ahora.isAfter(vence) || desconocida) {
                descargar(ahora);
            }
            return claves.get(idClave);
        }

        private void descargar(Instant ahora) {
            ultimaDescarga = ahora;
            try {
                ResponseEntity<JsonNode> respuesta = restClient.get().uri(URL_CLAVES).retrieve()
                        .toEntity(JsonNode.class);
                Map<String, PublicKey> nuevas = new HashMap<>();
                JsonNode cuerpo = respuesta.getBody();
                if (cuerpo != null) {
                    for (JsonNode jwk : cuerpo.path("keys")) {
                        if ("RSA".equals(jwk.path("kty").asText())) {
                            nuevas.put(jwk.path("kid").asText(), claveRsa(jwk.path("n").asText(),
                                    jwk.path("e").asText()));
                        }
                    }
                }
                claves = nuevas;
                vence = ahora.plus(vigencia(respuesta.getHeaders().getCacheControl()));
            } catch (RestClientException | GeneralSecurityException | IllegalArgumentException e) {
                LOG.warn("No se pudieron descargar las claves públicas de Google: {}", e.getMessage());
            }
        }

        private static Duration vigencia(String cacheControl) {
            Matcher m = cacheControl == null ? null : MAX_AGE.matcher(cacheControl);
            return m != null && m.find() ? Duration.ofSeconds(Long.parseLong(m.group(1))) : VIGENCIA_POR_DEFECTO;
        }

        private static PublicKey claveRsa(String modulo, String exponente) throws GeneralSecurityException {
            Base64.Decoder base64 = Base64.getUrlDecoder();
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                    new BigInteger(1, base64.decode(modulo)), new BigInteger(1, base64.decode(exponente))));
        }
    }
}
