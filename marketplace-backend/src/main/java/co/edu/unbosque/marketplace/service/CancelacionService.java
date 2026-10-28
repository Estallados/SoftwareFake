package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.CancelacionDTO;
import co.edu.unbosque.marketplace.dto.SimulacionCancelacionDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.Cancelacion;
import co.edu.unbosque.marketplace.entity.EstadoCancelacion;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.PoliticaCancelacion;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.CancelacionRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Cancelación de reservas por el huésped. Aplica la política de cancelación del
 * alojamiento según los días que faltan para la llegada:
 * <ul>
 * <li>FLEXIBLE: 100 % con 1 o más días de anticipación.</li>
 * <li>MODERADA: 100 % con 5 o más días; 50 % con 1 a 4 días.</li>
 * <li>ESTRICTA: 50 % con 7 o más días; 0 % después.</li>
 * </ul>
 * La devolución es simulada. Al cancelar, las fechas se liberan solas porque la
 * disponibilidad se deriva de las reservas activas.
 *
 * @see "RF-002, CA-001 a CA-009"
 */
@Service
public class CancelacionService {

	/** Política que se usa si el alojamiento no tiene una asignada. */
	public static final String POLITICA_POR_DEFECTO = "FLEXIBLE";

	private static final int CIEN = 100;

	private final ReservaRepository reservaRepo;
	private final AlojamientoRepository alojamientoRepo;
	private final PoliticaCancelacionRepository politicaRepo;
	private final CancelacionRepository cancelacionRepo;
	private final PagoService pagoService;
	private final NotificacionService notificacionService;
	private final AuditoriaService auditoriaService;
	private final ModelMapper modelMapper;

	public CancelacionService(ReservaRepository reservaRepo, AlojamientoRepository alojamientoRepo,
			PoliticaCancelacionRepository politicaRepo, CancelacionRepository cancelacionRepo, PagoService pagoService,
			NotificacionService notificacionService, AuditoriaService auditoriaService, ModelMapper modelMapper) {
		this.reservaRepo = reservaRepo;
		this.alojamientoRepo = alojamientoRepo;
		this.politicaRepo = politicaRepo;
		this.cancelacionRepo = cancelacionRepo;
		this.pagoService = pagoService;
		this.notificacionService = notificacionService;
		this.auditoriaService = auditoriaService;
		this.modelMapper = modelMapper;
	}

	/**
	 * Porcentaje a devolver según la política y los días de anticipación.
	 *
	 * @see "CA-002, CA-003"
	 */
	public static int porcentajeReembolso(PoliticaCancelacion politica, long diasAnticipacion) {
		if (politica.getDiasReembolsoTotal() != null && diasAnticipacion >= politica.getDiasReembolsoTotal()) {
			return CIEN;
		}
		if (politica.getDiasReembolsoParcial() != null && diasAnticipacion >= politica.getDiasReembolsoParcial()
				&& politica.getPorcentajeParcial() != null) {
			return politica.getPorcentajeParcial();
		}
		return 0;
	}

