package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.time.Duration;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.dto.ConfiguracionPagoDTO;
import co.edu.unbosque.marketplace.dto.MetodoPagoPruebaDTO;
import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.Pago;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.PagoRepository;

/**
 * Pagos simulados con Stripe. Nunca hace cobros reales.
 * <ul>
 * <li><b>Modo STRIPE_TEST</b>: con {@code STRIPE_SECRET_KEY=sk_test_...} cada
 * reserva crea y confirma un PaymentIntent en el modo de prueba de Stripe (se
 * ve en el dashboard de Stripe). Al cancelar, la devolución se hace también en
 * Stripe con un Refund por el valor que da la política.</li>
 * <li><b>Modo SIMULADO</b>: sin clave, un simulador local responde igual que
 * Stripe a sus métodos de pago de prueba (ver {@link #configuracion()}).</li>
 * <li>Una clave que no sea de prueba ({@code sk_live_...}) se rechaza siempre.</li>
 * </ul>
 * El front puede enviar un método de prueba ({@code pm_card_visa}) o el
 * PaymentMethod que crea Stripe Elements con una tarjeta de prueba
 * (4242 4242 4242 4242). Los cobros y las devoluciones llevan una clave de
 * idempotencia por reserva: si se reintenta, Stripe no cobra dos veces.
 *
 * @see "RF-002, RF-028, CA-003, CA-160, CA-161, CA-162, CA-163"
 */
@Service
public class PagoService {

    /** Método de pago que se usa si el huésped no envía ninguno. */
    public static final String METODO_POR_DEFECTO = "pm_card_visa";
    public static final String PROVEEDOR_STRIPE = "STRIPE_TEST";
    public static final String PROVEEDOR_SIMULADO = "SIMULADO";
    /** Moneda de los cobros; Stripe la maneja con dos decimales (centavos). */
    public static final String MONEDA = "cop";

    private static final Logger LOG = LoggerFactory.getLogger(PagoService.class);
    private static final String URL_STRIPE = "https://api.stripe.com/v1/";
    private static final Pattern PATRON_METODO = Pattern.compile("^pm_\\w{1,200}$");
    private static final String PROCESSING_ERROR = "processing_error";
    private static final String AUTENTICACION = "authentication_required";

