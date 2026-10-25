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
 *
 * @see "RNF-023, RNF-024, RNF-025, RNF-026"
 */
@Service
public class AuditoriaService {

    /** Acciones auditadas del módulo de identidad. */
    public static final String REGISTRO_USUARIO = "REGISTRO_USUARIO";
    public static final String CREACION_ADMINISTRADOR = "CREACION_ADMINISTRADOR";
    public static final String LOGIN = "LOGIN";
    public static final String PERFIL_ACTUALIZADO = "PERFIL_ACTUALIZADO";
    public static final String CUENTA_DESACTIVADA = "CUENTA_DESACTIVADA";
    public static final String CUENTA_REACTIVADA = "CUENTA_REACTIVADA";
    public static final String RECUPERACION_SOLICITADA = "RECUPERACION_SOLICITADA";
    public static final String CONTRASENIA_RESTABLECIDA = "CONTRASENIA_RESTABLECIDA";
    public static final String CODIGO_VERIFICACION_ENVIADO = "CODIGO_VERIFICACION_ENVIADO";
    public static final String CORREO_VERIFICADO = "CORREO_VERIFICADO";

    /** Acciones de alojamientos, disponibilidad y tarifas. */
    public static final String ALOJAMIENTO_CREADO = "ALOJAMIENTO_CREADO";
    public static final String ALOJAMIENTO_ACTUALIZADO = "ALOJAMIENTO_ACTUALIZADO";
    public static final String ALOJAMIENTO_CAMBIO_ESTADO = "ALOJAMIENTO_CAMBIO_ESTADO";
    public static final String IMAGEN_ALOJAMIENTO = "IMAGEN_ALOJAMIENTO";
    public static final String SERVICIO_ADICIONAL = "SERVICIO_ADICIONAL";
    public static final String DISPONIBILIDAD = "DISPONIBILIDAD";
    public static final String TARIFA_ACTUALIZADA = "TARIFA_ACTUALIZADA";

    /** Acciones de reservas, pagos, cancelaciones y reseñas. */
    public static final String RESERVA_CREADA = "RESERVA_CREADA";
    public static final String PAGO = "PAGO";
    public static final String CANCELACION = "CANCELACION";
    public static final String RESENA_CREADA = "RESENA_CREADA";
    public static final String RESENA_MODERADA = "RESENA_MODERADA";

    /** Acciones administrativas y opcionales. */
    public static final String USUARIO_RESTRINGIDO = "USUARIO_RESTRINGIDO";
    public static final String USUARIO_LIBERADO = "USUARIO_LIBERADO";
    public static final String REPORTE_EXPORTADO = "REPORTE_EXPORTADO";
    public static final String SEGUNDO_FACTOR = "SEGUNDO_FACTOR";

    /** Acciones transversales. */
    public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    /** Tipos de entidad auditada. */
    public static final String ENTIDAD_USUARIO = "USUARIO";
    public static final String ENTIDAD_ALOJAMIENTO = "ALOJAMIENTO";
    public static final String ENTIDAD_PERIODO = "PERIODO_DISPONIBILIDAD";
    public static final String ENTIDAD_CONFIGURACION = "CONFIGURACION_TARIFA";
    public static final String ENTIDAD_RESERVA = "RESERVA";
    public static final String ENTIDAD_RESENA = "RESENA";
    public static final String ENTIDAD_REPORTE = "REPORTE";

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaService.class);

    /** Log de auditoría: una línea JSON por evento. */
    private static final Logger LOG_AUDITORIA = LoggerFactory.getLogger("AUDITORIA");

    private static final int MAX_DETALLE = 1000;

    private final AuditoriaRepository auditoriaRepo;
    private final TransactionTemplate transaccionPropia;
    private final ObjectMapper objectMapper;

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
            // La auditoría nunca debe tumbar la operación auditada; el evento ya quedó en el log
            LOG.error("No se pudo guardar el evento de auditoría {}", accion, e);
        }
    }

    /**
     * Consulta paginada de la auditoría con filtros opcionales. Solo lectura.
     *
     * @param accion  Acción exacta, o null.
     * @param idActor Usuario que hizo la acción, o null.
     * @param desde   Fecha mínima, o null.
     * @param hasta   Fecha máxima, o null.
     * @param pagina  Página solicitada.
     * @return Página de eventos, del más reciente al más antiguo.
     * @see "RF-015, CA-062, CA-139"
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
     * @return 0 con el evento; 1 si no existe.
     * @see "RF-015, CA-064"
     */
    public Resultado<EventoAuditoriaDTO> obtener(Long idEvento) {
        return auditoriaRepo.findById(idEvento).map(e -> Resultado.exito(convertirDTO(e)))
                .orElseGet(() -> Resultado.error(Resultado.NO_ENCONTRADO, "El evento de auditoría no existe."));
    }

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

    private static String recortar(String detalle) {
        if (detalle == null || detalle.length() <= MAX_DETALLE) {
            return detalle;
        }
        return detalle.substring(0, MAX_DETALLE);
    }
}
