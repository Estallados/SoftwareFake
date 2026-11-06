package co.edu.unbosque.marketplace.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Mensajería entre un huésped y el anfitrión de un alojamiento. Solo los dos
 * participantes pueden ver la conversación y escribir en ella.
 */
@Service
public class MensajeriaService {

	/** Largo máximo de un mensaje. */
	private static final int LARGO_MAXIMO = 2000;

	/** Mensaje cuando la conversación no existe. */
	private static final String NO_EXISTE = "La conversación no existe.";

	/** Repositorio de conversaciones. */
	private final ConversacionRepository conversacionRepo;

	/** Repositorio de mensajes. */
	private final MensajeRepository mensajeRepo;

	/** Repositorio de alojamientos. */
	private final AlojamientoRepository alojamientoRepo;

	/** Repositorio de usuarios, para los nombres. */
	private final UsuarioRepository usuarioRepo;

	/** Avisa al destinatario de un mensaje nuevo. */
	private final NotificacionService notificacionService;

	/** Envía los mensajes por WebSocket. */
	private final TiempoRealService tiempoReal;

	/** Copia los datos de las entidades a los DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param conversacionRepo repositorio de conversaciones
	 * @param mensajeRepo repositorio de mensajes
	 * @param alojamientoRepo repositorio de alojamientos
	 * @param usuarioRepo repositorio de usuarios
	 * @param notificacionService servicio de notificaciones
	 * @param tiempoReal servicio de envíos en tiempo real
	 * @param modelMapper copiador de entidades a DTO
	 */
	public MensajeriaService(ConversacionRepository conversacionRepo, MensajeRepository mensajeRepo,
			AlojamientoRepository alojamientoRepo, UsuarioRepository usuarioRepo,
			NotificacionService notificacionService,
			TiempoRealService tiempoReal, ModelMapper modelMapper) {
		this.conversacionRepo = conversacionRepo;
		this.mensajeRepo = mensajeRepo;
		this.alojamientoRepo = alojamientoRepo;
		this.usuarioRepo = usuarioRepo;
		this.notificacionService = notificacionService;
		this.tiempoReal = tiempoReal;
		this.modelMapper = modelMapper;
	}

	/**
	 * Inicia (o retoma, si ya existe) la conversación del usuario con el
	 * anfitrión de un alojamiento, con un primer mensaje opcional.
	 *
	 * @param datos alojamiento y primer mensaje
	 * @param idUsuario huésped que inicia la conversación
	 * @return la conversación; NO_ENCONTRADO si el alojamiento no existe;
	 *         DATOS_INVALIDOS si el mensaje no es válido; SIN_PERMISO si es su
	 *         propio alojamiento
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
			return Resultado.error(Resultado.SIN_PERMISO,
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
		return Resultado.exito(convertirDTO(conversacion));
	}

	/**
	 * Indica si el usuario ya tiene una conversación sobre el alojamiento.
	 *
	 * @param idUsuario usuario
	 * @param idAlojamiento alojamiento
	 * @return {@code true} si ya existe
	 */
	public boolean tieneConversacion(Long idUsuario, Long idAlojamiento) {
		return idAlojamiento != null
				&& conversacionRepo.findByIdUsuarioAndIdAlojamiento(idUsuario, idAlojamiento).isPresent();
	}

	/**
	 * Conversaciones en las que participa el usuario, como huésped o como
	 * anfitrión.
	 *
	 * @param idUsuario usuario
	 * @param pagina página y orden
	 * @return página de conversaciones
	 */
	public PaginaDTO<ConversacionDTO> listar(Long idUsuario, Pageable pagina) {
		return PaginaDTO.de(conversacionRepo.buscarDeParticipante(idUsuario, pagina), this::convertirDTO);
	}

	/**
	 * Mensajes de una conversación; solo para sus participantes.
	 *
	 * @param idConversacion conversación
	 * @param idUsuario usuario que consulta
	 * @param pagina página y orden
	 * @return los mensajes; NO_ENCONTRADO si no existe; SIN_PERMISO si no
	 *         participa
	 */
	public Resultado<PaginaDTO<MensajeDTO>> mensajes(Long idConversacion, Long idUsuario, Pageable pagina) {
		Resultado<Conversacion> carga = cargarComoParticipante(idConversacion, idUsuario);
		if (!carga.esExito()) {
			return Resultado.error(carga.getCodigo(), carga.getMensaje());
		}
		Map<Long, String> nombres = nombres(carga.getDato());
		return Resultado.exito(PaginaDTO.de(mensajeRepo.findByIdConversacion(idConversacion, pagina),
				m -> convertirDTO(m, nombres)));
	}

