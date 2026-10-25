package co.edu.unbosque.marketplace.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

import co.edu.unbosque.marketplace.util.AESUtil;

/**
 * Servicio utilitario para la generación, validación y manejo de tokens JWT.
 * Proporciona métodos para crear tokens, extraer información de ellos y validar su autenticidad.
 */
@Service
public class JwtUtil {

    /**
     * Tiempo de expiración de los tokens JWT en milisegundos.
     * Se inyecta desde la configuración de la aplicación.
     */
    @Value("${spring.jwt.expirationtime}")
    private long expirationTime;

    /**
     * Genera un token JWT con los claims adicionales y el sujeto especificado.
     *
     * @param extraClaims Claims adicionales que se incluirán en el token.
     * @param subject     Sujeto del token, generalmente el nombre de usuario o correo.
     * @return Token JWT generado.
     */
    public String obtenerToken(Map<String, Object> extraClaims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts
        .builder()
        .setClaims(extraClaims)
        .setSubject(subject)
        .setIssuedAt(new Date(now))
        .setExpiration(new Date(now + expirationTime))
        .signWith(obtenerKey(), SignatureAlgorithm.HS256)
        .compact();
    }

    /**
     * Obtiene la clave de firma para los tokens JWT. Se deriva de AES_SECRET_KEY,
     * así no hay que configurar otro secreto ni escribirlo en el proyecto.
     *
     * @return Clave de firma para JWT.
     */
    private Key obtenerKey() {
        return Keys.hmacShaKeyFor(AESUtil.derivarClave("jwt"));
    }

    /**
     * Valida si un token JWT es válido para el usuario especificado.
     *
     * @param token       Token JWT a validar.
     * @param userDetails Detalles del usuario para comparar con el sujeto del token.
     * @return true si el token es válido, false en caso contrario.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Extrae el nombre de usuario (sujeto) de un token JWT.
     *
     * @param token Token JWT del cual extraer el sujeto.
     * @return Nombre de usuario contenido en el token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración de un token JWT.
     *
     * @param token Token JWT del cual extraer la fecha de expiración.
     * @return Fecha de expiración del token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico de un token JWT utilizando una función resolutora.
     *
     * @param <T>           Tipo del claim a extraer.
     * @param token         Token JWT del cual extraer el claim.
     * @param claimsResolver Función que resuelve el claim deseado a partir de los claims del token.
     * @return Claim extraído del token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims de un token JWT.
     *
     * @param token Token JWT del cual extraer los claims.
     * @return Objeto Claims que contiene todos los claims del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(obtenerKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Verifica si un token JWT ha expirado.
     *
     * @param token Token JWT a verificar.
     * @return true si el token ha expirado, false en caso contrario.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Genera un token JWT para los detalles de usuario proporcionados.
     *
     * @param userDetails Detalles del usuario para el cual generar el token.
     * @return Token JWT generado.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        String usuario = userDetails.getUsername();
        return obtenerToken(claims, usuario);
    }
	
	/** Constructor vacío. */
    public JwtUtil() {
		// Constructor vacío: Spring crea la instancia
	}
    
    
}