    /** Métodos de prueba de Stripe y el resultado que dan (el simulador responde igual). */
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
            new Prueba("pm_card_authenticationRequired", "Exige 3D Secure", EstadoPago.RECHAZADO, AUTENTICACION),
            new Prueba("pm_card_chargeDeclinedProcessingError", "Error de procesamiento", EstadoPago.ERROR,
                    PROCESSING_ERROR),
            new Prueba("pm_card_error", "Falla de la pasarela (solo simulador)", EstadoPago.ERROR, null));

    /** Mensaje en español de cada código de rechazo de Stripe (decline_code o code). */
    private static final Map<String, String> MENSAJES_RECHAZO = Map.ofEntries(
            Map.entry("generic_decline", "La tarjeta fue rechazada por el banco."),
            Map.entry("card_declined", "La tarjeta fue rechazada por el banco."),
            Map.entry("insufficient_funds", "La tarjeta no tiene fondos suficientes."),
            Map.entry("lost_card", "La tarjeta fue reportada como perdida."),
            Map.entry("stolen_card", "La tarjeta fue reportada como robada."),
            Map.entry("expired_card", "La tarjeta está vencida."),
            Map.entry("incorrect_cvc", "El código de seguridad (CVC) no es correcto."),
            Map.entry("fraudulent", "El pago se bloqueó por sospecha de fraude."),
            Map.entry(PROCESSING_ERROR, "Ocurrió un error al procesar la tarjeta. Intenta de nuevo."),
            Map.entry(AUTENTICACION, "La tarjeta exige autenticación 3D Secure, que este pago no admite. "
                    + "Usa otra tarjeta."),
            Map.entry("resource_missing", "El método de pago no existe en Stripe."));

    private final PagoRepository pagoRepo;
    private final String claveStripe;
    private final String clavePublica;
    private final RestClient restClient;

    @Autowired
    public PagoService(PagoRepository pagoRepo,
            @Value("${app.stripe.secret-key:${STRIPE_SECRET_KEY:}}") String claveStripe,
            @Value("${app.stripe.publishable-key:${STRIPE_PUBLISHABLE_KEY:}}") String clavePublica) {
        this(pagoRepo, claveStripe, clavePublica, RestClient.builder().requestFactory(fabricaConTiempos()).build());
    }

    /** Constructor con otro cliente HTTP; las pruebas lo usan para simular las respuestas de Stripe. */
    public PagoService(PagoRepository pagoRepo, String claveStripe, String clavePublica, RestClient restClient) {
        this.pagoRepo = pagoRepo;
        this.claveStripe = claveStripe == null ? "" : claveStripe.trim();
        this.clavePublica = clavePublica == null ? "" : clavePublica.trim();
        this.restClient = restClient;
        if (!this.claveStripe.isEmpty() && !esClaveDePrueba()) {
            LOG.error("STRIPE_SECRET_KEY no es una clave de prueba (sk_test_...): todos los pagos fallarán.");
        } else {
            LOG.info("Pagos en modo {}", modo());
        }
    }

    private static SimpleClientHttpRequestFactory fabricaConTiempos() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(15));
        return fabrica;
    }

    /** Resultado de cobrar o devolver en la pasarela. */
    public record ResultadoPago(EstadoPago estado, String referencia, String proveedor, String detalle) {
    }

    /** STRIPE_TEST si hay una clave de prueba de Stripe; si no, SIMULADO. */
    public String modo() {
        return esClaveDePrueba() ? PROVEEDOR_STRIPE : PROVEEDOR_SIMULADO;
    }

    /**
     * Configuración para el front: modo, clave publicable (solo en modo
     * STRIPE_TEST y si es pk_test_), moneda y métodos de prueba.
     */
    public ConfiguracionPagoDTO configuracion() {
        ConfiguracionPagoDTO dto = new ConfiguracionPagoDTO();
        dto.setModo(modo());
        dto.setClavePublica(esClaveDePrueba() && clavePublica.startsWith("pk_test_") ? clavePublica : null);
        dto.setMoneda(MONEDA);
        dto.setMetodosPrueba(PRUEBAS.stream().map(p -> new MetodoPagoPruebaDTO(p.metodo(), p.descripcion(),
                p.estado().name())).toList());
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
     * @param metodoPago PaymentMethod de Stripe; vacío usa {@link #METODO_POR_DEFECTO}.
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
        pago.setDetalle(resultado.detalle());
        pago.setFecha(LocalDateTime.now());
        return pagoRepo.save(pago);
    }

    /**
     * Devuelve al huésped el valor indicado del último pago aprobado de la
     * reserva: en Stripe con un Refund si el pago se hizo allí, o simulado. Si
     * Stripe rechaza la devolución, el pago queda APROBADO con el motivo en el
     * detalle para revisarlo.
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
        ResultadoPago resultado = enStripe ? devolverEnStripe(pago, valor)
                : new ResultadoPago(EstadoPago.REEMBOLSADO, "sim_re_" + UUID.randomUUID(), PROVEEDOR_SIMULADO,
                        "Devolución simulada de " + valor + ".");
        if (resultado.estado() == EstadoPago.REEMBOLSADO) {
            pago.setEstado(EstadoPago.REEMBOLSADO);
            pago.setValorReembolsado(valor);
            pago.setReferenciaReembolso(resultado.referencia());
        }
        pago.setDetalle(resultado.detalle());
        pagoRepo.save(pago);
    }

    /** Cobra en Stripe (modo prueba) o en el simulador. */
    public ResultadoPago cobrar(BigDecimal valor, String metodoPago, Long idReserva) {
        if (claveStripe.isEmpty()) {
            return simular(metodoPago);
        }
        if (!esClaveDePrueba()) {
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE,
                    "Solo se permiten claves de prueba de Stripe.");
        }
        return cobrarEnStripe(valor, metodoPago, idReserva);
    }

    private boolean esClaveDePrueba() {
        return claveStripe.startsWith("sk_test_");
    }

    /** Responde como Stripe a sus métodos de prueba; cualquier otro pm_ se trata como tarjeta válida. */
    private static ResultadoPago simular(String metodoPago) {
        Prueba prueba = PRUEBAS.stream().filter(p -> p.metodo().equalsIgnoreCase(metodoPago)).findFirst()
                .orElse(PRUEBAS.get(0));
        if (prueba.estado() == EstadoPago.APROBADO) {
            return new ResultadoPago(EstadoPago.APROBADO, "sim_pi_" + UUID.randomUUID(), PROVEEDOR_SIMULADO,
                    "Pago de prueba aprobado.");
        }
        String mensaje = prueba.codigoRechazo() == null ? "Error simulado de la pasarela de pago."
                : mensajeRechazo(prueba.codigoRechazo());
        return new ResultadoPago(prueba.estado(),
                prueba.estado() == EstadoPago.RECHAZADO ? "sim_pi_" + UUID.randomUUID() : null, PROVEEDOR_SIMULADO,
                mensaje);
    }

    private static String mensajeRechazo(String codigo) {
        return MENSAJES_RECHAZO.getOrDefault(codigo, "La tarjeta fue rechazada.");
    }

    private ResultadoPago cobrarEnStripe(BigDecimal valor, String metodoPago, Long idReserva) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("amount", centavos(valor));
        formulario.add("currency", MONEDA);
        formulario.add("payment_method", metodoPago);
        formulario.add("confirm", "true");
        formulario.add("description", "Reserva " + idReserva + " - Marketplace Inversiones LR");
        formulario.add("metadata[id_reserva]", String.valueOf(idReserva));
        // Solo tarjetas sin redirección: el pago se confirma en esta misma petición
        formulario.add("automatic_payment_methods[enabled]", "true");
        formulario.add("automatic_payment_methods[allow_redirects]", "never");
        try {
            return enviarAStripe("payment_intents", "reserva-" + idReserva, formulario, this::resultadoCobro);
        } catch (RuntimeException e) {
            LOG.error("No se pudo comunicar con Stripe", e);
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE,
                    "No se pudo comunicar con la pasarela de pago.");
        }
    }

    private ResultadoPago devolverEnStripe(Pago pago, BigDecimal valor) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("payment_intent", pago.getReferencia());
        formulario.add("amount", centavos(valor));
        formulario.add("metadata[id_reserva]", String.valueOf(pago.getIdReserva()));
        try {
            return enviarAStripe("refunds", "reembolso-reserva-" + pago.getIdReserva(), formulario,
                    (estado, cuerpo) -> {
                        String status = cuerpo == null ? "" : cuerpo.path("status").asText();
                        if (estado.is2xxSuccessful() && ("succeeded".equals(status) || "pending".equals(status))) {
                            return new ResultadoPago(EstadoPago.REEMBOLSADO, cuerpo.path("id").asText(),
                                    PROVEEDOR_STRIPE, "Devolución de " + valor + " hecha en Stripe.");
                        }
                        LOG.error("Stripe rechazó la devolución de la reserva {}: {}", pago.getIdReserva(), cuerpo);
                        return new ResultadoPago(EstadoPago.APROBADO, null, PROVEEDOR_STRIPE,
                                "La devolución de " + valor + " no se pudo hacer en Stripe: " + mensajeError(cuerpo));
                    });
        } catch (RuntimeException e) {
            LOG.error("No se pudo comunicar con Stripe para la devolución de la reserva {}", pago.getIdReserva(), e);
            return new ResultadoPago(EstadoPago.APROBADO, null, PROVEEDOR_STRIPE,
                    "La devolución de " + valor + " está pendiente: no se pudo comunicar con Stripe.");
        }
    }

    /** Interpreta la respuesta de Stripe al crear y confirmar el PaymentIntent. */
    private ResultadoPago resultadoCobro(HttpStatusCode estado, JsonNode cuerpo) {
        if (cuerpo == null) {
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE, "Respuesta vacía de Stripe.");
        }
        if (estado.is2xxSuccessful()) {
            String status = cuerpo.path("status").asText();
            String id = cuerpo.path("id").asText(null);
            if ("succeeded".equals(status)) {
                return new ResultadoPago(EstadoPago.APROBADO, id, PROVEEDOR_STRIPE,
                        "Pago de prueba aprobado por Stripe.");
            }
            if ("requires_action".equals(status)) {
                return new ResultadoPago(EstadoPago.RECHAZADO, id, PROVEEDOR_STRIPE, mensajeRechazo(AUTENTICACION));
            }
            return new ResultadoPago(EstadoPago.ERROR, id, PROVEEDOR_STRIPE,
                    "Stripe dejó el pago en estado " + status + ".");
        }
        JsonNode error = cuerpo.path("error");
        String codigo = error.path("decline_code").asText(error.path("code").asText(""));
        String referencia = error.path("payment_intent").path("id").asText(null);
        if (estado.value() == 402) {
            return new ResultadoPago(PROCESSING_ERROR.equals(codigo) ? EstadoPago.ERROR : EstadoPago.RECHAZADO,
                    referencia, PROVEEDOR_STRIPE, mensajeRechazo(codigo));
        }
        if (estado.value() == 400 && "invalid_request_error".equals(error.path("type").asText())) {
            // Por ejemplo, un PaymentMethod que no existe: es un problema del dato enviado, no de la pasarela
            return new ResultadoPago(EstadoPago.RECHAZADO, referencia, PROVEEDOR_STRIPE, MENSAJES_RECHAZO
                    .getOrDefault(codigo, "Stripe no aceptó el método de pago: " + mensajeError(cuerpo)));
        }
        LOG.error("Respuesta inesperada de Stripe ({}): {}", estado.value(), cuerpo);
        return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE,
                estado.value() == 401 ? "Stripe rechazó la clave configurada."
                        : "La pasarela de pago no respondió correctamente.");
    }

    private interface Interprete {
        ResultadoPago interpretar(HttpStatusCode estado, JsonNode cuerpo);
    }

    private ResultadoPago enviarAStripe(String recurso, String claveIdempotencia,
            MultiValueMap<String, String> formulario, Interprete interprete) {
        return restClient.post().uri(URL_STRIPE + recurso)
                .header("Authorization", "Bearer " + claveStripe)
                // Si la petición se repite (reintento, doble clic), Stripe devuelve la primera respuesta
                .header("Idempotency-Key", "marketplace-" + claveIdempotencia)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formulario)
                .exchange((peticion, respuesta) -> interprete.interpretar(respuesta.getStatusCode(),
                        respuesta.bodyTo(JsonNode.class)));
    }

    private static String mensajeError(JsonNode cuerpo) {
        return cuerpo == null ? "sin respuesta" : cuerpo.path("error").path("message").asText("error desconocido");
    }

    /** Stripe recibe el valor en la unidad mínima de la moneda: centavos de peso. */
    private static String centavos(BigDecimal valor) {
        return valor.movePointRight(2).toBigInteger().toString();
    }
}
