package co.edu.unbosque.marketplace.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.dto.EventoAuditoriaDTO;
import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.entity.EventoAuditoria;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.AuditoriaRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Registro de auditoría del marketplace. Cada evento se guarda en la tabla
 * eventoAuditoria y además se escribe como una línea JSON en el log
 * {@code AUDITORIA}.
 * <p>
 * El guardado usa una transacción propia (REQUIRES_NEW): así el evento queda
 * aunque la operación auditada se revierta, y también se guardan las
 * operaciones rechazadas. Si la auditoría falla, la operación auditada no se
 * interrumpe; el evento sigue quedando en el log.
 * </p>
 */
@Service
public class AuditoriaService {

	/** Un usuario se registró. */
	public static final String REGISTRO_USUARIO = "REGISTRO_USUARIO";

	/** Se creó un administrador. */
	public static final String CREACION_ADMINISTRADOR = "CREACION_ADMINISTRADOR";

	/** Intento de inicio de sesión. */
	public static final String LOGIN = "LOGIN";

	/** Un usuario cambió sus datos. */
	public static final String PERFIL_ACTUALIZADO = "PERFIL_ACTUALIZADO";

	/** Un usuario desactivó su cuenta. */
	public static final String CUENTA_DESACTIVADA = "CUENTA_DESACTIVADA";

	/** Un usuario reactivó su cuenta. */
	public static final String CUENTA_REACTIVADA = "CUENTA_REACTIVADA";

	/** Se pidió recuperar la contraseña. */
	public static final String RECUPERACION_SOLICITADA = "RECUPERACION_SOLICITADA";

	/** Se cambió la contraseña con un token de recuperación. */
	public static final String CONTRASENIA_RESTABLECIDA = "CONTRASENIA_RESTABLECIDA";

	/** Se envió un código de verificación del correo. */
	public static final String CODIGO_VERIFICACION_ENVIADO = "CODIGO_VERIFICACION_ENVIADO";

	/** Un usuario confirmó su correo. */
	public static final String CORREO_VERIFICADO = "CORREO_VERIFICADO";

	/** Se creó un alojamiento. */
	public static final String ALOJAMIENTO_CREADO = "ALOJAMIENTO_CREADO";

	/** Se editó un alojamiento. */
	public static final String ALOJAMIENTO_ACTUALIZADO = "ALOJAMIENTO_ACTUALIZADO";

	/** Un alojamiento cambió de estado. */
	public static final String ALOJAMIENTO_CAMBIO_ESTADO = "ALOJAMIENTO_CAMBIO_ESTADO";

	/** Se subió, eliminó o cambió la portada de una imagen. */
	public static final String IMAGEN_ALOJAMIENTO = "IMAGEN_ALOJAMIENTO";

	/** Se creó, editó o eliminó un servicio adicional. */
	public static final String SERVICIO_ADICIONAL = "SERVICIO_ADICIONAL";

	/** Se creó, editó o eliminó un período de disponibilidad. */
	public static final String DISPONIBILIDAD = "DISPONIBILIDAD";

	/** Un administrador cambió las tarifas globales. */
	public static final String TARIFA_ACTUALIZADA = "TARIFA_ACTUALIZADA";

	/** Se creó una reserva. */
	public static final String RESERVA_CREADA = "RESERVA_CREADA";

	/** Se intentó un pago. */
	public static final String PAGO = "PAGO";

	/** Se canceló una reserva. */
	public static final String CANCELACION = "CANCELACION";

	/** Se publicó una reseña. */
	public static final String RESENA_CREADA = "RESENA_CREADA";

	/** Un administrador ocultó o mostró una reseña. */
	public static final String RESENA_MODERADA = "RESENA_MODERADA";

	/** Un administrador restringió una cuenta. */
	public static final String USUARIO_RESTRINGIDO = "USUARIO_RESTRINGIDO";

	/** Un administrador volvió a habilitar una cuenta. */
	public static final String USUARIO_LIBERADO = "USUARIO_LIBERADO";

	/** Se exportó un reporte. */
	public static final String REPORTE_EXPORTADO = "REPORTE_EXPORTADO";

	/** Se activó, confirmó o desactivó el segundo factor. */
	public static final String SEGUNDO_FACTOR = "SEGUNDO_FACTOR";

	/** Alguien intentó entrar a una ruta sin el rol necesario. */
	public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";

	/** Ocurrió un error inesperado en el servidor. */
	public static final String ERROR_INTERNO = "ERROR_INTERNO";

	/** Entidad auditada: usuario. */
	public static final String ENTIDAD_USUARIO = "USUARIO";

	/** Entidad auditada: alojamiento. */
	public static final String ENTIDAD_ALOJAMIENTO = "ALOJAMIENTO";

	/** Entidad auditada: período de disponibilidad. */
	public static final String ENTIDAD_PERIODO = "PERIODO_DISPONIBILIDAD";

	/** Entidad auditada: configuración de tarifas. */
	public static final String ENTIDAD_CONFIGURACION = "CONFIGURACION_TARIFA";

	/** Entidad auditada: reserva. */
	public static final String ENTIDAD_RESERVA = "RESERVA";

	/** Entidad auditada: reseña. */
	public static final String ENTIDAD_RESENA = "RESENA";

	/** Entidad auditada: reporte. */
	public static final String ENTIDAD_REPORTE = "REPORTE";

	/** Log de la clase. */
	private static final Logger LOG = LoggerFactory.getLogger(AuditoriaService.class);

	/** Log de auditoría: una línea JSON por evento. */
	private static final Logger LOG_AUDITORIA = LoggerFactory.getLogger("AUDITORIA");

	/** Largo máximo del detalle que se guarda. */
	private static final int MAX_DETALLE = 1000;

