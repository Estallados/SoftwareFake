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
 * Pagos de las reservas, siempre de prueba: nunca se hacen cobros reales. Las
 * llamadas a la API de Stripe están en {@link StripeService}.
 * <p>
 * Modo STRIPE_TEST: con {@code STRIPE_SECRET_KEY=sk_test_...} cada reserva
 * crea y confirma un PaymentIntent en el modo de prueba de Stripe (se ve en el
 * dashboard de Stripe), y al cancelar la devolución también se hace en Stripe
 * con un Refund por el valor que da la política. Modo SIMULADO: sin clave, un
 * simulador local responde igual que Stripe a sus métodos de pago de prueba
 * (ver {@link #configuracion()}). Una clave que no sea de prueba
 * ({@code sk_live_...}) se rechaza siempre.
 * </p>
 * <p>
 * El frontend puede enviar un método de prueba ({@code pm_card_visa}) o el
 * PaymentMethod que crea Stripe Elements con una tarjeta de prueba (4242 4242
 * 4242 4242). Los cobros y las devoluciones llevan una clave de idempotencia
 * por reserva: si se reintenta, Stripe no cobra dos veces.
 * </p>
 */
@Service
public class PagoService {

	/** Método de pago que se usa si el huésped no envía ninguno. */
	public static final String METODO_POR_DEFECTO = "pm_card_visa";

	/** Proveedor que se registra en los pagos hechos en Stripe. */
	public static final String PROVEEDOR_STRIPE = StripeService.PROVEEDOR;

	/** Proveedor que se registra en los pagos del simulador. */
	public static final String PROVEEDOR_SIMULADO = "SIMULADO";
	/** Moneda de los cobros; Stripe la maneja con dos decimales (centavos). */
	public static final String MONEDA = StripeService.MONEDA;

	/** Log de la clase. */
	private static final Logger LOG = LoggerFactory.getLogger(PagoService.class);

	/** Forma válida de un PaymentMethod de Stripe ({@code pm_...}). */
	private static final Pattern PATRON_METODO = Pattern.compile("^pm_\\w{1,200}$");

	/**
	 * Método de pago de prueba de Stripe y el resultado que da.
	 *
	 * @param metodo id del método, por ejemplo {@code pm_card_visa}
	 * @param descripcion descripción para el usuario
	 * @param estado estado en que queda el pago
	 * @param codigoRechazo código de rechazo de Stripe; {@code null} si se
	 *                      aprueba
	 */
	private record Prueba(String metodo, String descripcion, EstadoPago estado, String codigoRechazo) {
	}

	/**
	 * Métodos de pago de prueba de Stripe con el resultado que da cada uno; el
	 * simulador responde igual.
	 */
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

	/** Repositorio de pagos. */
	private final PagoRepository pagoRepo;

	/** Cliente de la API de Stripe. */
	private final StripeService stripe;

	/** Clave publicable de Stripe para el frontend. */
	private final String clavePublica;

	/**
	 * Constructor que usa Spring.
	 *
	 * @param pagoRepo repositorio de pagos
	 * @param stripe cliente de Stripe
	 * @param clavePublica clave publicable de Stripe
	 *                     ({@code STRIPE_PUBLISHABLE_KEY})
	 */
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

	/**
	 * Constructor con la clave secreta de Stripe; lo usan las pruebas.
	 *
	 * @param pagoRepo repositorio de pagos
	 * @param claveStripe clave secreta de Stripe
	 * @param clavePublica clave publicable de Stripe
	 */
	public PagoService(PagoRepository pagoRepo, String claveStripe, String clavePublica) {
		this(pagoRepo, new StripeService(claveStripe, new ClienteHttp()), clavePublica);
	}

	/**
	 * Constructor con otro cliente HTTP; las pruebas lo usan para simular las
	 * respuestas de Stripe.
	 *
	 * @param pagoRepo repositorio de pagos
	 * @param claveStripe clave secreta de Stripe
	 * @param clavePublica clave publicable de Stripe
	 * @param restClient cliente HTTP que responde como Stripe
	 */
	public PagoService(PagoRepository pagoRepo, String claveStripe, String clavePublica, RestClient restClient) {
		this(pagoRepo, new StripeService(claveStripe, new ClienteHttp(restClient)), clavePublica);
	}

	/**
	 * Resultado de cobrar o devolver en la pasarela.
	 *
	 * @param estado estado del pago
	 * @param referencia referencia del proveedor
	 * @param proveedor proveedor que procesó la operación
	 * @param detalle mensaje de la pasarela, por ejemplo la causa de un rechazo
	 */
	public record ResultadoPago(EstadoPago estado, String referencia, String proveedor, String detalle) {
	}

	/**
	 * Modo de pago activo.
	 *
	 * @return STRIPE_TEST si hay una clave de prueba de Stripe; si no, SIMULADO
	 */
	public String modo() {
		return stripe.esClaveDePrueba() ? PROVEEDOR_STRIPE : PROVEEDOR_SIMULADO;
	}

	/**
	 * Configuración de pagos para el frontend: el modo, la clave publicable
	 * (solo en modo STRIPE_TEST y si es {@code pk_test_}), la moneda y los
	 * métodos de prueba.
	 *
	 * @return configuración de pagos
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
	 * ({@code pm_...}). Vacío también sirve: se usa
	 * {@link #METODO_POR_DEFECTO}.
	 *
	 * @param metodoPago método enviado por el frontend
	 * @return {@code true} si se puede usar
	 */
	public static boolean metodoValido(String metodoPago) {
		return metodoPago == null || metodoPago.isBlank() || PATRON_METODO.matcher(metodoPago.trim()).matches();
	}

	/**
	 * Cobra el total de la reserva y guarda el {@link Pago}.
	 *
	 * @param reserva reserva en PENDIENTE_PAGO
	 * @param metodoPago PaymentMethod de Stripe; vacío usa
	 *                   {@link #METODO_POR_DEFECTO}
	 * @return el pago guardado (APROBADO, RECHAZADO o ERROR)
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
	 * Devuelve al huésped el valor indicado del último pago aprobado de la
	 * reserva: en Stripe con un Refund si el pago se hizo allí, o simulado. Si
	 * Stripe rechaza la devolución, el pago queda APROBADO con el motivo en el
	 * detalle para revisarlo.
	 *
	 * @param idReserva reserva cancelada
	 * @param valor valor a devolver
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

	/**
	 * Cobra en Stripe (modo prueba) o en el simulador, según el modo activo.
	 *
	 * @param valor valor a cobrar
	 * @param metodoPago PaymentMethod de Stripe
	 * @param idReserva reserva; se usa como clave de idempotencia
	 * @return resultado del cobro
	 */
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
	 * Responde como Stripe a sus métodos de prueba; cualquier otro {@code pm_}
	 * se trata como una tarjeta válida.
	 *
	 * @param metodoPago método de pago
	 * @return resultado simulado
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
