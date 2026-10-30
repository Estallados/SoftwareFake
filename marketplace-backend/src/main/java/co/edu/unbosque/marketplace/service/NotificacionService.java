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
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Notificaciones dentro de la plataforma. Los demás services llaman a
 * {@link #enviar(Long, String, String)} cuando ocurre un evento relevante
 * (reservas, cancelaciones, revisión o bloqueo de alojamientos, reseñas,
 * restricciones y mensajes). Cada usuario ve y marca solo las suyas.
 *
 * @see "RF-003, CA-010, CA-013"
 */
@Service
public class NotificacionService {

	/** Tipos de evento que generan notificación. */
	public static final String RESERVA_CONFIRMADA = "RESERVA_CONFIRMADA";
	public static final String RESERVA_CANCELADA = "RESERVA_CANCELADA";
	public static final String RESERVA_EN_REVISION = "RESERVA_EN_REVISION";
	public static final String PAGO_NO_CONFIRMADO = "PAGO_NO_CONFIRMADO";
	public static final String ALOJAMIENTO_APROBADO = "ALOJAMIENTO_APROBADO";
	public static final String ALOJAMIENTO_RECHAZADO = "ALOJAMIENTO_RECHAZADO";
	public static final String ALOJAMIENTO_BLOQUEADO = "ALOJAMIENTO_BLOQUEADO";
	public static final String RESENA_RECIBIDA = "RESENA_RECIBIDA";
	public static final String RESENA_OCULTA = "RESENA_OCULTA";
	public static final String CUENTA_RESTRINGIDA = "CUENTA_RESTRINGIDA";
	public static final String CUENTA_LIBERADA = "CUENTA_LIBERADA";
	public static final String MENSAJE_RECIBIDO = "MENSAJE_RECIBIDO";

	private final NotificacionRepository notificacionRepo;
	private final TiempoRealService tiempoReal;
	private final ModelMapper modelMapper;

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
	 * @param idUsuario Destinatario.
	 * @param tipo      Evento que la origina (constantes de esta clase).
	 * @param mensaje   Texto para el usuario.
	 * @see "CA-010"
	 */
	@Transactional
	public void enviar(Long idUsuario, String tipo, String mensaje) {
		if (idUsuario == null) {
			return;
		}
		Notificacion notificacion = new Notificacion();
		notificacion.setIdUsuario(idUsuario);
		notificacion.setTipo(tipo);
		notificacion.setMensaje(mensaje);
		notificacion.setFecha(LocalDateTime.now());
		notificacion.setLeida(false);
		tiempoReal.enviarAUsuario(idUsuario, TiempoRealService.NOTIFICACIONES,
				modelMapper.map(notificacionRepo.save(notificacion), NotificacionDTO.class));
	}

	/**
	 * Notificaciones del usuario autenticado, de la más reciente a la más antigua.
	 *
	 * @see "CA-013"
	 */
	public PaginaDTO<NotificacionDTO> listar(Long idUsuario, Pageable pagina) {
		return PaginaDTO.de(notificacionRepo.findByIdUsuario(idUsuario, pagina),
				n -> modelMapper.map(n, NotificacionDTO.class));
	}

	/**
	 * Cantidad de notificaciones sin leer del usuario (contador de la campana).
	 *
	 * @see "CA-013"
	 */
	public long contarNoLeidas(Long idUsuario) {
		return notificacionRepo.countByIdUsuarioAndLeidaFalse(idUsuario);
	}

	/**
	 * Marca como leídas todas las notificaciones del usuario.
	 *
	 * @return cuántas notificaciones cambiaron.
	 */
	@Transactional
	public int marcarTodasLeidas(Long idUsuario) {
		return notificacionRepo.marcarTodasLeidas(idUsuario);
	}

	/**
	 * Marca como leída una notificación propia.
	 *
	 * @return 0 con la notificación; 1 si no existe o es de otro usuario.
	 */
	@Transactional
	public Resultado<NotificacionDTO> marcarLeida(Long idNotificacion, Long idUsuario) {
		return notificacionRepo.findByIdNotificacionAndIdUsuario(idNotificacion, idUsuario).map(n -> {
			n.setLeida(true);
			return Resultado.exito(modelMapper.map(notificacionRepo.save(n), NotificacionDTO.class));
		}).orElseGet(() -> Resultado.error(Resultado.NO_ENCONTRADO, "La notificación no existe."));
	}
}