	/** Repositorio de solo inserción de la auditoría. */
	private final AuditoriaRepository auditoriaRepo;

	/**
	 * Transacción independiente para guardar el evento aunque la operación se
	 * revierta.
	 */
	private final TransactionTemplate transaccionPropia;

	/** Convierte el evento a JSON para el log. */
	private final ObjectMapper objectMapper;

	/**
	 * Crea el servicio y prepara la transacción propia (REQUIRES_NEW).
	 *
	 * @param auditoriaRepo repositorio de la auditoría
	 * @param transactionManager administrador de transacciones de Spring
	 * @param objectMapper conversor a JSON
	 */
	public AuditoriaService(AuditoriaRepository auditoriaRepo, PlatformTransactionManager transactionManager,
			ObjectMapper objectMapper) {
		this.auditoriaRepo = auditoriaRepo;
		this.objectMapper = objectMapper;
		this.transaccionPropia = new TransactionTemplate(transactionManager);
		this.transaccionPropia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/**
	 * Registra un evento de auditoría.
	 *
	 * @param idActor   Usuario que hizo la acción; null si no se conoce.
	 * @param accion    Acción auditada (constantes de esta clase).
	 * @param entidad   Tipo de recurso afectado; puede ser null.
	 * @param idEntidad Id del recurso afectado; puede ser null.
	 * @param resultado EXITOSO, RECHAZADO o ERROR.
	 * @param detalle   Descripción corta, sin contraseñas ni datos privados.
	 */
	public void registrar(Long idActor, String accion, String entidad, Long idEntidad, ResultadoAuditoria resultado,
			String detalle) {
		EventoAuditoria evento = new EventoAuditoria();
		evento.setIdUsuario(idActor);
		evento.setAccion(accion);
		evento.setEntidad(entidad);
		evento.setIdEntidad(idEntidad);
		evento.setResultado(resultado);
		evento.setDetalle(recortar(detalle));
		evento.setFecha(LocalDateTime.now());

		escribirLog(evento);

		try {
			transaccionPropia.executeWithoutResult(estado -> auditoriaRepo.save(evento));
		} catch (RuntimeException e) {
			// La auditoría nunca debe tumbar la operación auditada; el evento ya quedó en
			// el log
			LOG.error("No se pudo guardar el evento de auditoría {}", accion, e);
		}
	}

	/**
	 * Consulta paginada de la auditoría con filtros opcionales. Solo lectura.
	 *
	 * @param accion acción exacta, o {@code null}
	 * @param idActor usuario que hizo la acción, o {@code null}
	 * @param desde fecha mínima, o {@code null}
	 * @param hasta fecha máxima, o {@code null}
	 * @param pagina página solicitada
	 * @return página de eventos, del más reciente al más antiguo
	 */
	public PaginaDTO<EventoAuditoriaDTO> consultar(String accion, Long idActor, LocalDateTime desde,
			LocalDateTime hasta, Pageable pagina) {
		String filtroAccion = accion == null || accion.isBlank() ? null : accion.trim().toUpperCase();
		return PaginaDTO.de(auditoriaRepo.buscar(filtroAccion, idActor, desde, hasta, pagina),
				AuditoriaService::convertirDTO);
	}

	/**
	 * Detalle de un evento de auditoría.
	 *
	 * @param idEvento evento a consultar
	 * @return el evento; o NO_ENCONTRADO si no existe
	 */
	public Resultado<EventoAuditoriaDTO> obtener(Long idEvento) {
		return auditoriaRepo.findById(idEvento).map(e -> Resultado.exito(convertirDTO(e)))
				.orElseGet(() -> Resultado.error(Resultado.NO_ENCONTRADO, "El evento de auditoría no existe."));
	}

	/**
	 * Convierte un evento a DTO.
	 *
	 * @param evento evento guardado
	 * @return evento para la API
	 */
	private static EventoAuditoriaDTO convertirDTO(EventoAuditoria evento) {
		EventoAuditoriaDTO dto = new EventoAuditoriaDTO();
		dto.setIdEvento(evento.getIdEvento());
		dto.setAccion(evento.getAccion());
		dto.setFecha(evento.getFecha());
		dto.setDetalle(evento.getDetalle());
		dto.setIdUsuario(evento.getIdUsuario());
		dto.setEntidad(evento.getEntidad());
		dto.setIdEntidad(evento.getIdEntidad());
		dto.setResultado(evento.getResultado());
		return dto;
	}

	/**
	 * Escribe el evento como una línea JSON en el log {@code AUDITORIA}.
	 *
	 * @param evento evento a escribir
	 */
	private void escribirLog(EventoAuditoria evento) {
		Map<String, Object> linea = new LinkedHashMap<>();
		linea.put("fecha", evento.getFecha().toString());
		linea.put("accion", evento.getAccion());
		linea.put("resultado", evento.getResultado());
		linea.put("idActor", evento.getIdUsuario());
		linea.put("entidad", evento.getEntidad());
		linea.put("idEntidad", evento.getIdEntidad());
		linea.put("detalle", evento.getDetalle());

		try {
			LOG_AUDITORIA.info(objectMapper.writeValueAsString(linea));
		} catch (JsonProcessingException e) {
			LOG.error("No se pudo escribir el evento de auditoría {} en el log", evento.getAccion(), e);
		}
	}

	/**
	 * Corta el detalle a {@link #MAX_DETALLE} caracteres para que quepa en la
	 * columna.
	 *
	 * @param detalle detalle original; puede ser {@code null}
	 * @return el detalle, recortado si hacía falta
	 */
	private static String recortar(String detalle) {
		if (detalle == null || detalle.length() <= MAX_DETALLE) {
			return detalle;
		}
		return detalle.substring(0, MAX_DETALLE);
	}
}
