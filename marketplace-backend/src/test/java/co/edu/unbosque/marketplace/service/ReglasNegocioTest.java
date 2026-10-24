package co.edu.unbosque.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.PoliticaCancelacion;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.util.TotpUtil;

/** Pruebas unitarias de las reglas de negocio puras (sin base de datos). */
class ReglasNegocioTest {

	private static PoliticaCancelacion politica(Integer diasTotal, Integer diasParcial, Integer porcentaje) {
		PoliticaCancelacion politica = new PoliticaCancelacion();
		politica.setDiasReembolsoTotal(diasTotal);
		politica.setDiasReembolsoParcial(diasParcial);
		politica.setPorcentajeParcial(porcentaje);
		return politica;
	}

	// ----- cotización (RNF-031) -----

	@Test
	void elTotalEsLaSumaCorrectaDeLosComponentes_CA081() {
		CotizacionService.Desglose d = CotizacionService.calcular(new BigDecimal("150000"), 3, new BigDecimal("5"),
				new BigDecimal("10"), List.of(new BigDecimal("20000"), new BigDecimal("15000")));

		assertEquals(new BigDecimal("450000.00"), d.precioBase());
		assertEquals(new BigDecimal("0.00"), d.descuento());
		assertEquals(new BigDecimal("22500.00"), d.valorLimpieza());
		assertEquals(new BigDecimal("45000.00"), d.valorServicio());
		assertEquals(new BigDecimal("35000.00"), d.valorAdicionales());
		assertEquals(new BigDecimal("552500.00"), d.total());
	}

	@Test
	void losValoresSeRedondeanADosDecimales_CA081() {
		CotizacionService.Desglose d = CotizacionService.calcular(new BigDecimal("99999.99"), 1, new BigDecimal("3.33"),
				new BigDecimal("0"), List.of());

		assertEquals(new BigDecimal("3330.00"), d.valorLimpieza());
		assertEquals(d.precioBase().add(d.valorLimpieza()), d.total());
	}

	// ----- políticas de cancelación (RF-002) -----

	@Test
	void flexibleDevuelveTodoConUnDiaDeAnticipacion_CA002_CA003() {
		PoliticaCancelacion flexible = politica(1, null, null);
		assertEquals(100, CancelacionService.porcentajeReembolso(flexible, 1));
		assertEquals(0, CancelacionService.porcentajeReembolso(flexible, 0));
	}

	@Test
	void moderadaDevuelveTodoOLaMitadSegunLosDias_CA002_CA003() {
		PoliticaCancelacion moderada = politica(5, 1, 50);
		assertEquals(100, CancelacionService.porcentajeReembolso(moderada, 5));
		assertEquals(50, CancelacionService.porcentajeReembolso(moderada, 4));
		assertEquals(50, CancelacionService.porcentajeReembolso(moderada, 1));
		assertEquals(0, CancelacionService.porcentajeReembolso(moderada, 0));
	}

	@Test
	void estrictaDevuelveLaMitadSoloConSieteDias_CA002_CA003() {
		PoliticaCancelacion estricta = politica(null, 7, 50);
		assertEquals(50, CancelacionService.porcentajeReembolso(estricta, 7));
		assertEquals(0, CancelacionService.porcentajeReembolso(estricta, 6));
	}

	// ----- fechas (RNF-027) -----

	@Test
	void validaElOrdenCronologicoDeLasFechas_CA121_CA122() {
		LocalDate manana = LocalDate.now().plusDays(1);
		assertNull(CalendarioService.validarFechas(manana, manana.plusDays(2)));
		assertTrue(CalendarioService.validarFechas(manana, manana).contains("inválidas"));
		assertTrue(CalendarioService.validarFechas(manana, manana.minusDays(1)).contains("inválidas"));
		assertTrue(CalendarioService.validarFechas(LocalDate.now().minusDays(1), manana).contains("pasado"));
		assertTrue(CalendarioService.validarFechas(null, manana) != null);
	}

	// ----- pago simulado (RF-028) -----

	@Test
	void elSimuladorUsaLosMetodosDePruebaDeStripe_CA161_CA162_CA163() {
		PagoService pagos = new PagoService(mock(PagoRepository.class), "");
		assertEquals(EstadoPago.APROBADO, pagos.cobrar(BigDecimal.TEN, "pm_card_visa", "x").estado());
		assertEquals(EstadoPago.RECHAZADO, pagos.cobrar(BigDecimal.TEN, "pm_card_chargeDeclined", "x").estado());
		assertEquals(EstadoPago.ERROR, pagos.cobrar(BigDecimal.TEN, "pm_card_error", "x").estado());
	}

	@Test
	void nuncaSeCobraConUnaClaveQueNoSeaDePrueba_CA160() {
		PagoService pagos = new PagoService(mock(PagoRepository.class), "sk_live_clave_real");
		PagoService.ResultadoPago resultado = pagos.cobrar(BigDecimal.TEN, "pm_card_visa", "x");
		assertEquals(EstadoPago.ERROR, resultado.estado());
		assertTrue(resultado.detalle().contains("prueba"));
	}

	// ----- segundo factor (RNF-033) -----

	@Test
	void elCodigoTotpActualEsValidoYUnoIncorrectoNo_CA175_CA176() {
		String secreto = TotpUtil.generarSecreto();
		assertTrue(TotpUtil.verificar(secreto, TotpUtil.codigo(secreto, Instant.now())));
		assertFalse(TotpUtil.verificar(secreto, "12345"));
		assertFalse(TotpUtil.verificar(secreto, null));
	}

	@Test
	void coincideConElVectorDePruebaDelRfc6238() {
		// Secreto ASCII "12345678901234567890" en Base32; RFC 6238, T = 59 s -> 94287082 (8 dígitos)
		String secreto = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
		assertEquals("287082", TotpUtil.codigo(secreto, Instant.ofEpochSecond(59)));
	}
}