	/**
	 * Cancela una reserva futura del huésped autenticado.
	 *
	 * @param idReserva Reserva a cancelar.
	 * @param motivo    Motivo obligatorio.
	 * @param idActor   Huésped autenticado.
	 * @return 0 con la cancelación; 1 la reserva no existe o es de otro usuario; 2
	 *         falta el motivo; 4 la reserva ya fue cancelada o no es futura.
	 */
	@Transactional
	public Resultado<CancelacionDTO> cancelar(Long idReserva, String motivo, Long idActor) {
		if (motivo == null || motivo.isBlank()) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El motivo de la cancelación es obligatorio.");
		}
		Optional<Reserva> reservaOpt = reservaRepo.findById(idReserva).filter(r -> r.getIdHuesped().equals(idActor));
		if (reservaOpt.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "La reserva no existe.");
		}
		Reserva reserva = reservaOpt.get();
		String noCancelable = motivoNoCancelable(reserva);
		if (noCancelable != null) {
			return rechazar(idActor, idReserva, noCancelable);
		}

		Optional<Alojamiento> alojamiento = alojamientoRepo.findById(reserva.getIdAlojamiento());
		PoliticaCancelacion politica = politicaDe(alojamiento);
		long dias = diasAnticipacion(reserva);
		int porcentaje = porcentajeReembolso(politica, dias);
		BigDecimal devolucion = devolucion(reserva, porcentaje);

		reserva.setEstado(EstadoReserva.CANCELADA);
		reservaRepo.save(reserva);

		Cancelacion cancelacion = new Cancelacion();
		cancelacion.setIdReserva(idReserva);
		cancelacion.setIdUsuario(idActor);
		cancelacion.setFecha(LocalDateTime.now());
		cancelacion.setMotivo(motivo.trim());
		cancelacion.setPolitica(politica.getNombre());
		cancelacion.setPorcentajeReembolso(porcentaje);
		cancelacion.setValorReembolso(devolucion);
		cancelacion.setEstado(EstadoCancelacion.COMPLETADA);
		cancelacion.setEstadoFinalReserva(EstadoReserva.CANCELADA);
		cancelacion = cancelacionRepo.save(cancelacion);

		pagoService.reembolsar(idReserva, devolucion);
		notificacionService.enviar(alojamiento.map(Alojamiento::getIdAnfitrion).orElse(null),
				NotificacionService.RESERVA_CANCELADA, "La reserva " + idReserva + " del " + reserva.getFechaEntrada()
						+ " al " + reserva.getFechaSalida() + " fue cancelada por el huésped.");
		notificacionService.enviar(idActor, NotificacionService.RESERVA_CANCELADA, "Cancelaste la reserva " + idReserva
				+ ". Devolución (" + politica.getNombre() + ", " + porcentaje + " %): " + devolucion + ".");
		auditoriaService.registrar(idActor, AuditoriaService.CANCELACION, AuditoriaService.ENTIDAD_RESERVA, idReserva,
				ResultadoAuditoria.EXITOSO, politica.getNombre() + " " + porcentaje + " %, devolución " + devolucion);
		return Resultado.exito(modelMapper.map(cancelacion, CancelacionDTO.class));
	}

	/**
	 * Devolución que recibiría el huésped si cancelara hoy, con la misma regla de
	 * {@link #cancelar(Long, String, Long)}. No cambia la reserva ni queda en la
	 * auditoría.
	 *
	 * @return 0 con la estimación; 1 la reserva no existe o es de otro usuario; 4
	 *         la reserva no se puede cancelar.
	 * @see "CA-002, CA-003"
	 */
	public Resultado<SimulacionCancelacionDTO> simular(Long idReserva, Long idActor) {
		Optional<Reserva> reservaOpt = reservaRepo.findById(idReserva).filter(r -> r.getIdHuesped().equals(idActor));
		if (reservaOpt.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "La reserva no existe.");
		}
		Reserva reserva = reservaOpt.get();
		String noCancelable = motivoNoCancelable(reserva);
		if (noCancelable != null) {
			return Resultado.error(Resultado.ESTADO_INVALIDO, noCancelable);
		}

		PoliticaCancelacion politica = politicaDe(alojamientoRepo.findById(reserva.getIdAlojamiento()));
		long dias = diasAnticipacion(reserva);
		int porcentaje = porcentajeReembolso(politica, dias);

		SimulacionCancelacionDTO simulacion = new SimulacionCancelacionDTO();
		simulacion.setIdReserva(idReserva);
		simulacion.setPolitica(politica.getNombre());
		simulacion.setDiasAnticipacion(dias);
		simulacion.setPorcentajeReembolso(porcentaje);
		simulacion.setValorReembolso(devolucion(reserva, porcentaje));
		simulacion.setTotalPagado(reserva.getTotal());
		return Resultado.exito(simulacion);
	}

	/** Causa por la que la reserva no se puede cancelar, o null si se puede. */
	private static String motivoNoCancelable(Reserva reserva) {
		if (reserva.getEstado() != EstadoReserva.CONFIRMADA && reserva.getEstado() != EstadoReserva.EN_REVISION) {
			return "La reserva no se puede cancelar en estado " + reserva.getEstado() + ".";
		}
		if (!reserva.getFechaEntrada().isAfter(LocalDate.now())) {
			return "Solo se pueden cancelar reservas futuras.";
		}
		return null;
	}

	/**
	 * Política del alojamiento, o la política por defecto si no tiene una asignada.
	 */
	private PoliticaCancelacion politicaDe(Optional<Alojamiento> alojamiento) {
		return alojamiento.map(Alojamiento::getIdPolitica).flatMap(politicaRepo::findById)
				.or(() -> politicaRepo.findByNombre(POLITICA_POR_DEFECTO)).orElseThrow();
	}

	private static long diasAnticipacion(Reserva reserva) {
		return ChronoUnit.DAYS.between(LocalDate.now(), reserva.getFechaEntrada());
	}

	private static BigDecimal devolucion(Reserva reserva, int porcentaje) {
		return reserva.getTotal().multiply(BigDecimal.valueOf(porcentaje)).divide(BigDecimal.valueOf(CIEN), 2,
				RoundingMode.HALF_UP);
	}

	private Resultado<CancelacionDTO> rechazar(Long idActor, Long idReserva, String mensaje) {
		auditoriaService.registrar(idActor, AuditoriaService.CANCELACION, AuditoriaService.ENTIDAD_RESERVA, idReserva,
				ResultadoAuditoria.RECHAZADO, mensaje);
		return Resultado.error(Resultado.ESTADO_INVALIDO, mensaje);
	}
}
