package co.edu.unbosque.marketplace.security;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import co.edu.unbosque.marketplace.entity.Usuario;
import io.jsonwebtoken.JwtException;

/**
 * Seguridad de los frames STOMP. El navegador no puede poner el encabezado
 * Authorization al abrir el WebSocket, por eso el token viaja en el frame
 * CONNECT ({@code Authorization: Bearer <jwt>}) y se valida igual que en
 * {@link JwtAuthenticationFilter}. Además:
 * <ul>
 * <li>Solo se puede suscribir a los destinos propios
 * ({@code /user/queue/...}).</li>
 * <li>No se aceptan SEND: los mensajes se envían por REST. Así nadie puede
 * publicar un mensaje falso en la cola de otro usuario.</li>
 * </ul>
 * Si algo no cumple, el cliente recibe un frame ERROR y se cierra la conexión.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

	private static final String PREFIJO_BEARER = "Bearer ";
	private static final String NO_AUTENTICADO = "No autenticado: token ausente, inválido o expirado";

	private final JwtUtil jwtUtil;
	private final UserDetailsService userDetailsService;

	public WebSocketAuthInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
		this.jwtUtil = jwtUtil;
		this.userDetailsService = userDetailsService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		StompCommand comando = accessor.getCommand();
		if (StompCommand.CONNECT.equals(comando) || StompCommand.STOMP.equals(comando)) {
			accessor.setUser(autenticar(accessor.getFirstNativeHeader("Authorization")));
		} else if (StompCommand.SUBSCRIBE.equals(comando)) {
			String destino = accessor.getDestination();
			if (accessor.getUser() == null || destino == null
					|| !destino.startsWith("/user/queue/")) {
				throw new MessageDeliveryException("Solo puedes suscribirte a tus destinos (/user/queue/...).");
			}
		} else if (StompCommand.SEND.equals(comando)) {
			throw new MessageDeliveryException("Los mensajes se envían por la API REST.");
		}
		return message;
	}

	/**
	 * Usuario de la conexión. Su nombre es el id, así los services envían a
	 * {@code /user/{id}/queue/...} sin buscar ni descifrar el correo.
	 */
	private Principal autenticar(String encabezado) {
		if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
			String jwt = encabezado.substring(PREFIJO_BEARER.length());
			try {
				UserDetails userDetails = userDetailsService.loadUserByUsername(jwtUtil.extractUsername(jwt));
				// Un usuario desactivado o restringido no debe seguir usando un token viejo
				if (userDetails instanceof Usuario usuario
						&& Boolean.TRUE.equals(jwtUtil.validateToken(jwt, usuario)) && usuario.isEnabled()
						&& usuario.isAccountNonLocked()) {
					String id = String.valueOf(usuario.getIdUsuario());
					return () -> id;
				}
			} catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
				// Token mal formado, con firma inválida, expirado o de un usuario que ya no
				// existe: se rechaza igual que un token ausente
			}
		}
		throw new MessageDeliveryException(NO_AUTENTICADO);
	}
}
