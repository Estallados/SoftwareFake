package co.edu.unbosque.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Envíos en tiempo real por WebSocket/STOMP a un usuario concreto. El cliente
 * los recibe en {@code /user/queue/<destino>}; el nombre de cada sesión es el
 * id del usuario ({@code WebSocketAuthInterceptor}).
 * <p>
 * Si hay una transacción en curso, el envío espera a que se confirme: así nadie
 * recibe un mensaje o una notificación que al final se revirtió. Un fallo del
 * envío no afecta la operación, que ya quedó guardada y se puede consultar por
 * la API REST.
 * </p>
 *
 * @see "RF-003, RF-029"
 */
@Service
public class TiempoRealService {

	/** Mensajes de las conversaciones del usuario. */
	public static final String MENSAJES = "/queue/mensajes";
	/** Notificaciones nuevas del usuario. */
	public static final String NOTIFICACIONES = "/queue/notificaciones";

	private static final Logger LOG = LoggerFactory.getLogger(TiempoRealService.class);

	private final SimpMessagingTemplate plantilla;

	public TiempoRealService(SimpMessagingTemplate plantilla) {
		this.plantilla = plantilla;
	}

	/**
	 * Envía {@code carga} (como JSON) a todas las sesiones abiertas del usuario.
	 *
	 * @param idUsuario Destinatario; si es null no se envía nada.
	 * @param destino   Una de las constantes de esta clase.
	 * @param carga     DTO a enviar.
	 */
	public void enviarAUsuario(Long idUsuario, String destino, Object carga) {
		if (idUsuario == null) {
			return;
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					enviarAhora(idUsuario, destino, carga);
				}
			});
		} else {
			enviarAhora(idUsuario, destino, carga);
		}
	}

	private void enviarAhora(Long idUsuario, String destino, Object carga) {
		try {
			plantilla.convertAndSendToUser(String.valueOf(idUsuario), destino, carga);
		} catch (MessagingException e) {
			LOG.warn("No se pudo enviar {} al usuario {} por WebSocket: {}", destino, idUsuario, e.getMessage());
		}
	}
}
