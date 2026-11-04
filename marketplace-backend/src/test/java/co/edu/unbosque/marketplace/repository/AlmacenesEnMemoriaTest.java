package co.edu.unbosque.marketplace.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.entity.CodigoVerificacion;
import co.edu.unbosque.marketplace.entity.TokenRecuperacion;

/** Pruebas de los almacenes en memoria de códigos de verificación y tokens de recuperación. */
class AlmacenesEnMemoriaTest {

	private static CodigoVerificacion codigo(long idUsuario, LocalDateTime vence, boolean usado) {
		CodigoVerificacion codigo = new CodigoVerificacion();
		codigo.setIdUsuario(idUsuario);
		codigo.setCodigoHash("hash-" + idUsuario);
		codigo.setFechaExpiracion(vence);
		codigo.setUsado(usado);
		return codigo;
	}

	private static TokenRecuperacion token(long idUsuario, String hash, LocalDateTime vence, boolean usado) {
		TokenRecuperacion token = new TokenRecuperacion();
		token.setIdUsuario(idUsuario);
		token.setTokenHash(hash);
		token.setFechaExpiracion(vence);
		token.setUsado(usado);
		return token;
	}

	@Test
	void elCodigoNuevoReemplazaAlAnteriorDelUsuario() {
		CodigoVerificacionRepository repo = new CodigoVerificacionRepository();
		LocalDateTime vence = LocalDateTime.now().plusMinutes(15);
		repo.save(codigo(1L, vence, false));
		CodigoVerificacion nuevo = repo.save(codigo(1L, vence, false));

		assertEquals(1, repo.count());
		assertSame(nuevo, repo.findTopByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(1L).orElseThrow());
	}

	@Test
	void unCodigoUsadoNoSeDevuelve() {
		CodigoVerificacionRepository repo = new CodigoVerificacionRepository();
		repo.save(codigo(1L, LocalDateTime.now().plusMinutes(15), true));

		assertTrue(repo.findTopByIdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(1L).isEmpty());
		assertTrue(repo.findByIdUsuarioAndUsadoFalse(1L).isEmpty());
	}

	@Test
	void laLimpiezaQuitaCodigosUsadosYVencidos() {
		CodigoVerificacionRepository repo = new CodigoVerificacionRepository();
		LocalDateTime futuro = LocalDateTime.now().plusMinutes(15);
		repo.save(codigo(1L, futuro, false));
		repo.save(codigo(2L, futuro, true));
		repo.save(codigo(3L, LocalDateTime.now().minusMinutes(1), false));

		repo.limpiar();

		assertEquals(1, repo.count());
		assertEquals(1L, repo.findAll().get(0).getIdUsuario());
	}

	@Test
	void unTokenUsadoSigueEncontrandosePorHashHastaLaLimpieza() {
		TokenRecuperacionRepository repo = new TokenRecuperacionRepository();
		repo.save(token(1L, "abc", LocalDateTime.now().plusMinutes(30), true));

		assertTrue(repo.findByTokenHash("abc").isPresent());
		assertTrue(repo.findByIdUsuarioAndUsadoFalse(1L).isEmpty());

		repo.limpiar();

		assertTrue(repo.findByTokenHash("abc").isEmpty());
	}

	@Test
	void elTokenNuevoReemplazaAlAnteriorDelUsuario() {
		TokenRecuperacionRepository repo = new TokenRecuperacionRepository();
		LocalDateTime vence = LocalDateTime.now().plusMinutes(30);
		repo.save(token(1L, "viejo", vence, false));
		repo.save(token(1L, "nuevo", vence, false));

		assertEquals(1, repo.count());
		assertTrue(repo.findByTokenHash("viejo").isEmpty());
		assertTrue(repo.findByTokenHash("nuevo").isPresent());
	}
}
