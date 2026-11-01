package co.edu.unbosque.marketplace.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.dto.CancelacionDTO;
import co.edu.unbosque.marketplace.dto.PagoDTO;
import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.dto.ReservaAdminDTO;
import co.edu.unbosque.marketplace.dto.ReservaDTO;
import co.edu.unbosque.marketplace.dto.SolicitudReservaDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.Cotizacion;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.Pago;
import co.edu.unbosque.marketplace.entity.PoliticaCancelacion;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.CancelacionRepository;
import co.edu.unbosque.marketplace.repository.CotizacionRepository;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Reservas: creación con pago de prueba y consulta del historial.
 * <p>
 * Crear una reserva tiene tres pasos:
 * </p>
 * <ol>
 * <li>Transacción con bloqueo pesimista del alojamiento: valida la cotización
 * (vigente, del huésped, sin usar), que el alojamiento esté ACTIVA y no sea del
 * propio huésped, revalida la disponibilidad y crea la reserva en
 * PENDIENTE_PAGO con el monto exacto de la cotización.</li>
 * <li>Pago de prueba (fuera de la transacción, para no retener el bloqueo).</li>
 * <li>Si el pago se aprueba, la reserva queda CONFIRMADA de inmediato, sin
 * aprobación del anfitrión; si se rechaza o falla, queda CANCELADA y las fechas
 * se liberan.</li>
 * </ol>
 * La idempotencia (cabecera Idempotency-Key) la aplica el controller con
 * {@link IdempotenciaService}.
 *
 * @see "RF-020, RF-021, RF-022, RF-028, RNF-028, RNF-029, RNF-030, RNF-031"
 */
@Service
public class ReservaService {

    private static final Logger LOG = LoggerFactory.getLogger(ReservaService.class);
    private static final String NO_EXISTE = "La reserva no existe.";

    private final AlojamientoRepository alojamientoRepo;
    private final CotizacionRepository cotizacionRepo;
    private final ReservaRepository reservaRepo;
    private final PagoRepository pagoRepo;
    private final UsuarioRepository usuarioRepo;
    private final PoliticaCancelacionRepository politicaRepo;
    private final ResenaRepository resenaRepo;
    private final CancelacionRepository cancelacionRepo;
    private final CalendarioService calendarioService;
    private final PublicacionService publicacionService;
    private final CotizacionService cotizacionService;
    private final PagoService pagoService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transaccion;

    public ReservaService(AlojamientoRepository alojamientoRepo, CotizacionRepository cotizacionRepo,
            ReservaRepository reservaRepo, PagoRepository pagoRepo, UsuarioRepository usuarioRepo,
            PoliticaCancelacionRepository politicaRepo, ResenaRepository resenaRepo,
            CancelacionRepository cancelacionRepo, CalendarioService calendarioService,
            PublicacionService publicacionService, CotizacionService cotizacionService, PagoService pagoService,
            NotificacionService notificacionService, AuditoriaService auditoriaService, ModelMapper modelMapper,
            ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.alojamientoRepo = alojamientoRepo;
        this.cotizacionRepo = cotizacionRepo;
        this.reservaRepo = reservaRepo;
        this.pagoRepo = pagoRepo;
        this.usuarioRepo = usuarioRepo;
        this.politicaRepo = politicaRepo;
        this.resenaRepo = resenaRepo;
        this.cancelacionRepo = cancelacionRepo;
        this.calendarioService = calendarioService;
        this.publicacionService = publicacionService;
        this.cotizacionService = cotizacionService;
        this.pagoService = pagoService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    /**
     * Una reserva está completada si está CONFIRMADA y su fecha de salida ya
     * llegó.
     */
    public static boolean esCompletada(Reserva reserva) {
        return reserva.getEstado() == EstadoReserva.CONFIRMADA && !reserva.getFechaSalida().isAfter(LocalDate.now());
    }

    /**
     * Crea una reserva desde una cotización vigente y la paga.
     *
     * @return 0 con la reserva CONFIRMADA; 1 la cotización no existe; 2 datos
     *         inválidos; 3 es su propio alojamiento; 4 cotización vencida o
     *         alojamiento no ACTIVA; 5 fechas ya no disponibles; 6 pago
     *         rechazado; 7 error en el pago (en 6 y 7 se devuelve la reserva
     *         CANCELADA como dato).
     * @see "CA-070 a CA-075, CA-086, CA-109, CA-123, CA-159 a CA-163"
     */
    public Resultado<ReservaDTO> crear(SolicitudReservaDTO solicitud, Long idHuesped) {
        if (solicitud == null || solicitud.getIdCotizacion() == null) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "Debes indicar la cotización a reservar.");
        }
        if (!PagoService.metodoValido(solicitud.getMetodoPago())) {
            return Resultado.error(Resultado.DATOS_INVALIDOS,
                    "El método de pago debe ser un PaymentMethod de Stripe (pm_...), por ejemplo pm_card_visa.");
        }

