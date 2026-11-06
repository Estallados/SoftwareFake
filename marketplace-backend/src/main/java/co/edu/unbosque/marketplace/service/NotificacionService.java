package co.edu.unbosque.marketplace.service;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.NotificacionDTO;
import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.entity.Notificacion;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.util.LimitesTexto;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Notificaciones dentro de la plataforma.
 * <p>
 * Los demás servicios llaman a {@link #enviar(Long, String, String)} cuando
 * ocurre algo que el usuario debe saber: reservas, cancelaciones, revisión o
 * bloqueo de alojamientos, reseñas, restricciones de la cuenta y mensajes
 * nuevos. Cada usuario ve y marca solo las suyas.
 * </p>
 */
@Service
public class NotificacionService {

	/** Una reserva quedó confirmada. */
	public static final String RESERVA_CONFIRMADA = "RESERVA_CONFIRMADA";

	/** Una reserva se canceló. */
	public static final String RESERVA_CANCELADA = "RESERVA_CANCELADA";

	/** Una reserva pasó a revisión porque bloquearon el alojamiento. */
	public static final String RESERVA_EN_REVISION = "RESERVA_EN_REVISION";

	/** El pago de una reserva no se pudo confirmar. */
	public static final String PAGO_NO_CONFIRMADO = "PAGO_NO_CONFIRMADO";

	/** El administrador aprobó una publicación. */
	public static final String ALOJAMIENTO_APROBADO = "ALOJAMIENTO_APROBADO";

	/** El administrador rechazó una publicación. */
	public static final String ALOJAMIENTO_RECHAZADO = "ALOJAMIENTO_RECHAZADO";

	/** El administrador bloqueó una publicación. */
	public static final String ALOJAMIENTO_BLOQUEADO = "ALOJAMIENTO_BLOQUEADO";

	/** Un alojamiento del anfitrión recibió una reseña. */
	public static final String RESENA_RECIBIDA = "RESENA_RECIBIDA";

	/** El administrador ocultó una reseña del huésped. */
	public static final String RESENA_OCULTA = "RESENA_OCULTA";

	/** El administrador restringió la cuenta. */
	public static final String CUENTA_RESTRINGIDA = "CUENTA_RESTRINGIDA";

	/** El administrador volvió a habilitar la cuenta. */
	public static final String CUENTA_LIBERADA = "CUENTA_LIBERADA";

	/** Llegó un mensaje nuevo en una conversación. */
	public static final String MENSAJE_RECIBIDO = "MENSAJE_RECIBIDO";

	/** Repositorio de notificaciones. */
	private final NotificacionRepository notificacionRepo;

	/** Envía la notificación por WebSocket. */
	private final TiempoRealService tiempoReal;

	/** Copia los datos de la entidad al DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param notificacionRepo repositorio de notificaciones
	 * @param tiempoReal servicio de envíos en tiempo real
	 * @param modelMapper copiador de entidades a DTO
	 */
	public NotificacionService(NotificacionRepository notificacionRepo, TiempoRealService tiempoReal,
			ModelMapper modelMapper) {
		this.notificacionRepo = notificacionRepo;
		this.tiempoReal = tiempoReal;
		this.modelMapper = modelMapper;
	}

	/**
	 * Registra una notificación para un usuario y se la envía en tiempo real a
	 * {@code /user/queue/notificaciones}.
	 *
	 * @param idUsuario destinatario
	 * @param tipo evento que la origina (una de las constantes de esta clase)
	 * @param mensaje texto para el usuario
	 */
	@Transactional
	public void enviar(Long idUsuario, String tipo, String mensaje) {
		if (idUsuario == null) {
			return;
		}
		Notificacion notificacion = new Notificacion();
		notificacion.setIdUsuario(idUsuario);
		notificacion.setTipo(tipo);
		notificacion.setMensaje(LimitesTexto.recortar(mensaje, LimitesTexto.MEDIO));
		notificacion.setFecha(LocalDateTime.now());
		notificacion.setLeida(false);
		tiempoReal.enviarAUsuario(idUsuario, TiempoRealService.NOTIFICACIONES,
				modelMapper.map(notificacionRepo.save(notificacion), NotificacionDTO.class));
	}

	/**
	 * Notificaciones del usuario autenticado, de la más reciente a la más
	 * antigua.
	 *
	 * @param idUsuario usuario
	 * @param pagina página y orden
	 * @return página de notificaciones
	 */
	public PaginaDTO<NotificacionDTO> listar(Long idUsuario, Pageable pagina) {
		return PaginaDTO.de(notificacionRepo.findByIdUsuario(idUsuario, pagina),
				n -> modelMapper.map(n, NotificacionDTO.class));
	}

	/**
	 * Cantidad de notificaciones sin leer del usuario (el contador de la
	 * campana).
	 *
	 * @param idUsuario usuario
	 * @return cantidad sin leer
	 */
	public long contarNoLeidas(Long idUsuario) {
		return notificacionRepo.countByIdUsuarioAndLeidaFalse(idUsuario);
	}

	/**
	 * Marca como leídas todas las notificaciones del usuario.
	 *
	 * @param idUsuario usuario
	 * @return cuántas notificaciones cambiaron
	 */
	@Transactional
	public int marcarTodasLeidas(Long idUsuario) {
		return notificacionRepo.marcarTodasLeidas(idUsuario);
	}

	/**
	 * Marca como leída una notificación propia.
	 *
	 * @param idNotificacion notificación a marcar
	 * @param idUsuario usuario dueño
	 * @return la notificación; o NO_ENCONTRADO si no existe o es de otro
	 *         usuario
	 */
	@Transactional
	public Resultado<NotificacionDTO> marcarLeida(Long idNotificacion, Long idUsuario) {
		return notificacionRepo.findByIdNotificacionAndIdUsuario(idNotificacion, idUsuario).map(n -> {
			n.setLeida(true);
			return Resultado.exito(modelMapper.map(notificacionRepo.save(n), NotificacionDTO.class));
		}).orElseGet(() -> Resultado.error(Resultado.NO_ENCONTRADO, "La notificación no existe."));
	}
}