	/**
	 * Envía un mensaje en una conversación propia y notifica al otro
	 * participante.
	 *
	 * @param idConversacion conversación
	 * @param contenido texto del mensaje
	 * @param idUsuario remitente
	 * @return el mensaje; NO_ENCONTRADO si no existe; DATOS_INVALIDOS si está
	 *         vacío o es muy largo; SIN_PERMISO si no participa
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
		return Resultado.exito(guardarMensaje(carga.getDato(), contenido, idUsuario));
	}

	/**
	 * Guarda el mensaje, lo envía en tiempo real a los dos participantes
	 * ({@code /user/queue/mensajes}; al remitente le sirve para sincronizar
	 * sus otras pestañas) y notifica al destinatario.
	 *
	 * @param conversacion conversación
	 * @param contenido texto del mensaje
	 * @param idRemitente usuario que lo envía
	 * @return mensaje guardado
	 */
	private MensajeDTO guardarMensaje(Conversacion conversacion, String contenido, Long idRemitente) {
		Mensaje mensaje = new Mensaje();
		mensaje.setIdConversacion(conversacion.getIdConversacion());
		mensaje.setIdUsuario(idRemitente);
		mensaje.setContenido(contenido.trim());
		mensaje.setFechaEnvio(LocalDateTime.now());
		MensajeDTO guardado = convertirDTO(mensajeRepo.save(mensaje), nombres(conversacion));

		Long destinatario = idRemitente.equals(conversacion.getIdUsuario()) ? conversacion.getIdAnfitrion()
				: conversacion.getIdUsuario();
		tiempoReal.enviarAUsuario(destinatario, TiempoRealService.MENSAJES, guardado);
		tiempoReal.enviarAUsuario(idRemitente, TiempoRealService.MENSAJES, guardado);
		notificacionService.enviar(destinatario, NotificacionService.MENSAJE_RECIBIDO,
				"Tienes un mensaje nuevo en la conversación " + conversacion.getIdConversacion() + ".");
		return guardado;
	}

	/**
	 * Convierte una conversación a DTO con los nombres de los participantes.
	 *
	 * @param conversacion conversación
	 * @return conversación para la API
	 */
	private ConversacionDTO convertirDTO(Conversacion conversacion) {
		ConversacionDTO dto = modelMapper.map(conversacion, ConversacionDTO.class);
		Map<Long, String> nombres = nombres(conversacion);
		dto.setNombreUsuario(nombres.get(conversacion.getIdUsuario()));
		dto.setNombreAnfitrion(nombres.get(conversacion.getIdAnfitrion()));
		return dto;
	}

	/**
	 * Convierte un mensaje a DTO con el nombre del remitente.
	 *
	 * @param mensaje mensaje
	 * @param nombres nombres de los participantes por id
	 * @return mensaje para la API
	 */
	private MensajeDTO convertirDTO(Mensaje mensaje, Map<Long, String> nombres) {
		MensajeDTO dto = modelMapper.map(mensaje, MensajeDTO.class);
		dto.setNombreUsuario(nombres.get(mensaje.getIdUsuario()));
		return dto;
	}

	/**
	 * Nombres de los dos participantes, por id (en una sola consulta).
	 *
	 * @param conversacion conversación
	 * @return mapa de id a nombre
	 */
	private Map<Long, String> nombres(Conversacion conversacion) {
		Map<Long, String> nombres = new HashMap<>();
		usuarioRepo.findAllById(List.of(conversacion.getIdUsuario(), conversacion.getIdAnfitrion()))
				.forEach(u -> nombres.put(u.getIdUsuario(), u.getNombre()));
		return nombres;
	}

	/**
	 * Carga la conversación verificando que el usuario participe en ella.
	 *
	 * @param idConversacion conversación
	 * @param idUsuario usuario
	 * @return la conversación; NO_ENCONTRADO si no existe; SIN_PERMISO si no
	 *         participa
	 */
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
