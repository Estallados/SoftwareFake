package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.dto.CotizacionDTO;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.Cotizacion;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.ServicioAdicional;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.CotizacionRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Cotización de una estancia. Calcula cada componente con los porcentajes
 * vigentes y guarda la cotización por {@value #MINUTOS_VIGENCIA} minutos; la
 * reserva se crea desde ella con el mismo monto.
 * <p>
 * Fórmula (redondeo a 2 decimales, mitad hacia arriba):
 * </p>
 * <ul>
 * <li>precio base = noches × precio por noche</li>
 * <li>descuento = 0 (no hay reglas de descuento definidas)</li>
 * <li>limpieza = (base − descuento) × % limpieza</li>
 * <li>servicio = (base − descuento) × % servicio</li>
 * <li>adicionales = suma del costo de cada servicio adicional elegido (por
 * estancia)</li>
 * <li>total = base − descuento + limpieza + servicio + adicionales</li>
 * </ul>
 *
 * @see "RF-021, RF-023, RNF-027, RNF-031, CA-080, CA-081, CA-179 a CA-184"
 */
@Service
public class CotizacionService {

	/** Minutos que una cotización sirve para reservar. */
	public static final int MINUTOS_VIGENCIA = 30;

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final AlojamientoRepository alojamientoRepo;
	private final ServicioAdicionalRepository servicioRepo;
	private final CotizacionRepository cotizacionRepo;
	private final CalendarioService calendarioService;
	private final TarifaService tarifaService;
	private final ModelMapper modelMapper;
	private final ObjectMapper objectMapper;

	public CotizacionService(AlojamientoRepository alojamientoRepo, ServicioAdicionalRepository servicioRepo,
			CotizacionRepository cotizacionRepo, CalendarioService calendarioService, TarifaService tarifaService,
			ModelMapper modelMapper, ObjectMapper objectMapper) {
		this.alojamientoRepo = alojamientoRepo;
		this.servicioRepo = servicioRepo;
		this.cotizacionRepo = cotizacionRepo;
		this.calendarioService = calendarioService;
		this.tarifaService = tarifaService;
		this.modelMapper = modelMapper;
		this.objectMapper = objectMapper;
	}

	/** Componentes del precio de una estancia. */
	public record Desglose(BigDecimal precioBase, BigDecimal descuento, BigDecimal valorLimpieza,
			BigDecimal valorServicio, BigDecimal valorAdicionales, BigDecimal total) {
	}

	/**
	 * Calcula el desglose de una estancia (función pura).
	 *
	 * @see "CA-081"
	 */
	public static Desglose calcular(BigDecimal precioNoche, long noches, BigDecimal porcentajeLimpieza,
			BigDecimal porcentajeServicio, List<BigDecimal> costosAdicionales) {
		BigDecimal base = redondear(precioNoche.multiply(BigDecimal.valueOf(noches)));
		BigDecimal descuento = redondear(BigDecimal.ZERO);
		BigDecimal neto = base.subtract(descuento);
		BigDecimal limpieza = redondear(neto.multiply(porcentajeLimpieza).divide(CIEN));
		BigDecimal servicio = redondear(neto.multiply(porcentajeServicio).divide(CIEN));
		BigDecimal adicionales = redondear(costosAdicionales.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
		BigDecimal total = neto.add(limpieza).add(servicio).add(adicionales);
		return new Desglose(base, descuento, limpieza, servicio, adicionales, redondear(total));
	}

	/**
	 * Cotiza una estancia y guarda la cotización.
	 *
	 * @return 0 con la cotización; 1 el alojamiento no existe; 2 fechas, huéspedes
	 *         o servicios inválidos; 4 el alojamiento no está ACTIVA; 5 las fechas
	 *         no están disponibles.
	 * @see "CA-082, CA-083, CA-179, CA-181, CA-182, CA-183, CA-184"
	 */
	@Transactional
	public Resultado<CotizacionDTO> cotizar(CotizacionDTO solicitud, Long idUsuario) {
		if (solicitud == null || solicitud.getIdAlojamiento() == null) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "Debes indicar el alojamiento a cotizar.");
		}
		Optional<Alojamiento> alojamientoOpt = alojamientoRepo.findById(solicitud.getIdAlojamiento());
		if (alojamientoOpt.isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe.");
		}
		Alojamiento alojamiento = alojamientoOpt.get();
		if (alojamiento.getEstado() != EstadoAlojamiento.ACTIVA) {
			return Resultado.error(Resultado.ESTADO_INVALIDO,
					"El alojamiento no está disponible para reservas (estado " + alojamiento.getEstado() + ").");
		}
		String errorFechas = CalendarioService.validarFechas(solicitud.getFechaEntrada(), solicitud.getFechaSalida());
		if (errorFechas != null) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, errorFechas);
		}
		int huespedes = solicitud.getHuespedes() == null ? 1 : solicitud.getHuespedes();
		if (huespedes <= 0) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "La cantidad de huéspedes debe ser mayor que cero.");
		}
		if (huespedes > alojamiento.getCapacidad()) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "La cantidad de huéspedes (" + huespedes
					+ ") supera la capacidad del alojamiento (" + alojamiento.getCapacidad() + ").");
		}
		if (!calendarioService.estaLibre(alojamiento.getIdAlojamiento(), solicitud.getFechaEntrada(),
				solicitud.getFechaSalida())) {
			return Resultado.error(Resultado.NO_DISPONIBLE, "El alojamiento no está disponible en esas fechas.");
		}

		List<ServicioAdicional> disponibles = servicioRepo
				.findByIdAlojamientoOrderByIdServicio(alojamiento.getIdAlojamiento());
		List<ServicioAdicional> elegidos = new ArrayList<>();
		if (solicitud.getServiciosSeleccionados() != null) {
			for (Long idServicio : new LinkedHashSet<>(solicitud.getServiciosSeleccionados())) {
				Optional<ServicioAdicional> servicio = disponibles.stream()
						.filter(s -> s.getIdServicio().equals(idServicio)).findFirst();
				if (servicio.isEmpty()) {
					return Resultado.error(Resultado.DATOS_INVALIDOS,
							"El servicio adicional " + idServicio + " no pertenece a este alojamiento.");
				}
				elegidos.add(servicio.get());
			}
		}

		long noches = ChronoUnit.DAYS.between(solicitud.getFechaEntrada(), solicitud.getFechaSalida());
		BigDecimal porcentajeLimpieza = tarifaService.porcentajeLimpieza(alojamiento);
		BigDecimal porcentajeServicio = tarifaService.porcentajeServicio(alojamiento);
		Desglose desglose = calcular(alojamiento.getPrecioNoche(), noches, porcentajeLimpieza, porcentajeServicio,
				elegidos.stream().map(ServicioAdicional::getCosto).toList());

		Cotizacion cotizacion = new Cotizacion();
		cotizacion.setIdAlojamiento(alojamiento.getIdAlojamiento());
		cotizacion.setIdUsuario(idUsuario);
		cotizacion.setFechaEntrada(solicitud.getFechaEntrada());
		cotizacion.setFechaSalida(solicitud.getFechaSalida());
		cotizacion.setHuespedes(huespedes);
		cotizacion.setNoches((int) noches);
		cotizacion.setPrecioNoche(alojamiento.getPrecioNoche());
		cotizacion.setPrecioBase(desglose.precioBase());
		cotizacion.setDescuento(desglose.descuento());
		cotizacion.setPorcentajeLimpieza(porcentajeLimpieza);
		cotizacion.setValorLimpieza(desglose.valorLimpieza());
		cotizacion.setPorcentajeServicio(porcentajeServicio);
		cotizacion.setValorServicio(desglose.valorServicio());
		cotizacion.setValorAdicionales(desglose.valorAdicionales());
		cotizacion.setTotal(desglose.total());
		cotizacion.setDesglose(
				serializar(elegidos.stream().map(s -> modelMapper.map(s, ServicioAdicionalDTO.class)).toList()));
		cotizacion.setFechaCreacion(LocalDateTime.now());
		cotizacion.setFechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA));
		cotizacion = cotizacionRepo.save(cotizacion);

		CotizacionDTO dto = convertirDTO(cotizacion);
		dto.setServiciosDisponibles(
				disponibles.stream().map(s -> modelMapper.map(s, ServicioAdicionalDTO.class)).toList());
		return Resultado.exito(dto);
	}

	/**
	 * Convierte una cotización guardada a DTO, con los servicios adicionales
	 * congelados.
	 */
	public CotizacionDTO convertirDTO(Cotizacion cotizacion) {
		CotizacionDTO dto = modelMapper.map(cotizacion, CotizacionDTO.class);
		List<ServicioAdicionalDTO> elegidos = leerServicios(cotizacion.getDesglose());
		dto.setServiciosAdicionales(elegidos);
		dto.setServiciosSeleccionados(elegidos.stream().map(ServicioAdicionalDTO::getIdServicio).toList());
		return dto;
	}

	private String serializar(List<ServicioAdicionalDTO> servicios) {
		try {
			return objectMapper.writeValueAsString(servicios);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("No se pudo guardar el desglose de la cotización", e);
		}
	}

	private List<ServicioAdicionalDTO> leerServicios(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<ServicioAdicionalDTO>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("No se pudo leer el desglose de la cotización", e);
		}
	}

	private static BigDecimal redondear(BigDecimal valor) {
		return valor.setScale(2, RoundingMode.HALF_UP);
	}
}
