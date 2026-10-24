package co.edu.unbosque.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.entity.EventoAuditoria;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.AuditoriaRepository;

/** Pruebas unitarias de {@link AuditoriaService}. */
class AuditoriaServiceTest {

	private AuditoriaRepository auditoriaRepo;
	private AuditoriaService auditoriaService;

	@BeforeEach
	void preparar() {
		auditoriaRepo = mock(AuditoriaRepository.class);
		auditoriaService = new AuditoriaService(auditoriaRepo, mock(PlatformTransactionManager.class),
				new ObjectMapper());
	}

	@Test
	void debeGuardarEventoConActorFechaYResultado_CA136_CA137_CA138() {
		auditoriaService.registrar(7L, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, 7L,
				ResultadoAuditoria.EXITOSO, "detalle");

		ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
		verify(auditoriaRepo).save(captor.capture());
		EventoAuditoria evento = captor.getValue();
		assertEquals(7L, evento.getIdUsuario());
		assertEquals(AuditoriaService.LOGIN, evento.getAccion());
		assertEquals(AuditoriaService.ENTIDAD_USUARIO, evento.getEntidad());
		assertEquals(7L, evento.getIdEntidad());
		assertEquals(ResultadoAuditoria.EXITOSO, evento.getResultado());
		assertEquals("detalle", evento.getDetalle());
		assertNotNull(evento.getFecha());
	}

	@Test
	void debeGuardarTambienLasOperacionesRechazadas_CA152() {
		auditoriaService.registrar(null, AuditoriaService.LOGIN, AuditoriaService.ENTIDAD_USUARIO, null,
				ResultadoAuditoria.RECHAZADO, "Credenciales inválidas");

		ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
		verify(auditoriaRepo).save(captor.capture());
		assertEquals(ResultadoAuditoria.RECHAZADO, captor.getValue().getResultado());
	}

	@Test
	void noDebeInterrumpirLaOperacionSiLaAuditoriaFalla_CA152() {
		when(auditoriaRepo.save(any())).thenThrow(new IllegalStateException("base caída"));

		assertDoesNotThrow(() -> auditoriaService.registrar(1L, AuditoriaService.LOGIN, null, null,
				ResultadoAuditoria.EXITOSO, null));
	}

	@Test
	void debeRecortarDetallesMuyLargos() {
		auditoriaService.registrar(1L, AuditoriaService.LOGIN, null, null, ResultadoAuditoria.EXITOSO,
				"x".repeat(5000));

		ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
		verify(auditoriaRepo).save(captor.capture());
		assertEquals(1000, captor.getValue().getDetalle().length());
	}
}
