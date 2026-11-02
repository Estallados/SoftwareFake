package co.edu.unbosque.marketplace.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.Usuario;

/**
 * Utilidades comunes de los controllers: respuestas {@code {"message": ...}},
 * traducción de {@link Resultado} a HTTP, identidad del token y paginación.
 *
 * @see "RNF-022, CA-149"
 */
public final class RespuestaUtil {

	/** Tamaño máximo de página que acepta la API. */
	public static final int TAMANIO_MAXIMO = 100;

	private RespuestaUtil() {
		throw new IllegalStateException("Clase utilitaria");
	}

	/** Respuesta JSON uniforme: {"message": "..."}. */
	public static ResponseEntity<Object> mensaje(HttpStatus status, String texto) {
		return ResponseEntity.status(status).body(Map.of("message", texto));
	}

	/**
	 * Traduce un {@link Resultado} a HTTP: 200 con el dato si fue exitoso; si no,
	 * el estado que corresponde al código y {"message": causa}. Si el resultado
	 * rechazado trae dato (por ejemplo, la reserva con pago rechazado), se incluye
	 * como "dato".
	 */
	public static ResponseEntity<Object> desde(Resultado<?> resultado) {
		return desde(resultado, HttpStatus.OK);
	}

	/**
	 * Igual que {@link #desde(Resultado)} pero con el estado de éxito indicado (por
	 * ejemplo 201).
	 */
	public static ResponseEntity<Object> desde(Resultado<?> resultado, HttpStatus estadoExito) {
		if (resultado.esExito()) {
			return ResponseEntity.status(estadoExito).body(resultado.getDato());
		}
		HttpStatus estado = estadoDe(resultado.getCodigo());
		if (resultado.getDato() == null) {
			return mensaje(estado, resultado.getMensaje());
		}
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("message", resultado.getMensaje());
		cuerpo.put("dato", resultado.getDato());
		return ResponseEntity.status(estado).body(cuerpo);
	}

	/** Estado HTTP de cada código de {@link Resultado}. */
	public static HttpStatus estadoDe(int codigo) {
		return switch (codigo) {
		case Resultado.NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
		case Resultado.DATOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
		case Resultado.SIN_PERMISO -> HttpStatus.FORBIDDEN;
		case Resultado.ESTADO_INVALIDO, Resultado.NO_DISPONIBLE -> HttpStatus.CONFLICT;
		case Resultado.PAGO_RECHAZADO -> HttpStatus.PAYMENT_REQUIRED;
		case Resultado.PAGO_ERROR -> HttpStatus.BAD_GATEWAY;
		default -> HttpStatus.INTERNAL_SERVER_ERROR;
		};
	}

	/**
	 * Id del usuario autenticado (el principal es la entidad Usuario); null si no
	 * hay sesión.
	 */
	public static Long idUsuario(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
			return usuario.getIdUsuario();
		}
		return null;
	}

	/** Indica si el usuario autenticado es administrador. */
	public static boolean esAdmin(Authentication authentication) {
		return authentication != null && authentication.getPrincipal() instanceof Usuario usuario
				&& usuario.getRol() == Role.ADMIN;
	}

	/**
	 * Página saneada: página &gt;= 0 y tamaño entre 1 y {@link #TAMANIO_MAXIMO}.
	 * La página se limita para que el desplazamiento (página × tamaño) quepa en
	 * un int; si no, la consulta falla con 500.
	 */
	public static Pageable pagina(int pagina, int tamanio, Sort orden) {
		int tamanioValido = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
		int paginaValida = Math.min(Math.max(pagina, 0), Integer.MAX_VALUE / tamanioValido - 1);
		return PageRequest.of(paginaValida, tamanioValido, orden);
	}
}
