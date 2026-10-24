package co.edu.unbosque.marketplace.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.ConsultaDisponibilidadDTO;
import co.edu.unbosque.marketplace.dto.DisponibilidadDTO;
import co.edu.unbosque.marketplace.dto.PeriodoDisponibilidadDTO;
import co.edu.unbosque.marketplace.dto.RangoReservadoDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.PeriodoDisponibilidad;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Calendario de disponibilidad. El anfitrión registra los períodos en que
 * ofrece su alojamiento; las fechas reservadas no se guardan aparte: se
 * derivan de las reservas activas, así el bloqueo es automático (RF-020).
 * <p>
 * Una estancia [llegada, salida) está libre si la publicación está ACTIVA, un
 * período ofrecido la contiene y ninguna reserva activa se cruza con ella.
 * Las reservas PENDIENTE_PAGO ocupan las fechas solo durante
 * {@value #MINUTOS_PENDIENTE} minutos.
 * </p>
 *
 * @see "RF-020, RF-025, RF-026, RNF-027"
 */
@Service
public class CalendarioService {

    /** Estados de reserva que ocupan fechas. */
    public static final List<EstadoReserva> ESTADOS_OCUPAN = List.of(EstadoReserva.CONFIRMADA,
            EstadoReserva.EN_REVISION);

    /** Minutos que una reserva sin pagar retiene las fechas. */
    public static final int MINUTOS_PENDIENTE = 15;

    private static final String NO_EXISTE = "El alojamiento no existe.";

    private final AlojamientoRepository alojamientoRepo;
    private final PeriodoDisponibilidadRepository periodoRepo;
    private final ReservaRepository reservaRepo;
    private final AuditoriaService auditoriaService;
    private final ModelMapper modelMapper;

    public CalendarioService(AlojamientoRepository alojamientoRepo, PeriodoDisponibilidadRepository periodoRepo,
            ReservaRepository reservaRepo, AuditoriaService auditoriaService, ModelMapper modelMapper) {
        this.alojamientoRepo = alojamientoRepo;
        this.periodoRepo = periodoRepo;
        this.reservaRepo = reservaRepo;
        this.auditoriaService = auditoriaService;
        this.modelMapper = modelMapper;
    }

    /** Las reservas PENDIENTE_PAGO creadas antes de este instante ya no ocupan fechas. */
    public static LocalDateTime limitePendiente() {
        return LocalDateTime.now().minusMinutes(MINUTOS_PENDIENTE);
    }

    /**
     * Valida un rango de estancia: ambas fechas presentes, llegada no pasada y
     * salida posterior a la llegada.
     *
     * @return el mensaje de error, o null si las fechas son válidas.
     * @see "RNF-027, CA-082, CA-121, CA-122"
     */
    public static String validarFechas(LocalDate llegada, LocalDate salida) {
        if (llegada == null || salida == null) {
            return "Debes indicar la fecha de llegada y la de salida.";
        }
        if (!salida.isAfter(llegada)) {
            return "Las fechas son inválidas: la salida debe ser posterior a la llegada.";
        }
        if (llegada.isBefore(LocalDate.now())) {
            return "Las fechas son inválidas: la llegada no puede estar en el pasado.";
        }
        return null;
    }

    /** Indica si [llegada, salida) está dentro de un período ofrecido y sin reservas que se crucen. */
    public boolean estaLibre(Long idAlojamiento, LocalDate llegada, LocalDate salida) {
        return periodoRepo.contiene(idAlojamiento, llegada, salida)
                && !reservaRepo.existeCruce(idAlojamiento, llegada, salida, ESTADOS_OCUPAN, limitePendiente());
    }

    /**
     * Consulta si un alojamiento está disponible en unas fechas.
     *
     * @return 0 con la respuesta (disponible o no, con el motivo); 1 si no
     *         existe; 2 si las fechas no son válidas.
     * @see "RF-026, CA-119, CA-120, CA-121, CA-122, CA-159"
     */
    public Resultado<ConsultaDisponibilidadDTO> consultar(Long idAlojamiento, LocalDate llegada, LocalDate salida) {
        String errorFechas = validarFechas(llegada, salida);
        if (errorFechas != null) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, errorFechas);
        }
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }

        ConsultaDisponibilidadDTO respuesta = new ConsultaDisponibilidadDTO();
        respuesta.setIdAlojamiento(idAlojamiento);
        respuesta.setFechaEntrada(llegada);
        respuesta.setFechaSalida(salida);
        if (alojamiento.get().getEstado() != EstadoAlojamiento.ACTIVA) {
            respuesta.setDisponible(false);
            respuesta.setMotivo("El alojamiento no está disponible para nuevas reservas (estado "
                    + alojamiento.get().getEstado() + ").");
        } else if (!periodoRepo.contiene(idAlojamiento, llegada, salida)) {
            respuesta.setDisponible(false);
            respuesta.setMotivo("El anfitrión no ofrece el alojamiento en esas fechas.");
        } else if (reservaRepo.existeCruce(idAlojamiento, llegada, salida, ESTADOS_OCUPAN, limitePendiente())) {
            respuesta.setDisponible(false);
            respuesta.setMotivo("Las fechas se cruzan con una reserva existente.");
        } else {
            respuesta.setDisponible(true);
        }
        return Resultado.exito(respuesta);
    }

    /**
     * Calendario del alojamiento: períodos ofrecidos y rangos reservados. Es
     * público si está ACTIVA; en otro estado solo lo ven su anfitrión o un
     * administrador.
     *
     * @see "RF-026, CA-076, CA-148"
     */
    public Resultado<DisponibilidadDTO> calendario(Long idAlojamiento, Long idActor, boolean esAdmin) {
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty() || !(alojamiento.get().getEstado() == EstadoAlojamiento.ACTIVA || esAdmin
                || alojamiento.get().getIdAnfitrion().equals(idActor))) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        DisponibilidadDTO dto = new DisponibilidadDTO();
        dto.setIdAlojamiento(idAlojamiento);
        dto.setEstadoAlojamiento(alojamiento.get().getEstado());
        dto.setPeriodos(periodoRepo.findByIdAlojamientoOrderByFechaInicio(idAlojamiento).stream()
                .map(p -> modelMapper.map(p, PeriodoDisponibilidadDTO.class)).toList());
        dto.setReservados(reservaRepo.buscarOcupadas(idAlojamiento, LocalDate.now(), ESTADOS_OCUPAN,
                limitePendiente()).stream().map(CalendarioService::rango).toList());
        return Resultado.exito(dto);
    }

    /**
     * Registra un período disponible (anfitrión propietario o administrador).
     * No puede cruzarse con otro período del mismo alojamiento.
     *
     * @return 0 con el período; 1 no existe; 2 fechas inválidas; 3 sin permiso;
     *         4 se cruza con otro período.
     * @see "RF-025, CA-115, CA-116"
     */
    @Transactional
    public Resultado<PeriodoDisponibilidadDTO> agregarPeriodo(Long idAlojamiento, PeriodoDisponibilidadDTO datos,
            Long idActor, boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        String error = validarPeriodo(datos);
        if (error != null) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, error);
        }
        if (periodoRepo.existeCruce(idAlojamiento, datos.getFechaInicio(), datos.getFechaFin(), null)) {
            return Resultado.error(Resultado.ESTADO_INVALIDO,
                    "El período se cruza con otro período ya registrado; modifícalo en lugar de crear uno nuevo.");
        }
        PeriodoDisponibilidad periodo = new PeriodoDisponibilidad();
        periodo.setIdAlojamiento(idAlojamiento);
        periodo.setFechaInicio(datos.getFechaInicio());
        periodo.setFechaFin(datos.getFechaFin());
        periodo = periodoRepo.save(periodo);
        auditar(idActor, periodo.getIdPeriodo(), ResultadoAuditoria.EXITOSO,
                "Período agregado " + periodo.getFechaInicio() + " a " + periodo.getFechaFin());
        return Resultado.exito(modelMapper.map(periodo, PeriodoDisponibilidadDTO.class));
    }

    /**
     * Modifica un período. Se rechaza si deja por fuera una reserva activa que
     * estaba dentro del período.
     *
     * @return 0 con el período; 1 no existe; 2 fechas inválidas; 3 sin permiso;
     *         4 afecta una reserva o se cruza con otro período.
     * @see "RF-025, CA-117, CA-118"
     */
    @Transactional
    public Resultado<PeriodoDisponibilidadDTO> actualizarPeriodo(Long idAlojamiento, Long idPeriodo,
            PeriodoDisponibilidadDTO datos, Long idActor, boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        Optional<PeriodoDisponibilidad> periodo = periodoRepo.findById(idPeriodo)
                .filter(p -> p.getIdAlojamiento().equals(idAlojamiento));
        if (periodo.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El período no existe.");
        }
        String error = validarPeriodo(datos);
        if (error != null) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, error);
        }
        if (periodoRepo.existeCruce(idAlojamiento, datos.getFechaInicio(), datos.getFechaFin(), idPeriodo)) {
            return Resultado.error(Resultado.ESTADO_INVALIDO, "El período se cruza con otro período ya registrado.");
        }
        for (Reserva reserva : reservasDentro(periodo.get())) {
            if (reserva.getFechaEntrada().isBefore(datos.getFechaInicio())
                    || reserva.getFechaSalida().isAfter(datos.getFechaFin())) {
                auditar(idActor, idPeriodo, ResultadoAuditoria.RECHAZADO,
                        "El cambio deja por fuera la reserva " + reserva.getIdReserva());
                return Resultado.error(Resultado.ESTADO_INVALIDO,
                        "No puedes modificar el período: afecta la reserva del " + reserva.getFechaEntrada()
                                + " al " + reserva.getFechaSalida() + ".");
            }
        }
        periodo.get().setFechaInicio(datos.getFechaInicio());
        periodo.get().setFechaFin(datos.getFechaFin());
        PeriodoDisponibilidad guardado = periodoRepo.save(periodo.get());
        auditar(idActor, idPeriodo, ResultadoAuditoria.EXITOSO,
                "Período modificado a " + guardado.getFechaInicio() + " - " + guardado.getFechaFin());
        return Resultado.exito(modelMapper.map(guardado, PeriodoDisponibilidadDTO.class));
    }

    /**
     * Elimina un período sin reservas activas dentro.
     *
     * @return 0 eliminado; 1 no existe; 3 sin permiso; 4 tiene reservas.
     * @see "RF-025, CA-118"
     */
    @Transactional
    public Resultado<Void> eliminarPeriodo(Long idAlojamiento, Long idPeriodo, Long idActor, boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        Optional<PeriodoDisponibilidad> periodo = periodoRepo.findById(idPeriodo)
                .filter(p -> p.getIdAlojamiento().equals(idAlojamiento));
        if (periodo.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El período no existe.");
        }
        if (!reservasDentro(periodo.get()).isEmpty()) {
            auditar(idActor, idPeriodo, ResultadoAuditoria.RECHAZADO, "Intento de eliminar un período con reservas");
            return Resultado.error(Resultado.ESTADO_INVALIDO,
                    "No puedes eliminar el período: tiene reservas activas.");
        }
        periodoRepo.delete(periodo.get());
        auditar(idActor, idPeriodo, ResultadoAuditoria.EXITOSO, "Período eliminado");
        return Resultado.exito(null);
    }

    private List<Reserva> reservasDentro(PeriodoDisponibilidad periodo) {
        return reservaRepo.buscarOcupadas(periodo.getIdAlojamiento(), periodo.getFechaInicio(), ESTADOS_OCUPAN,
                limitePendiente()).stream()
                .filter(r -> r.getFechaEntrada().isBefore(periodo.getFechaFin())
                        && r.getFechaSalida().isAfter(periodo.getFechaInicio()))
                .toList();
    }

    private Resultado<Alojamiento> cargarModificable(Long idAlojamiento, Long idActor, boolean esAdmin) {
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        if (!esAdmin && !alojamiento.get().getIdAnfitrion().equals(idActor)) {
            auditar(idActor, null, ResultadoAuditoria.RECHAZADO,
                    "Sin permiso sobre la disponibilidad del alojamiento " + idAlojamiento);
            return Resultado.error(Resultado.SIN_PERMISO,
                    "Solo el anfitrión propietario o un administrador puede modificar la disponibilidad.");
        }
        return Resultado.exito(alojamiento.get());
    }

    private static String validarPeriodo(PeriodoDisponibilidadDTO datos) {
        if (datos == null || datos.getFechaInicio() == null || datos.getFechaFin() == null) {
            return "Debes indicar la fecha de inicio y la de fin del período.";
        }
        if (!datos.getFechaFin().isAfter(datos.getFechaInicio())) {
            return "Las fechas son inválidas: el fin del período debe ser posterior al inicio.";
        }
        if (datos.getFechaFin().isBefore(LocalDate.now())) {
            return "El período no puede terminar en el pasado.";
        }
        return null;
    }

    private void auditar(Long idActor, Long idPeriodo, ResultadoAuditoria resultado, String detalle) {
        auditoriaService.registrar(idActor, AuditoriaService.DISPONIBILIDAD, AuditoriaService.ENTIDAD_PERIODO,
                idPeriodo, resultado, detalle);
    }

    private static RangoReservadoDTO rango(Reserva reserva) {
        RangoReservadoDTO rango = new RangoReservadoDTO();
        rango.setFechaEntrada(reserva.getFechaEntrada());
        rango.setFechaSalida(reserva.getFechaSalida());
        return rango;
    }
}
