package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.dto.ConfiguracionPagoDTO;
import co.edu.unbosque.marketplace.dto.MetodoPagoPruebaDTO;
import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.Pago;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.util.LimitesTexto;

/**
 * Pagos simulados con Stripe. Nunca hace cobros reales. Las llamadas a la API
 * de Stripe están en {@link StripeService}.
 * <ul>
 * <li><b>Modo STRIPE_TEST</b>: con {@code STRIPE_SECRET_KEY=sk_test_...} cada
 * reserva crea y confirma un PaymentIntent en el modo de prueba de Stripe (se
 * ve en el dashboard de Stripe). Al cancelar, la devolución se hace también en
 * Stripe con un Refund por el valor que da la política.</li>
 * <li><b>Modo SIMULADO</b>: sin clave, un simulador local responde igual que
 * Stripe a sus métodos de pago de prueba (ver {@link #configuracion()}).</li>
 * <li>Una clave que no sea de prueba ({@code sk_live_...}) se rechaza
 * siempre.</li>
 * </ul>
 * El front puede enviar un método de prueba ({@code pm_card_visa}) o el
 * PaymentMethod que crea Stripe Elements con una tarjeta de prueba (4242 4242
 * 4242 4242). Los cobros y las devoluciones llevan una clave de idempotencia
 * por reserva: si se reintenta, Stripe no cobra dos veces.
 *
 * @see "RF-002, RF-028, CA-003, CA-160, CA-161, CA-162, CA-163"
 */
@Service
public class PagoService {

	/** Método de pago que se usa si el huésped no envía ninguno. */
	public static final String METODO_POR_DEFECTO = "pm_card_visa";
	public static final String PROVEEDOR_STRIPE = StripeService.PROVEEDOR;
	public static final String PROVEEDOR_SIMULADO = "SIMULADO";
	/** Moneda de los cobros; Stripe la maneja con dos decimales (centavos). */
	public static final String MONEDA = StripeService.MONEDA;

	private static final Logger LOG = LoggerFactory.getLogger(PagoService.class);
	private static final Pattern PATRON_METODO = Pattern.compile("^pm_\\w{1,200}$");

	/**
	 * Métodos de prueba de Stripe y el resultado que dan (el simulador responde
	 * igual).
	 */
	private record Prueba(String metodo, String descripcion, EstadoPago estado, String codigoRechazo) {
	}

	private static final List<Prueba> PRUEBAS = List.of(
			new Prueba(METODO_POR_DEFECTO, "Visa aprobada", EstadoPago.APROBADO, null),
			new Prueba("pm_card_mastercard", "Mastercard aprobada", EstadoPago.APROBADO, null),
			new Prueba("pm_card_amex", "American Express aprobada", EstadoPago.APROBADO, null),
			new Prueba("pm_card_visa_debit", "Visa débito aprobada", EstadoPago.APROBADO, null),
			new Prueba("pm_card_chargeDeclined", "Rechazo genérico", EstadoPago.RECHAZADO, "generic_decline"),
			new Prueba("pm_card_chargeDeclinedInsufficientFunds", "Fondos insuficientes", EstadoPago.RECHAZADO,
					"insufficient_funds"),
			new Prueba("pm_card_chargeDeclinedLostCard", "Tarjeta perdida", EstadoPago.RECHAZADO, "lost_card"),
			new Prueba("pm_card_chargeDeclinedStolenCard", "Tarjeta robada", EstadoPago.RECHAZADO, "stolen_card"),
			new Prueba("pm_card_chargeDeclinedExpiredCard", "Tarjeta vencida", EstadoPago.RECHAZADO, "expired_card"),
			new Prueba("pm_card_chargeDeclinedIncorrectCvc", "CVC incorrecto", EstadoPago.RECHAZADO, "incorrect_cvc"),
			new Prueba("pm_card_chargeDeclinedFraudulent", "Sospecha de fraude", EstadoPago.RECHAZADO, "fraudulent"),
			new Prueba("pm_card_authenticationRequired", "Exige 3D Secure", EstadoPago.RECHAZADO,
					StripeService.AUTENTICACION),
			new Prueba("pm_card_chargeDeclinedProcessingError", "Error de procesamiento", EstadoPago.ERROR,
					StripeService.PROCESSING_ERROR),
			new Prueba("pm_card_error", "Falla de la pasarela (solo simulador)", EstadoPago.ERROR, null));

