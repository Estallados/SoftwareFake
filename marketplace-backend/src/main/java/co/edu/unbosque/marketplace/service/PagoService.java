package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.Pago;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.PagoRepository;

/**
 * Pago de prueba de las reservas. Nunca hace cobros reales.
 * <ul>
 * <li>Si la variable de entorno {@code STRIPE_SECRET_KEY} tiene una clave de
 * prueba ({@code sk_test_...}), crea y confirma un PaymentIntent en el modo de
 * prueba de Stripe.</li>
 * <li>Si no hay clave, usa un simulador local con los mismos métodos de pago de
 * prueba de Stripe: {@code pm_card_visa} se aprueba,
 * {@code pm_card_chargeDeclined} se rechaza y {@code pm_card_error} produce un
 * error.</li>
 * <li>Una clave que no sea de prueba se rechaza siempre.</li>
 * </ul>
 * La devolución por cancelación es simulada: solo cambia el estado del pago.
 *
 * @see "RF-028, CA-160, CA-161, CA-162, CA-163"
 */
@Service
public class PagoService {

    /** Método de pago que se usa si el huésped no envía ninguno. */
    public static final String METODO_POR_DEFECTO = "pm_card_visa";

    private static final Logger LOG = LoggerFactory.getLogger(PagoService.class);
    private static final String PROVEEDOR_STRIPE = "STRIPE_TEST";
    private static final String PROVEEDOR_SIMULADO = "SIMULADO";
    private static final String URL_STRIPE = "https://api.stripe.com/v1/payment_intents";

    private final PagoRepository pagoRepo;
    private final String claveStripe;
    private final RestClient restClient;

    public PagoService(PagoRepository pagoRepo, @Value("${app.stripe.secret-key:${STRIPE_SECRET_KEY:}}") String claveStripe) {
        this.pagoRepo = pagoRepo;
        this.claveStripe = claveStripe == null ? "" : claveStripe.trim();
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
    }

    /** Resultado de cobrar en la pasarela. */
    public record ResultadoPago(EstadoPago estado, String referencia, String proveedor, String detalle) {
    }

    /**
     * Cobra el total de la reserva en modo de prueba y guarda el {@link Pago}.
     *
     * @param reserva    Reserva PENDIENTE_PAGO.
     * @param metodoPago Método de pago de prueba de Stripe.
     * @return El pago guardado (APROBADO, RECHAZADO o ERROR).
     */
    @Transactional
    public Pago procesar(Reserva reserva, String metodoPago) {
        String metodo = metodoPago == null || metodoPago.isBlank() ? METODO_POR_DEFECTO : metodoPago.trim();
        ResultadoPago resultado = cobrar(reserva.getTotal(), metodo, "Reserva " + reserva.getIdReserva());

        Pago pago = new Pago();
        pago.setIdReserva(reserva.getIdReserva());
        pago.setValor(reserva.getTotal());
        pago.setEstado(resultado.estado());
        pago.setReferencia(resultado.referencia());
        pago.setProveedor(resultado.proveedor());
        pago.setDetalle(resultado.detalle());
        pago.setFecha(LocalDateTime.now());
        return pagoRepo.save(pago);
    }

    /**
     * Devolución simulada: marca el último pago aprobado como REEMBOLSADO si
     * corresponde devolver algo.
     *
     * @see "RF-002, CA-003"
     */
    @Transactional
    public void reembolsar(Long idReserva, BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            return;
        }
        pagoRepo.findTopByIdReservaOrderByFechaDescIdPagoDesc(idReserva)
                .filter(p -> p.getEstado() == EstadoPago.APROBADO).ifPresent(p -> {
                    p.setEstado(EstadoPago.REEMBOLSADO);
                    p.setDetalle("Devolución simulada de " + valor);
                    pagoRepo.save(p);
                });
    }

    /** Cobra en Stripe (modo prueba) o en el simulador. */
    public ResultadoPago cobrar(BigDecimal valor, String metodoPago, String descripcion) {
        if (claveStripe.isEmpty()) {
            return simular(metodoPago);
        }
        if (!claveStripe.startsWith("sk_test_")) {
            LOG.error("STRIPE_SECRET_KEY no es una clave de prueba: no se procesa ningún cobro");
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE,
                    "Solo se permiten claves de prueba de Stripe.");
        }
        return cobrarEnStripe(valor, metodoPago, descripcion);
    }

    private static ResultadoPago simular(String metodoPago) {
        String referencia = "sim_" + UUID.randomUUID();
        String metodo = metodoPago.toLowerCase();
        if (metodo.contains("declined")) {
            return new ResultadoPago(EstadoPago.RECHAZADO, referencia, PROVEEDOR_SIMULADO,
                    "La tarjeta de prueba fue rechazada.");
        }
        if (metodo.contains("error")) {
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_SIMULADO,
                    "Error simulado de la pasarela de pago.");
        }
        return new ResultadoPago(EstadoPago.APROBADO, referencia, PROVEEDOR_SIMULADO, "Pago de prueba aprobado.");
    }

    private ResultadoPago cobrarEnStripe(BigDecimal valor, String metodoPago, String descripcion) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        // Stripe recibe el valor en la unidad mínima de la moneda (centavos)
        formulario.add("amount", valor.movePointRight(2).toBigInteger().toString());
        formulario.add("currency", "cop");
        formulario.add("payment_method", metodoPago);
        formulario.add("confirm", "true");
        formulario.add("description", descripcion);
        formulario.add("automatic_payment_methods[enabled]", "true");
        formulario.add("automatic_payment_methods[allow_redirects]", "never");
        try {
            return restClient.post().uri(URL_STRIPE).header("Authorization", "Bearer " + claveStripe)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formulario)
                    .exchange((peticion, respuesta) -> {
                        JsonNode cuerpo = respuesta.bodyTo(JsonNode.class);
                        if (respuesta.getStatusCode().is2xxSuccessful() && cuerpo != null
                                && "succeeded".equals(cuerpo.path("status").asText())) {
                            return new ResultadoPago(EstadoPago.APROBADO, cuerpo.path("id").asText(),
                                    PROVEEDOR_STRIPE, "Pago de prueba aprobado por Stripe.");
                        }
                        String mensaje = cuerpo == null ? "Respuesta vacía de Stripe"
                                : cuerpo.path("error").path("message").asText("Estado " + cuerpo.path("status").asText());
                        if (respuesta.getStatusCode().value() == 402) {
                            return new ResultadoPago(EstadoPago.RECHAZADO,
                                    cuerpo == null ? null : cuerpo.path("error").path("payment_intent").path("id").asText(null),
                                    PROVEEDOR_STRIPE, mensaje);
                        }
                        return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE, mensaje);
                    });
        } catch (RuntimeException e) {
            LOG.error("No se pudo comunicar con Stripe", e);
            return new ResultadoPago(EstadoPago.ERROR, null, PROVEEDOR_STRIPE,
                    "No se pudo comunicar con la pasarela de pago.");
        }
    }
}