        Resultado<Reserva> pendiente = transaccion.execute(estado -> reservarPendiente(solicitud.getIdCotizacion(),
                idHuesped));
        if (pendiente == null || !pendiente.esExito()) {
            String mensaje = pendiente == null ? "No se pudo crear la reserva." : pendiente.getMensaje();
            auditoriaService.registrar(idHuesped, AuditoriaService.RESERVA_CREADA, AuditoriaService.ENTIDAD_RESERVA,
                    null, ResultadoAuditoria.RECHAZADO, mensaje);
            return Resultado.error(pendiente == null ? Resultado.ESTADO_INVALIDO : pendiente.getCodigo(), mensaje);
        }

        Reserva reserva = pendiente.getDato();
        Pago pago = pagoService.procesar(reserva, solicitud.getMetodoPago());
        return transaccion.execute(estado -> cerrar(reserva.getIdReserva(), pago));
    }

    /**
     * Reservas del huésped autenticado con su estado actual.
     *
     * @see "RF-022, CA-078"
     */
    public PaginaDTO<ReservaDTO> listarMias(Long idHuesped, Pageable pagina) {
        return PaginaDTO.de(reservaRepo.findByIdHuesped(idHuesped, pagina), this::convertirDTO);
    }

    /**
     * Reservas de los alojamientos del anfitrión autenticado (nunca las de
     * otros).
     *
     * @see "RF-022, CA-079"
     */
    public PaginaDTO<ReservaDTO> listarDeAnfitrion(Long idAnfitrion, Pageable pagina) {
        return PaginaDTO.de(reservaRepo.buscarDeAnfitrion(idAnfitrion, pagina), this::convertirDTO);
    }

    /**
     * Todas las reservas de la plataforma, o solo las de un estado (vista del
     * administrador), en forma resumida: una sola consulta por página.
     *
     * @see "RF-005, RF-022"
     */
    public PaginaDTO<ReservaAdminDTO> listarTodas(EstadoReserva estado, Pageable pagina) {
        List<EstadoReserva> estados = estado == null ? List.of(EstadoReserva.values()) : List.of(estado);
        return PaginaDTO.de(reservaRepo.listarResumenAdmin(estados, pagina), Function.identity());
    }

    /**
     * Detalle de una reserva: la ven su huésped, el anfitrión del alojamiento y
     * los administradores.
     *
     * @return 0 con la reserva; 1 si no existe o es de otro usuario.
     * @see "RNF-006, CA-133"
     */
    public Resultado<ReservaDTO> obtener(Long idReserva, Long idActor, boolean esAdmin) {
        Optional<Reserva> reserva = reservaRepo.findById(idReserva);
        if (reserva.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        boolean esHuesped = reserva.get().getIdHuesped().equals(idActor);
        boolean esAnfitrion = alojamientoRepo.findById(reserva.get().getIdAlojamiento())
                .map(a -> a.getIdAnfitrion().equals(idActor)).orElse(false);
        if (!esAdmin && !esHuesped && !esAnfitrion) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        return Resultado.exito(convertirDTO(reserva.get()));
    }

    /**
     * Cancela las reservas que quedaron PENDIENTE_PAGO más del tiempo
     * permitido (por ejemplo, si el servidor se detuvo durante el pago).
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelarPendientesVencidas() {
        for (Reserva reserva : reservaRepo.findByEstadoAndFechaCreacionBefore(EstadoReserva.PENDIENTE_PAGO,
                CalendarioService.limitePendiente())) {
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaRepo.save(reserva);
            LOG.info("Reserva {} cancelada por no completar el pago a tiempo", reserva.getIdReserva());
        }
    }

    /**
     * Convierte la reserva a DTO con el último pago, los datos del alojamiento
     * (aunque esté pausado o bloqueado), los nombres del huésped y del
     * anfitrión, si ya tiene reseña y su cancelación, si la hubo.
     */
    public ReservaDTO convertirDTO(Reserva reserva) {
        ReservaDTO dto = modelMapper.map(reserva, ReservaDTO.class);
        dto.setCompletada(esCompletada(reserva));
        alojamientoRepo.findById(reserva.getIdAlojamiento()).ifPresent(a -> {
            dto.setTituloAlojamiento(a.getTitulo());
            dto.setCiudad(a.getCiudad());
            dto.setUbicacion(a.getUbicacion());
            dto.setTipoAlojamiento(a.getTipo());
            dto.setCapacidadAlojamiento(a.getCapacidad());
            dto.setIdAnfitrion(a.getIdAnfitrion());
            dto.setNombreAnfitrion(nombreUsuario(a.getIdAnfitrion()));
            publicacionService.imagenesDTO(a.getIdAlojamiento()).stream().findFirst()
                    .ifPresent(i -> dto.setImagenAlojamiento(i.getUrl()));
            String politica = a.getIdPolitica() == null ? null
                    : politicaRepo.findById(a.getIdPolitica()).map(PoliticaCancelacion::getNombre).orElse(null);
            dto.setNombrePolitica(politica == null ? CancelacionService.POLITICA_POR_DEFECTO : politica);
        });
        dto.setNombreHuesped(nombreUsuario(reserva.getIdHuesped()));
        dto.setTieneResena(resenaRepo.existsByIdReserva(reserva.getIdReserva()));
        cancelacionRepo.findByIdReserva(reserva.getIdReserva())
                .ifPresent(c -> dto.setCancelacion(modelMapper.map(c, CancelacionDTO.class)));
        pagoRepo.findTopByIdReservaOrderByFechaDescIdPagoDesc(reserva.getIdReserva())
                .ifPresent(p -> dto.setPago(modelMapper.map(p, PagoDTO.class)));
        return dto;
    }

    private String nombreUsuario(Long idUsuario) {
        return idUsuario == null ? null
                : usuarioRepo.findById(idUsuario).map(Usuario::getNombre).orElse(null);
    }

    /** Paso 1: validaciones con bloqueo pesimista y reserva PENDIENTE_PAGO. */
    private Resultado<Reserva> reservarPendiente(Long idCotizacion, Long idHuesped) {
        Optional<Cotizacion> cotizacionOpt = cotizacionRepo.findById(idCotizacion)
                .filter(c -> c.getIdUsuario().equals(idHuesped));
        if (cotizacionOpt.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "La cotización no existe.");
        }
        Cotizacion cotizacion = cotizacionOpt.get();
        Optional<Alojamiento> alojamientoOpt = alojamientoRepo.buscarParaReservar(cotizacion.getIdAlojamiento());
        if (alojamientoOpt.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe.");
        }
        Alojamiento alojamiento = alojamientoOpt.get();

        if (cotizacion.getIdReserva() != null || cotizacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return Resultado.error(Resultado.ESTADO_INVALIDO,
                    "La cotización venció o ya se usó; solicita una cotización nueva.");
        }
        if (alojamiento.getEstado() != EstadoAlojamiento.ACTIVA) {
            return Resultado.error(Resultado.ESTADO_INVALIDO,
                    "El alojamiento no está disponible para reservas (estado " + alojamiento.getEstado() + ").");
        }
        if (alojamiento.getIdAnfitrion().equals(idHuesped)) {
            return Resultado.error(Resultado.SIN_PERMISO, "No puedes reservar tu propio alojamiento.");
        }
        String errorFechas = CalendarioService.validarFechas(cotizacion.getFechaEntrada(),
                cotizacion.getFechaSalida());
        if (errorFechas != null) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, errorFechas);
        }
        // Revalidación inmediatamente antes de confirmar (RNF-029)
        if (!calendarioService.estaLibre(alojamiento.getIdAlojamiento(), cotizacion.getFechaEntrada(),
                cotizacion.getFechaSalida())) {
            return Resultado.error(Resultado.NO_DISPONIBLE,
                    "El período ya no está disponible: otra reserva lo tomó.");
        }

        Reserva reserva = new Reserva();
        reserva.setIdHuesped(idHuesped);
        reserva.setIdAlojamiento(alojamiento.getIdAlojamiento());
        reserva.setIdCotizacion(cotizacion.getIdCotizacion());
        reserva.setFechaEntrada(cotizacion.getFechaEntrada());
        reserva.setFechaSalida(cotizacion.getFechaSalida());
        reserva.setNoches(cotizacion.getNoches());
        reserva.setHuespedes(cotizacion.getHuespedes());
        reserva.setTotal(cotizacion.getTotal());
        reserva.setDesglose(desgloseCongelado(cotizacion));
        reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
        reserva.setFechaCreacion(LocalDateTime.now());
        reserva = reservaRepo.saveAndFlush(reserva);

        cotizacion.setIdReserva(reserva.getIdReserva());
        cotizacionRepo.save(cotizacion);
        return Resultado.exito(reserva);
    }

    /** Paso 3: confirma o cancela según el resultado del pago. */
    private Resultado<ReservaDTO> cerrar(Long idReserva, Pago pago) {
        Reserva reserva = reservaRepo.findById(idReserva).orElseThrow();
        Long idAnfitrion = alojamientoRepo.findById(reserva.getIdAlojamiento()).map(Alojamiento::getIdAnfitrion)
                .orElse(null);

        if (pago.getEstado() == EstadoPago.APROBADO) {
            if (reserva.getEstado() == EstadoReserva.PENDIENTE_PAGO) {
                reserva.setEstado(EstadoReserva.CONFIRMADA);
                reserva = reservaRepo.save(reserva);
            }
            notificacionService.enviar(reserva.getIdHuesped(), NotificacionService.RESERVA_CONFIRMADA,
                    "Tu reserva " + reserva.getIdReserva() + " del " + reserva.getFechaEntrada() + " al "
                            + reserva.getFechaSalida() + " está confirmada.");
            notificacionService.enviar(idAnfitrion, NotificacionService.RESERVA_CONFIRMADA,
                    "Tienes una reserva confirmada del " + reserva.getFechaEntrada() + " al "
                            + reserva.getFechaSalida() + ".");
            auditoriaService.registrar(reserva.getIdHuesped(), AuditoriaService.PAGO, AuditoriaService.ENTIDAD_RESERVA,
                    reserva.getIdReserva(), ResultadoAuditoria.EXITOSO, "Referencia " + pago.getReferencia());
            auditoriaService.registrar(reserva.getIdHuesped(), AuditoriaService.RESERVA_CREADA,
                    AuditoriaService.ENTIDAD_RESERVA, reserva.getIdReserva(), ResultadoAuditoria.EXITOSO,
                    "Total " + reserva.getTotal());
            return Resultado.exito(convertirDTO(reserva));
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva = reservaRepo.save(reserva);
        // La cotización vuelve a quedar libre para reintentar con otro método de pago
        cotizacionRepo.findById(reserva.getIdCotizacion()).ifPresent(c -> {
            c.setIdReserva(null);
            cotizacionRepo.save(c);
        });
        notificacionService.enviar(reserva.getIdHuesped(), NotificacionService.PAGO_NO_CONFIRMADO,
                "El pago de tu reserva no se confirmó y la reserva no quedó registrada como pagada.");
        boolean rechazado = pago.getEstado() == EstadoPago.RECHAZADO;
        auditoriaService.registrar(reserva.getIdHuesped(), AuditoriaService.PAGO, AuditoriaService.ENTIDAD_RESERVA,
                reserva.getIdReserva(), rechazado ? ResultadoAuditoria.RECHAZADO : ResultadoAuditoria.ERROR,
                pago.getDetalle());
        return Resultado.error(rechazado ? Resultado.PAGO_RECHAZADO : Resultado.PAGO_ERROR,
                (rechazado ? "El pago fue rechazado: " : "No se pudo confirmar el pago: ") + pago.getDetalle()
                        + " La reserva no quedó pagada.",
                convertirDTO(reserva));
    }

    private String desgloseCongelado(Cotizacion cotizacion) {
        try {
            return objectMapper.writeValueAsString(cotizacionService.convertirDTO(cotizacion));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo congelar el desglose de la reserva", e);
        }
    }
}
