package co.edu.unbosque.marketplace.service;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Consumo de la API de claves públicas de Google (formato JWK), con las que
 * Google firma los ID token del inicio de sesión. Las claves se guardan en
 * memoria y se vuelven a descargar cuando vence el tiempo indicado por Google
 * (Cache-Control max-age) o cuando llega un token firmado con una clave nueva,
 * como mucho una vez por minuto.
 *
 * @see "RF-008"
 */
@Service
public class GoogleClavesService {

    /** Claves públicas con las que Google firma los ID token (formato JWK). */
    public static final String URL_CLAVES = "https://www.googleapis.com/oauth2/v3/certs";

    private static final Logger LOG = LoggerFactory.getLogger(GoogleClavesService.class);
    private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");
    private static final Duration VIGENCIA_POR_DEFECTO = Duration.ofHours(1);
    private static final Duration ESPERA_ENTRE_DESCARGAS = Duration.ofMinutes(1);

    /** Claves descargadas y cuándo se descargaron; solo se usan dentro de {@link #buscar}. */
    private static final class Cache {
        private Map<String, PublicKey> claves = Map.of();
        private Instant vence = Instant.EPOCH;
        private Instant ultimaDescarga = Instant.EPOCH;
    }

    private final ClienteHttp clienteHttp;
    private final Cache cache;

    public GoogleClavesService(ClienteHttp clienteHttp) {
        this.clienteHttp = clienteHttp;
        this.cache = new Cache();
    }

    /**
     * Clave pública de Google con el identificador indicado.
     *
     * @return la clave, o null si Google no tiene una con ese identificador.
     */
    public synchronized PublicKey buscar(String idClave) {
        Instant ahora = Instant.now();
        boolean desconocida = !cache.claves.containsKey(idClave)
                && ahora.isAfter(cache.ultimaDescarga.plus(ESPERA_ENTRE_DESCARGAS));
        if (ahora.isAfter(cache.vence) || desconocida) {
            descargar(ahora);
        }
        return cache.claves.get(idClave);
    }

    private void descargar(Instant ahora) {
        cache.ultimaDescarga = ahora;
        try {
            ResponseEntity<JsonNode> respuesta = clienteHttp.getJson(URL_CLAVES);
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
            cache.claves = nuevas;
            cache.vence = ahora.plus(vigencia(respuesta.getHeaders().getCacheControl()));
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