	private final PagoRepository pagoRepo;
	private final StripeService stripe;
	private final String clavePublica;

	@Autowired
	public PagoService(PagoRepository pagoRepo, StripeService stripe,
			@Value("${app.stripe.publishable-key:${STRIPE_PUBLISHABLE_KEY:}}") String clavePublica) {
		this.pagoRepo = pagoRepo;
		this.stripe = stripe;
		this.clavePublica = clavePublica == null ? "" : clavePublica.trim();
		if (stripe.estaConfigurado() && !stripe.esClaveDePrueba()) {
			LOG.error("STRIPE_SECRET_KEY no es una clave de prueba (sk_test_...): todos los pagos fallarán.");
		} else {
			LOG.info("Pagos en modo {}", modo());
		}
	}

	/** Constructor con la clave secreta de Stripe; lo usan las pruebas. */
	public PagoService(PagoRepository pagoRepo, String claveStripe, String clavePublica) {
		this(pagoRepo, new StripeService(claveStripe, new ClienteHttp()), clavePublica);
	}

	/**
	 * Constructor con otro cliente HTTP; las pruebas lo usan para simular las
	 * respuestas de Stripe.
	 */
	public PagoService(PagoRepository pagoRepo, String claveStripe, String clavePublica, RestClient restClient) {
		this(pagoRepo, new StripeService(claveStripe, new ClienteHttp(restClient)), clavePublica);
	}

	/** Resultado de cobrar o devolver en la pasarela. */
	public record ResultadoPago(EstadoPago estado, String referencia, String proveedor, String detalle) {
	}

	/** STRIPE_TEST si hay una clave de prueba de Stripe; si no, SIMULADO. */
	public String modo() {
		return stripe.esClaveDePrueba() ? PROVEEDOR_STRIPE : PROVEEDOR_SIMULADO;
	}

	/**
	 * Configuración para el front: modo, clave publicable (solo en modo STRIPE_TEST
	 * y si es pk_test_), moneda y métodos de prueba.
	 */
	public ConfiguracionPagoDTO configuracion() {
		ConfiguracionPagoDTO dto = new ConfiguracionPagoDTO();
		dto.setModo(modo());
		dto.setClavePublica(stripe.esClaveDePrueba() && clavePublica.startsWith("pk_test_") ? clavePublica : null);
		dto.setMoneda(MONEDA);
		dto.setMetodosPrueba(PRUEBAS.stream()
				.map(p -> new MetodoPagoPruebaDTO(p.metodo(), p.descripcion(), p.estado().name())).toList());
		return dto;
	}

	/**
	 * Indica si el método de pago tiene la forma de un PaymentMethod de Stripe
	 * ({@code pm_...}). Vacío también sirve: se usa {@link #METODO_POR_DEFECTO}.
	 */
	public static boolean metodoValido(String metodoPago) {
		return metodoPago == null || metodoPago.isBlank() || PATRON_METODO.matcher(metodoPago.trim()).matches();
	}

