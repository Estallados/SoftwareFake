package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.Pago;
import co.edu.unbosque.marketplace.service.PagoService.ResultadoPago;

/**
 * Consumo de la API de Stripe en modo prueba: crea y confirma PaymentIntents
 * y hace devoluciones (Refunds). Interpreta las respuestas de Stripe y las
 * convierte en un {@link ResultadoPago}. La lógica de reservas y pagos está en
 * {@link PagoService}.
 *
 * @see "RF-002, RF-028, CA-003, CA-161, CA-162, CA-163"
 */
@Service
public class StripeService {

    /** Proveedor que queda registrado en los pagos hechos en Stripe. */
    public static final String PROVEEDOR = "STRIPE_TEST";
    /** Moneda de los cobros; Stripe la maneja con dos decimales (centavos). */
    public static final String MONEDA = "cop";
    /** Código de Stripe para un error al procesar la tarjeta. */
    public static final String PROCESSING_ERROR = "processing_error";
    /** Código de Stripe para una tarjeta que exige 3D Secure. */
    public static final String AUTENTICACION = "authentication_required";

    private static final Logger LOG = LoggerFactory.getLogger(StripeService.class);
    private static final String URL_STRIPE = "https://api.stripe.com/v1/";

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

    private final String claveSecreta;
    private final ClienteHttp clienteHttp;

    @Autowired
    public StripeService(@Value("${app.stripe.secret-key:${STRIPE_SECRET_KEY:}}") String claveSecreta,
            ClienteHttp clienteHttp) {
        this.claveSecreta = claveSecreta == null ? "" : claveSecreta.trim();
        this.clienteHttp = clienteHttp;
    }

    /** Indica si se configuró alguna clave secreta de Stripe. */
    public boolean estaConfigurado() {
        return !claveSecreta.isEmpty();
    }

    /** Indica si la clave es de prueba ({@code sk_test_...}); las demás se rechazan. */
    public boolean esClaveDePrueba() {
        return claveSecreta.startsWith("sk_test_");
    }

    /** Mensaje en español para un código de rechazo de Stripe. */
    public static String mensajeRechazo(String codigo) {
        return MENSAJES_RECHAZO.getOrDefault(codigo, "La tarjeta fue rechazada.");
    }

    /** Crea y confirma el PaymentIntent de la reserva. */
    public ResultadoPago cobrar(BigDecimal valor, String metodoPago, Long idReserva) {
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
            return enviar("payment_intents", "reserva-" + idReserva, formulario, this::resultadoCobro);
        } catch (RuntimeException e) {
            LOG.error("No se pudo comunicar con Stripe", e);
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR,
                    "No se pudo comunicar con la pasarela de pago.");
        }
    }

    /**
     * Devuelve el valor indicado del pago con un Refund. Si Stripe no hace la
     * devolución, el resultado queda APROBADO con el motivo en el detalle.
     */
    public ResultadoPago devolver(Pago pago, BigDecimal valor) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("payment_intent", pago.getReferencia());
        formulario.add("amount", centavos(valor));
        formulario.add("metadata[id_reserva]", String.valueOf(pago.getIdReserva()));
        try {
            return enviar("refunds", "reembolso-reserva-" + pago.getIdReserva(), formulario,
                    (estado, cuerpo) -> {
                        String status = cuerpo == null ? "" : cuerpo.path("status").asText();
                        if (estado.is2xxSuccessful() && ("succeeded".equals(status) || "pending".equals(status))) {
                            return new ResultadoPago(EstadoPago.REEMBOLSADO, cuerpo.path("id").asText(),
                                    PROVEEDOR, "Devolución de " + valor + " hecha en Stripe.");
                        }
                        LOG.error("Stripe rechazó la devolución de la reserva {}: {}", pago.getIdReserva(), cuerpo);
                        return new ResultadoPago(EstadoPago.APROBADO, null, PROVEEDOR,
                                "La devolución de " + valor + " no se pudo hacer en Stripe: " + mensajeError(cuerpo));
                    });
        } catch (RuntimeException e) {
            LOG.error("No se pudo comunicar con Stripe para la devolución de la reserva {}", pago.getIdReserva(), e);
            return new ResultadoPago(EstadoPago.APROBADO, null, PROVEEDOR,
                    "La devolución de " + valor + " está pendiente: no se pudo comunicar con Stripe.");
        }
    }

    /** Interpreta la respuesta de Stripe al crear y confirmar el PaymentIntent. */
    private ResultadoPago resultadoCobro(HttpStatusCode estado, JsonNode cuerpo) {
        if (cuerpo == null) {
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR, "Respuesta vacía de Stripe.");
        }
        if (estado.is2xxSuccessful()) {
            String status = cuerpo.path("status").asText();
            String id = cuerpo.path("id").asText(null);
            if ("succeeded".equals(status)) {
                return new ResultadoPago(EstadoPago.APROBADO, id, PROVEEDOR, "Pago de prueba aprobado por Stripe.");
            }
            if ("requires_action".equals(status)) {
                return new ResultadoPago(EstadoPago.RECHAZADO, id, PROVEEDOR, mensajeRechazo(AUTENTICACION));
            }
            return new ResultadoPago(EstadoPago.ERROR, id, PROVEEDOR, "Stripe dejó el pago en estado " + status + ".");
        }
        JsonNode error = cuerpo.path("error");
        String codigo = error.path("decline_code").asText(error.path("code").asText(""));
        String referencia = error.path("payment_intent").path("id").asText(null);
        if (estado.value() == 402) {
            return new ResultadoPago(PROCESSING_ERROR.equals(codigo) ? EstadoPago.ERROR : EstadoPago.RECHAZADO,
                    referencia, PROVEEDOR, mensajeRechazo(codigo));
        }
        if (estado.value() == 400 && "invalid_request_error".equals(error.path("type").asText())) {
            // Por ejemplo, un PaymentMethod que no existe: es un problema del dato enviado, no de la pasarela
            return new ResultadoPago(EstadoPago.RECHAZADO, referencia, PROVEEDOR, MENSAJES_RECHAZO
                    .getOrDefault(codigo, "Stripe no aceptó el método de pago: " + mensajeError(cuerpo)));
        }
        LOG.error("Respuesta inesperada de Stripe ({}): {}", estado.value(), cuerpo);
        return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR,
                estado.value() == 401 ? "Stripe rechazó la clave configurada."
                        : "La pasarela de pago no respondió correctamente.");
    }

    private ResultadoPago enviar(String recurso, String claveIdempotencia, MultiValueMap<String, String> formulario,
            BiFunction<HttpStatusCode, JsonNode, ResultadoPago> interprete) {
        Map<String, String> encabezados = Map.of(
                "Authorization", "Bearer " + claveSecreta,
                // Si la petición se repite (reintento, doble clic), Stripe devuelve la primera respuesta
                "Idempotency-Key", "marketplace-" + claveIdempotencia);
        return clienteHttp.postFormulario(URL_STRIPE + recurso, encabezados, formulario, interprete);
    }

    private static String mensajeError(JsonNode cuerpo) {
        return cuerpo == null ? "sin respuesta" : cuerpo.path("error").path("message").asText("error desconocido");
    }

    /** Stripe recibe el valor en la unidad mínima de la moneda: centavos de peso. */
    private static String centavos(BigDecimal valor) {
        return valor.movePointRight(2).toBigInteger().toString();
    }
}
