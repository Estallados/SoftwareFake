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
import co.edu.unbosque.marketplace.util.LimitesTexto;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Cancelación de reservas por parte del huésped. Aplica la política de
 * cancelación del alojamiento según los días que faltan para la llegada:
 * FLEXIBLE devuelve el 100 % con 1 o más días de anticipación; MODERADA el 100
 * % con 5 o más días y el 50 % con 1 a 4 días; ESTRICTA el 50 % con 7 o más
 * días y nada después.
 * <p>
 * La devolución se hace con la misma pasarela de prueba del pago. Al cancelar,
 * las fechas se liberan solas, porque la disponibilidad se calcula a partir de
 * las reservas activas.
 * </p>
 */
@Service
public class CancelacionService {

	/** Política que se usa si el alojamiento no tiene una asignada. */
	public static final String POLITICA_POR_DEFECTO = "FLEXIBLE";

	/** Cien, para calcular porcentajes. */
	private static final int CIEN = 100;

	/** Repositorio de reservas. */
	private final ReservaRepository reservaRepo;

	/** Repositorio de alojamientos. */
	private final AlojamientoRepository alojamientoRepo;

	/** Repositorio de políticas de cancelación. */
	private final PoliticaCancelacionRepository politicaRepo;

	/** Repositorio de cancelaciones. */
	private final CancelacionRepository cancelacionRepo;

	/** Hace la devolución del pago. */
	private final PagoService pagoService;

	/** Avisa al huésped y al anfitrión. */
	private final NotificacionService notificacionService;

	/** Servicio de auditoría. */
	private final AuditoriaService auditoriaService;

	/** Copia los datos de la entidad al DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param reservaRepo repositorio de reservas
	 * @param alojamientoRepo repositorio de alojamientos
	 * @param politicaRepo repositorio de políticas
	 * @param cancelacionRepo repositorio de cancelaciones
	 * @param pagoService servicio de pagos
	 * @param notificacionService servicio de notificaciones
	 * @param auditoriaService servicio de auditoría
	 * @param modelMapper copiador de entidades a DTO
	 */
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
	 * @param politica política del alojamiento
	 * @param diasAnticipacion días que faltan para la llegada
	 * @return porcentaje a devolver (0 a 100)
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
	 * Cancela una reserva futura del huésped autenticado: calcula la
	 * devolución, la hace, guarda la cancelación y avisa al huésped y al
	 * anfitrión.
	 *
	 * @param idReserva reserva a cancelar
	 * @param motivo motivo de la cancelación (obligatorio)
	 * @param idActor huésped autenticado
	 * @return la cancelación; NO_ENCONTRADO si la reserva no existe o es de
	 *         otro usuario; DATOS_INVALIDOS si falta el motivo; ESTADO_INVALIDO
	 *         si ya fue cancelada o ya empezó
	 */
	@Transactional
	public Resultado<CancelacionDTO> cancelar(Long idReserva, String motivo, Long idActor) {
		if (motivo == null || motivo.isBlank()) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "El motivo de la cancelación es obligatorio.");
		}
		if (LimitesTexto.excede(motivo, LimitesTexto.MOTIVO)) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, LimitesTexto.MOTIVO_LARGO);
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
	 * Devolución que recibiría el huésped si cancelara hoy, con la misma regla
	 * de {@link #cancelar(Long, String, Long)}. No cambia la reserva ni queda
	 * en la auditoría.
	 *
	 * @param idReserva reserva a consultar
	 * @param idActor huésped autenticado
	 * @return la estimación; NO_ENCONTRADO si la reserva no existe o es de otro
	 *         usuario; ESTADO_INVALIDO si no se puede cancelar
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

	/**
	 * Causa por la que la reserva no se puede cancelar.
	 *
	 * @param reserva reserva a revisar
	 * @return la causa, o {@code null} si se puede cancelar
	 */
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
	 * Política del alojamiento, o la política por defecto si no tiene una
	 * asignada.
	 *
	 * @param alojamiento alojamiento de la reserva
	 * @return política a aplicar
	 */
	private PoliticaCancelacion politicaDe(Optional<Alojamiento> alojamiento) {
		return alojamiento.map(Alojamiento::getIdPolitica).flatMap(politicaRepo::findById)
				.or(() -> politicaRepo.findByNombre(POLITICA_POR_DEFECTO)).orElseThrow();
	}

	/**
	 * Días que faltan desde hoy hasta la llegada.
	 *
	 * @param reserva reserva
	 * @return cantidad de días (negativa si la llegada ya pasó)
	 */
	private static long diasAnticipacion(Reserva reserva) {
		return ChronoUnit.DAYS.between(LocalDate.now(), reserva.getFechaEntrada());
	}

	/**
	 * Valor a devolver: el total por el porcentaje, redondeado a 2 decimales.
	 *
	 * @param reserva reserva
	 * @param porcentaje porcentaje a devolver
	 * @return valor de la devolución
	 */
	private static BigDecimal devolucion(Reserva reserva, int porcentaje) {
		return reserva.getTotal().multiply(BigDecimal.valueOf(porcentaje)).divide(BigDecimal.valueOf(CIEN), 2,
				RoundingMode.HALF_UP);
	}

	/**
	 * Registra en la auditoría una cancelación rechazada y arma el error.
	 *
	 * @param idActor usuario que intentó cancelar
	 * @param idReserva reserva
	 * @param mensaje causa del rechazo
	 * @return resultado con ESTADO_INVALIDO
	 */
	private Resultado<CancelacionDTO> rechazar(Long idActor, Long idReserva, String mensaje) {
		auditoriaService.registrar(idActor, AuditoriaService.CANCELACION, AuditoriaService.ENTIDAD_RESERVA, idReserva,
				ResultadoAuditoria.RECHAZADO, mensaje);
		return Resultado.error(Resultado.ESTADO_INVALIDO, mensaje);
	}
}
