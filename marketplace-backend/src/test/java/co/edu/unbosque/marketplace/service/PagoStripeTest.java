package co.edu.unbosque.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import co.edu.unbosque.marketplace.dto.ConfiguracionPagoDTO;
import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.Pago;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.repository.PagoRepository;

/**
 * Pagos en modo STRIPE_TEST. Un servidor falso responde como la API de
 * Stripe, con el mismo formato de sus respuestas de éxito y de error.
 */
class PagoStripeTest {

	private static final String PAYMENT_INTENTS = "https://api.stripe.com/v1/payment_intents";
	private static final String REFUNDS = "https://api.stripe.com/v1/refunds";

	private MockRestServiceServer stripe;
	private PagoRepository pagoRepo;
	private PagoService pagos;

	@BeforeEach
	void preparar() {
		RestClient.Builder builder = RestClient.builder();
		stripe = MockRestServiceServer.bindTo(builder).build();
		pagoRepo = mock(PagoRepository.class);
		when(pagoRepo.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
		pagos = new PagoService(pagoRepo, "sk_test_123", "pk_test_456", builder.build());
	}

	private static Reserva reserva(long id, String total) {
		Reserva reserva = new Reserva();
		reserva.setIdReserva(id);
		reserva.setTotal(new BigDecimal(total));
		return reserva;
	}

	private void responderError(int estado, String error) {
		stripe.expect(requestTo(PAYMENT_INTENTS))
				.andRespond(withStatus(HttpStatus.valueOf(estado)).contentType(MediaType.APPLICATION_JSON).body(error));
	}

	@Test
	void creaYConfirmaElPaymentIntentEnCentavosConClaveDeIdempotencia_CA161() {
		stripe.expect(requestTo(PAYMENT_INTENTS)).andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer sk_test_123"))
				.andExpect(header("Idempotency-Key", "marketplace-reserva-7"))
				.andExpect(content().formDataContains(java.util.Map.of("amount", "35000000", "currency", "cop",
						"payment_method", "pm_card_visa", "confirm", "true", "metadata[id_reserva]", "7")))
				.andRespond(withSuccess("{\"id\":\"pi_3Abc\",\"status\":\"succeeded\"}", MediaType.APPLICATION_JSON));

		Pago pago = pagos.procesar(reserva(7, "350000.00"), null);

		stripe.verify();
		assertEquals(EstadoPago.APROBADO, pago.getEstado());
		assertEquals("pi_3Abc", pago.getReferencia());
		assertEquals(PagoService.PROVEEDOR_STRIPE, pago.getProveedor());
		assertEquals("pm_card_visa", pago.getMetodo());
	}

	@Test
	void unRechazoDeLaTarjetaLlegaConSuMotivoEnEspanol_CA162() {
		responderError(402, "{\"error\":{\"type\":\"card_error\",\"code\":\"card_declined\","
				+ "\"decline_code\":\"insufficient_funds\",\"message\":\"Your card has insufficient funds.\","
				+ "\"payment_intent\":{\"id\":\"pi_rechazado\",\"status\":\"requires_payment_method\"}}}");

		Pago pago = pagos.procesar(reserva(8, "100000"), "pm_card_chargeDeclinedInsufficientFunds");

		assertEquals(EstadoPago.RECHAZADO, pago.getEstado());
		assertEquals("pi_rechazado", pago.getReferencia());
		assertEquals("La tarjeta no tiene fondos suficientes.", pago.getDetalle());
	}

	@Test
	void tresDSecureYUnMetodoInexistenteSeRechazanYUnaFallaDeStripeEsError_CA163() {
		stripe.expect(requestTo(PAYMENT_INTENTS)).andRespond(
				withSuccess("{\"id\":\"pi_3ds\",\"status\":\"requires_action\"}", MediaType.APPLICATION_JSON));
		assertTrue(pagos.cobrar(BigDecimal.TEN, "pm_card_authenticationRequired", 1L).detalle().contains("3D Secure"));

		stripe.reset();
		responderError(400, "{\"error\":{\"type\":\"invalid_request_error\",\"code\":\"resource_missing\","
				+ "\"message\":\"No such PaymentMethod: 'pm_inventado'\"}}");
		PagoService.ResultadoPago inexistente = pagos.cobrar(BigDecimal.TEN, "pm_inventado", 2L);
		assertEquals(EstadoPago.RECHAZADO, inexistente.estado());
		assertEquals("El método de pago no existe en Stripe.", inexistente.detalle());

		stripe.reset();
		responderError(401, "{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Invalid API Key\"}}");
		assertEquals(EstadoPago.ERROR, pagos.cobrar(BigDecimal.TEN, "pm_card_visa", 3L).estado());

		stripe.reset();
		responderError(500, "{\"error\":{\"type\":\"api_error\"}}");
		assertEquals(EstadoPago.ERROR, pagos.cobrar(BigDecimal.TEN, "pm_card_visa", 4L).estado());
	}

	@Test
	void alCancelarDevuelveEnStripeElValorDeLaPolitica_CA003() {
		Pago aprobado = new Pago();
		aprobado.setIdReserva(9L);
		aprobado.setEstado(EstadoPago.APROBADO);
		aprobado.setProveedor(PagoService.PROVEEDOR_STRIPE);
		aprobado.setReferencia("pi_pagado");
		aprobado.setFecha(LocalDateTime.now());
		when(pagoRepo.findTopByIdReservaOrderByFechaDescIdPagoDesc(9L)).thenReturn(Optional.of(aprobado));
		stripe.expect(requestTo(REFUNDS)).andExpect(header("Idempotency-Key", "marketplace-reembolso-reserva-9"))
				.andExpect(content().formDataContains(java.util.Map.of("payment_intent", "pi_pagado",
						"amount", "5000000")))
				.andRespond(withSuccess("{\"id\":\"re_1\",\"status\":\"succeeded\"}", MediaType.APPLICATION_JSON));

		pagos.reembolsar(9L, new BigDecimal("50000"));

		stripe.verify();
		assertEquals(EstadoPago.REEMBOLSADO, aprobado.getEstado());
		assertEquals("re_1", aprobado.getReferenciaReembolso());
		assertEquals(0, new BigDecimal("50000").compareTo(aprobado.getValorReembolsado()));
	}

	@Test
	void siStripeNoDevuelveElPagoQuedaAprobadoConElMotivo() {
		Pago aprobado = new Pago();
		aprobado.setIdReserva(10L);
		aprobado.setEstado(EstadoPago.APROBADO);
		aprobado.setProveedor(PagoService.PROVEEDOR_STRIPE);
		aprobado.setReferencia("pi_pagado");
		when(pagoRepo.findTopByIdReservaOrderByFechaDescIdPagoDesc(10L)).thenReturn(Optional.of(aprobado));
		stripe.expect(requestTo(REFUNDS)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Charge already refunded\"}}"));

		pagos.reembolsar(10L, BigDecimal.TEN);

		assertEquals(EstadoPago.APROBADO, aprobado.getEstado());
		assertTrue(aprobado.getDetalle().contains("Charge already refunded"));
		assertNull(aprobado.getReferenciaReembolso());
	}

