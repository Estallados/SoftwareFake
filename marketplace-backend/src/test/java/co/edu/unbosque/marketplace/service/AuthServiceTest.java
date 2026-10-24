package co.edu.unbosque.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.TokenRecuperacion;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.TokenRecuperacionRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;

/** Pruebas unitarias de {@link AuthService}: recuperación simulada y reactivación. */
class AuthServiceTest {

	private UsuarioRepository usuarioRepo;
	private TokenRecuperacionRepository tokenRepo;
	private PasswordEncoder passwordEncoder;
	private AuthService authService;

	@BeforeEach
	void preparar() {
		usuarioRepo = mock(UsuarioRepository.class);
		tokenRepo = mock(TokenRecuperacionRepository.class);
		passwordEncoder = new BCryptPasswordEncoder(4);
		authService = new AuthService(usuarioRepo, tokenRepo, passwordEncoder, mock(AuditoriaService.class));
	}

	private Usuario usuario(EstadoUsuario estado, String contrasenia) {
		Usuario usuario = new Usuario();
		usuario.setIdUsuario(5L);
		usuario.setRol(Role.HUESPED);
		usuario.setEstado(estado);
		usuario.setContrasenia(passwordEncoder.encode(contrasenia));
		return usuario;
	}

	private TokenRecuperacion token(String tokenEnClaro, LocalDateTime vence, boolean usado) {
		TokenRecuperacion token = new TokenRecuperacion();
		token.setIdUsuario(5L);
		token.setTokenHash(AESUtil.hash(tokenEnClaro));
		token.setFechaExpiracion(vence);
		token.setUsado(usado);
		return token;
	}

	@Test
	void debeGenerarTokenDe30MinutosGuardandoSoloSuHash() {
		when(usuarioRepo.findByCorreoHash(anyString())).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO, "x")));
		TokenRecuperacion anterior = token("viejo", LocalDateTime.now().plusMinutes(10), false);
		when(tokenRepo.findByIdUsuarioAndUsadoFalse(5L)).thenReturn(List.of(anterior));

		assertEquals(0, authService.solicitarRecuperacion("ana@correo.com"));

		ArgumentCaptor<TokenRecuperacion> captor = ArgumentCaptor.forClass(TokenRecuperacion.class);
		verify(tokenRepo, times(2)).save(captor.capture());
		TokenRecuperacion nuevo = captor.getAllValues().get(1);
		assertEquals(5L, nuevo.getIdUsuario());
		assertFalse(nuevo.getUsado());
		assertTrue(nuevo.getFechaExpiracion().isAfter(LocalDateTime.now().plusMinutes(29)));
		assertTrue(nuevo.getFechaExpiracion().isBefore(LocalDateTime.now().plusMinutes(31)));
		// El token anterior queda invalidado
		assertTrue(anterior.getUsado());
	}

	@Test
	void debeResponderIgualSiElCorreoNoExiste() {
		when(usuarioRepo.findByCorreoHash(anyString())).thenReturn(Optional.empty());

		assertEquals(0, authService.solicitarRecuperacion("nadie@correo.com"));
		verify(tokenRepo, never()).save(any());
	}

	@Test
	void debeRechazarRecuperacionSinCorreo() {
		assertEquals(2, authService.solicitarRecuperacion(" "));
	}

	@Test
	void debeRestablecerContraseniaConTokenVigente() {
		Usuario usuario = usuario(EstadoUsuario.ACTIVO, "Vieja#2026");
		TokenRecuperacion token = token("abc", LocalDateTime.now().plusMinutes(5), false);
		when(tokenRepo.findByTokenHash(AESUtil.hash("abc"))).thenReturn(Optional.of(token));
		when(usuarioRepo.findById(5L)).thenReturn(Optional.of(usuario));

		assertEquals(0, authService.restablecerContrasenia("abc", "Nueva#2026"));
		assertTrue(passwordEncoder.matches("Nueva#2026", usuario.getContrasenia()));
		assertTrue(token.getUsado());
	}

	@Test
	void debeRechazarTokenVencido() {
		when(tokenRepo.findByTokenHash(AESUtil.hash("abc")))
				.thenReturn(Optional.of(token("abc", LocalDateTime.now().minusMinutes(1), false)));

		assertEquals(1, authService.restablecerContrasenia("abc", "Nueva#2026"));
	}

	@Test
	void debeRechazarTokenYaUsado() {
		when(tokenRepo.findByTokenHash(AESUtil.hash("abc")))
				.thenReturn(Optional.of(token("abc", LocalDateTime.now().plusMinutes(5), true)));

		assertEquals(1, authService.restablecerContrasenia("abc", "Nueva#2026"));
	}

	@Test
	void debeRechazarContraseniaNuevaDebil_RNF008() {
		assertEquals(3, authService.restablecerContrasenia("abc", "debil"));
		verify(tokenRepo, never()).findByTokenHash(anyString());
	}

	@Test
	void debeReactivarCuentaInactivaConCredencialesCorrectas() {
		Usuario usuario = usuario(EstadoUsuario.INACTIVO, "Segura#2026");
		when(usuarioRepo.findByCorreoHash(anyString())).thenReturn(Optional.of(usuario));

		assertEquals(0, authService.reactivarCuenta("ana@correo.com", "Segura#2026"));
		assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
	}

	@Test
	void noDebeReactivarCuentaRestringidaPorUnAdministrador() {
		Usuario usuario = usuario(EstadoUsuario.RESTRINGIDO, "Segura#2026");
		when(usuarioRepo.findByCorreoHash(anyString())).thenReturn(Optional.of(usuario));

		assertEquals(3, authService.reactivarCuenta("ana@correo.com", "Segura#2026"));
		assertEquals(EstadoUsuario.RESTRINGIDO, usuario.getEstado());
	}

	@Test
	void noDebeReactivarConContraseniaIncorrecta() {
		Usuario usuario = usuario(EstadoUsuario.INACTIVO, "Segura#2026");
		when(usuarioRepo.findByCorreoHash(anyString())).thenReturn(Optional.of(usuario));

		assertEquals(1, authService.reactivarCuenta("ana@correo.com", "Otra#2026"));
		assertEquals(EstadoUsuario.INACTIVO, usuario.getEstado());
	}

	@Test
	void debeIndicarQueLaCuentaYaEstaActiva() {
		when(usuarioRepo.findByCorreoHash(anyString()))
				.thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO, "Segura#2026")));

		assertEquals(4, authService.reactivarCuenta("ana@correo.com", "Segura#2026"));
	}
}
