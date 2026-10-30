package co.edu.unbosque.marketplace.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import co.edu.unbosque.marketplace.security.WebSocketAuthInterceptor;

/**
 * WebSocket con STOMP para mensajes y notificaciones en tiempo real. El cliente
 * se conecta a {@code ws://host:8081/ws} con el JWT en el frame CONNECT y se
 * suscribe a {@code /user/queue/mensajes} y {@code /user/queue/notificaciones};
 * cada usuario recibe solo lo suyo. Los mensajes se siguen enviando por REST.
 *
 * @see <a href="https://spring.io/guides/gs/messaging-stomp-websocket">Guía
 *      STOMP de Spring</a>
 * @see "RF-003, RF-029"
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final WebSocketAuthInterceptor authInterceptor;
	private final List<String> origenesPermitidos;

	public WebSocketConfig(WebSocketAuthInterceptor authInterceptor,
			@Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> origenesPermitidos) {
		this.authInterceptor = authInterceptor;
		this.origenesPermitidos = origenesPermitidos;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// Mismos orígenes que CORS (SecurityConfig)
		registry.addEndpoint("/ws").setAllowedOrigins(origenesPermitidos.stream().map(String::trim)
				.toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/queue");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authInterceptor);
	}
}