	/**
	 * Cobra el total de la reserva y guarda el {@link Pago}.
	 *
	 * @param reserva    Reserva PENDIENTE_PAGO.
	 * @param metodoPago PaymentMethod de Stripe; vacío usa
	 *                   {@link #METODO_POR_DEFECTO}.
	 * @return El pago guardado (APROBADO, RECHAZADO o ERROR).
	 */
	@Transactional
	public Pago procesar(Reserva reserva, String metodoPago) {
		String metodo = metodoPago == null || metodoPago.isBlank() ? METODO_POR_DEFECTO : metodoPago.trim();
		ResultadoPago resultado = cobrar(reserva.getTotal(), metodo, reserva.getIdReserva());

		Pago pago = new Pago();
		pago.setIdReserva(reserva.getIdReserva());
		pago.setValor(reserva.getTotal());
		pago.setMetodo(metodo);
		pago.setEstado(resultado.estado());
		pago.setReferencia(resultado.referencia());
		pago.setProveedor(resultado.proveedor());
		pago.setDetalle(LimitesTexto.recortar(resultado.detalle(), LimitesTexto.CORTO));
		pago.setFecha(LocalDateTime.now());
		return pagoRepo.save(pago);
	}

	/**
	 * Devuelve al huésped el valor indicado del último pago aprobado de la reserva:
	 * en Stripe con un Refund si el pago se hizo allí, o simulado. Si Stripe
	 * rechaza la devolución, el pago queda APROBADO con el motivo en el detalle
	 * para revisarlo.
	 *
	 * @see "RF-002, CA-003"
	 */
	@Transactional
	public void reembolsar(Long idReserva, BigDecimal valor) {
		if (valor == null || valor.signum() <= 0) {
			return;
		}
		Optional<Pago> aprobado = pagoRepo.findTopByIdReservaOrderByFechaDescIdPagoDesc(idReserva)
				.filter(p -> p.getEstado() == EstadoPago.APROBADO);
		if (aprobado.isEmpty()) {
			return;
		}
		Pago pago = aprobado.get();
		boolean enStripe = PROVEEDOR_STRIPE.equals(pago.getProveedor()) && pago.getReferencia() != null
				&& pago.getReferencia().startsWith("pi_");
		ResultadoPago resultado = enStripe ? stripe.devolver(pago, valor)
				: new ResultadoPago(EstadoPago.REEMBOLSADO, "sim_re_" + UUID.randomUUID(), PROVEEDOR_SIMULADO,
						"Devolución simulada de " + valor + ".");
		if (resultado.estado() == EstadoPago.REEMBOLSADO) {
			pago.setEstado(EstadoPago.REEMBOLSADO);
			pago.setValorReembolsado(valor);
			pago.setReferenciaReembolso(resultado.referencia());
		}
		pago.setDetalle(LimitesTexto.recortar(resultado.detalle(), LimitesTexto.CORTO));
		pagoRepo.save(pago);
	}

	/** Cobra en Stripe (modo prueba) o en el simulador. */
	public ResultadoPago cobrar(BigDecimal valor, String metodoPago, Long idReserva) {
		if (!stripe.estaConfigurado()) {
			return simular(metodoPago);
		}
		if (!stripe.esClaveDePrueba()) {
			return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE,
					"Solo se permiten claves de prueba de Stripe.");
		}
		return stripe.cobrar(valor, metodoPago, idReserva);
	}

	/**
	 * Responde como Stripe a sus métodos de prueba; cualquier otro pm_ se trata
	 * como tarjeta válida.
	 */
	private static ResultadoPago simular(String metodoPago) {
		Prueba prueba = PRUEBAS.stream().filter(p -> p.metodo().equalsIgnoreCase(metodoPago)).findFirst()
				.orElse(PRUEBAS.get(0));
		if (prueba.estado() == EstadoPago.APROBADO) {
			return new ResultadoPago(EstadoPago.APROBADO, "sim_pi_" + UUID.randomUUID(), PROVEEDOR_SIMULADO,
					"Pago de prueba aprobado.");
		}
		String mensaje = prueba.codigoRechazo() == null ? "Error simulado de la pasarela de pago."
				: StripeService.mensajeRechazo(prueba.codigoRechazo());
		return new ResultadoPago(prueba.estado(),
				prueba.estado() == EstadoPago.RECHAZADO ? "sim_pi_" + UUID.randomUUID() : null, PROVEEDOR_SIMULADO,
				mensaje);
	}
}
