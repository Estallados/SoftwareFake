package co.edu.unbosque.marketplace.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.ConversacionDTO;
import co.edu.unbosque.marketplace.dto.MensajeDTO;
import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.Conversacion;
import co.edu.unbosque.marketplace.entity.EstadoConversacion;
import co.edu.unbosque.marketplace.entity.Mensaje;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.ConversacionRepository;
import co.edu.unbosque.marketplace.repository.MensajeRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Mensajería entre un huésped y el anfitrión de un alojamiento. Solo los dos
 * participantes pueden ver la conversación y escribir en ella.
 *
 * @see "RF-029, CA-165 a CA-169"
 */
@Service
public class MensajeriaService {

	private static final int LARGO_MAXIMO = 2000;
	private static final String NO_EXISTE = "La conversación no existe.";

	private final ConversacionRepository conversacionRepo;
	private final MensajeRepository mensajeRepo;
	private final AlojamientoRepository alojamientoRepo;
	private final NotificacionService notificacionService;
	private final ModelMapper modelMapper;

	public MensajeriaService(ConversacionRepository conversacionRepo, MensajeRepository mensajeRepo,
			AlojamientoRepository alojamientoRepo, NotificacionService notificacionService, ModelMapper modelMapper) {
		this.conversacionRepo = conversacionRepo;
		this.mensajeRepo = mensajeRepo;
		this.alojamientoRepo = alojamientoRepo;
		this.notificacionService = notificacionService;
		this.modelMapper = modelMapper;
	}

	/**
	 * Inicia (o retoma) la conversación del usuario con el anfitrión de un
	 * alojamiento, con un primer mensaje opcional.
	 *
	 * @return 0 con la conversación; 1 el alojamiento no existe; 2 es su propio
	 *         alojamiento o el mensaje es inválido.
	 * @see "CA-169"
	 */
	@Transactional
	public Resultado<ConversacionDTO> iniciar(ConversacionDTO datos, Long idUsuario) {
		if (datos == null || datos.getIdAlojamiento() == null) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "Debes indicar el alojamiento.");
		}
		Optional<Alojamiento> alojamiento = alojamientoRepo.findById(datos.getIdAlojamiento());
		if (alojamiento.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe.");
		}
		if (alojamiento.get().getIdAnfitrion().equals(idUsuario)) {
			return Resultado.error(Resultado.DATOS_INVALIDOS,
					"No puedes iniciar una conversación con tu propio " + "alojamiento.");
		}
		if (datos.getMensaje() != null && datos.getMensaje().length() > LARGO_MAXIMO) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El mensaje no puede superar 2000 caracteres.");
		}

		Conversacion conversacion = conversacionRepo
				.findByIdUsuarioAndIdAlojamiento(idUsuario, datos.getIdAlojamiento()).orElseGet(() -> {
					Conversacion nueva = new Conversacion();
					nueva.setIdUsuario(idUsuario);
					nueva.setIdAnfitrion(alojamiento.get().getIdAnfitrion());
					nueva.setIdAlojamiento(alojamiento.get().getIdAlojamiento());
					nueva.setEstado(EstadoConversacion.ACTIVA);
					nueva.setFechaCreacion(LocalDateTime.now());
					return conversacionRepo.save(nueva);
				});
		if (datos.getMensaje() != null && !datos.getMensaje().isBlank()) {
			guardarMensaje(conversacion, datos.getMensaje(), idUsuario);
		}
		return Resultado.exito(modelMapper.map(conversacion, ConversacionDTO.class));
	}

	/** Conversaciones en las que participa el usuario. */
	public PaginaDTO<ConversacionDTO> listar(Long idUsuario, Pageable pagina) {
		return PaginaDTO.de(conversacionRepo.buscarDeParticipante(idUsuario, pagina),
				c -> modelMapper.map(c, ConversacionDTO.class));
	}

	/**
	 * Mensajes de una conversación, solo para sus participantes.
	 *
	 * @return 0 con los mensajes; 1 no existe; 3 no participa.
	 * @see "CA-167"
	 */
	public Resultado<PaginaDTO<MensajeDTO>> mensajes(Long idConversacion, Long idUsuario, Pageable pagina) {
		Resultado<Conversacion> carga = cargarComoParticipante(idConversacion, idUsuario);
		if (!carga.esExito()) {
			return Resultado.error(carga.getCodigo(), carga.getMensaje());
		}
		return Resultado.exito(PaginaDTO.de(mensajeRepo.findByIdConversacion(idConversacion, pagina),
				m -> modelMapper.map(m, MensajeDTO.class)));
	}

	/**
	 * Envía un mensaje en una conversación propia y notifica al otro participante.
	 *
	 * @return 0 con el mensaje; 1 no existe; 2 mensaje vacío o muy largo; 3 no
	 *         participa.
	 * @see "CA-165, CA-166, CA-168"
	 */
	@Transactional
	public Resultado<MensajeDTO> enviar(Long idConversacion, String contenido, Long idUsuario) {
		if (contenido == null || contenido.isBlank() || contenido.length() > LARGO_MAXIMO) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El mensaje debe tener entre 1 y 2000 caracteres.");
		}
		Resultado<Conversacion> carga = cargarComoParticipante(idConversacion, idUsuario);
		if (!carga.esExito()) {
			return Resultado.error(carga.getCodigo(), carga.getMensaje());
		}
		return Resultado
				.exito(modelMapper.map(guardarMensaje(carga.getDato(), contenido, idUsuario), MensajeDTO.class));
	}

	private Mensaje guardarMensaje(Conversacion conversacion, String contenido, Long idRemitente) {
		Mensaje mensaje = new Mensaje();
		mensaje.setIdConversacion(conversacion.getIdConversacion());
		mensaje.setIdUsuario(idRemitente);
		mensaje.setContenido(contenido.trim());
		mensaje.setFechaEnvio(LocalDateTime.now());
		Mensaje guardado = mensajeRepo.save(mensaje);

		Long destinatario = idRemitente.equals(conversacion.getIdUsuario()) ? conversacion.getIdAnfitrion()
				: conversacion.getIdUsuario();
		notificacionService.enviar(destinatario, NotificacionService.MENSAJE_RECIBIDO,
				"Tienes un mensaje nuevo en la conversación " + conversacion.getIdConversacion() + ".");
		return guardado;
	}

	private Resultado<Conversacion> cargarComoParticipante(Long idConversacion, Long idUsuario) {
		Optional<Conversacion> conversacion = conversacionRepo.findById(idConversacion);
		if (conversacion.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
		}
		if (!idUsuario.equals(conversacion.get().getIdUsuario())
				&& !idUsuario.equals(conversacion.get().getIdAnfitrion())) {
			return Resultado.error(Resultado.SIN_PERMISO, "No participas en esta conversación.");
		}
		return Resultado.exito(conversacion.get());
	}
}