	@Test
	void laConfiguracionIndicaElModoYSoloEntregaClavesDePrueba() {
		ConfiguracionPagoDTO stripeTest = pagos.configuracion();
		assertEquals("STRIPE_TEST", stripeTest.getModo());
		assertEquals("pk_test_456", stripeTest.getClavePublica());
		assertEquals("cop", stripeTest.getMoneda());
		assertTrue(stripeTest.getMetodosPrueba().stream().anyMatch(m -> m.getMetodo().equals("pm_card_visa")));

		ConfiguracionPagoDTO simulado = new PagoService(pagoRepo, "", "pk_test_456").configuracion();
		assertEquals("SIMULADO", simulado.getModo());
		assertNull(simulado.getClavePublica());
		assertNull(new PagoService(pagoRepo, "sk_test_1", "pk_live_real").configuracion().getClavePublica());
	}

	@Test
	void elSimuladorRespondeComoStripeASusTarjetasDePrueba() {
		PagoService simulador = new PagoService(pagoRepo, "", "");
		assertEquals("La tarjeta está vencida.",
				simulador.cobrar(BigDecimal.TEN, "pm_card_chargeDeclinedExpiredCard", 1L).detalle());
		assertEquals(EstadoPago.ERROR,
				simulador.cobrar(BigDecimal.TEN, "pm_card_chargeDeclinedProcessingError", 1L).estado());
		// Un PaymentMethod creado por Stripe Elements con una tarjeta válida
		assertEquals(EstadoPago.APROBADO, simulador.cobrar(BigDecimal.TEN, "pm_1PxYzAbC123", 1L).estado());
		assertTrue(PagoService.metodoValido(null));
		assertTrue(PagoService.metodoValido("pm_card_visa"));
		assertFalse(PagoService.metodoValido("4242424242424242"));
		assertFalse(PagoService.metodoValido("pm_<script>"));
	}
}
