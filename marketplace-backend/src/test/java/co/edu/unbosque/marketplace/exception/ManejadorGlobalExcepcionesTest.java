package co.edu.unbosque.marketplace.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import co.edu.unbosque.marketplace.service.AuditoriaService;

/** Cada error técnico responde el código HTTP que le corresponde, no un 500 genérico. */
class ManejadorGlobalExcepcionesTest {

	private AuditoriaService auditoriaService;
	private ManejadorGlobalExcepciones manejador;
	private final MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/favoritos/3");

	@BeforeEach
	void preparar() {
		auditoriaService = mock(AuditoriaService.class);
		manejador = new ManejadorGlobalExcepciones(auditoriaService);
	}

	private static DataIntegrityViolationException violacion(String estadoSql) {
		return new DataIntegrityViolationException("fallo", new SQLException("fallo", estadoSql));
	}

	@Test
	void unDuplicadoPorPeticionesSimultaneasEs409() {
		assertEquals(HttpStatus.CONFLICT, manejador.conflictoDeDatos(violacion("23505"), peticion).getStatusCode());
		verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any());
	}

	@Test
	void otraViolacionDeIntegridadSigueSiendoUnError500Auditado() {
		// 23502: columna obligatoria en null, un error del código
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
				manejador.conflictoDeDatos(violacion("23502"), peticion).getStatusCode());
		verify(auditoriaService).registrar(any(), any(), any(), any(), any(), any());
	}

	@Test
	void unBloqueoConOtraOperacionEs409() {
		assertEquals(HttpStatus.CONFLICT, manejador.conflictoConcurrente().getStatusCode());
	}

	@Test
	void laBaseCaidaEs503SinIntentarAuditarEnElla() {
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, manejador
				.baseNoDisponible(new DataAccessResourceFailureException("sin conexión"), peticion).getStatusCode());
		verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any());
	}

	@Test
	void unaExcepcionDeSpringConEstado4xxConservaSuCodigo() {
		assertEquals(HttpStatus.GONE,
				manejador.errorInesperado(new ResponseStatusException(HttpStatus.GONE), peticion).getStatusCode());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, manejador
				.errorInesperado(new IllegalStateException("fallo"), peticion).getStatusCode());
	}

	@Test
	void pedirOtroFormatoQueJsonEs406() {
		assertEquals(HttpStatus.NOT_ACCEPTABLE, manejador.formatoNoAceptable().getStatusCode());
	}
}
