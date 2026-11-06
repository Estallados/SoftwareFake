package co.edu.unbosque.marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.SegundoFactorDTO;
import co.edu.unbosque.marketplace.service.SegundoFactorService;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST del segundo factor (códigos TOTP de una app autenticadora).
 * Ruta base: {@code /usuarios/segundofactor}. Activarlo tiene dos pasos:
 * generar el secreto y confirmarlo con un código.
 */
@RestController
@RequestMapping("/usuarios/segundofactor")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Segundo factor", description = "Autenticación en dos pasos con app autenticadora")
@CrossOrigin(origins = "*")
public class SegundoFactorController {

	/** Servicio del segundo factor. */
	private final SegundoFactorService segundoFactorService;

	/**
	 * Crea el controlador.
	 *
	 * @param segundoFactorService servicio del segundo factor
	 */
	public SegundoFactorController(SegundoFactorService segundoFactorService) {
		this.segundoFactorService = segundoFactorService;
	}

	/**
	 * Paso 1: genera el secreto TOTP. Ruta:
	 * {@code POST /usuarios/segundofactor/activar}.
	 *
	 * @param authentication usuario autenticado
	 * @return 200 con {secreto, uri} (la URI otpauth se muestra como código
	 *         QR); 409 si ya está habilitado
	 */
	@Operation(summary = "Generar el secreto TOTP (paso 1)", description = "Devuelve el secreto y la URI otpauth para registrarlo en la app autenticadora.")
	@ApiResponse(responseCode = "200", description = "{secreto, uri}: la URI otpauth se muestra como código QR")
	@ApiResponse(responseCode = "409", description = "El segundo factor ya está habilitado.")
	@PostMapping("/activar")
	public ResponseEntity<Object> activar(Authentication authentication) {
		return RespuestaUtil.desde(segundoFactorService.activar(RespuestaUtil.idUsuario(authentication)));
	}

	/**
	 * Paso 2: confirma la activación con un código de la app. Ruta:
	 * {@code POST /usuarios/segundofactor/confirmar}.
	 *
	 * @param datos DTO con el {@code codigo}
	 * @param authentication usuario autenticado
	 * @return 200 si quedó habilitado; 400 si el código no es válido; 409 si no
	 *         se pidió la activación
	 */
	@Operation(summary = "Confirmar la activación con un código (paso 2)")
	@ApiResponse(responseCode = "200", description = "Segundo factor habilitado.")
	@ApiResponse(responseCode = "400", description = "El código no es válido.")
	@ApiResponse(responseCode = "409", description = "Primero solicita la activación del segundo factor.")
	@PostMapping("/confirmar")
	public ResponseEntity<Object> confirmar(@RequestBody SegundoFactorDTO datos, Authentication authentication) {
		Resultado<Void> resultado = segundoFactorService.confirmar(RespuestaUtil.idUsuario(authentication),
				datos.getCodigo());
		return resultado.esExito() ? RespuestaUtil.mensaje(HttpStatus.OK, "Segundo factor habilitado.")
				: RespuestaUtil.desde(resultado);
	}

	/**
	 * Deshabilita el segundo factor con un código actual. Ruta:
	 * {@code POST /usuarios/segundofactor/desactivar}.
	 *
	 * @param datos DTO con el {@code codigo}
	 * @param authentication usuario autenticado
	 * @return 200 si se deshabilitó; 400 si el código no es válido; 409 si no
	 *         estaba habilitado
	 */
	@Operation(summary = "Deshabilitar el segundo factor con un código actual")
	@ApiResponse(responseCode = "200", description = "Segundo factor deshabilitado.")
	@ApiResponse(responseCode = "400", description = "El código no es válido.")
	@ApiResponse(responseCode = "409", description = "El segundo factor no está habilitado.")
	@PostMapping("/desactivar")
	public ResponseEntity<Object> desactivar(@RequestBody SegundoFactorDTO datos, Authentication authentication) {
		Resultado<Void> resultado = segundoFactorService.desactivar(RespuestaUtil.idUsuario(authentication),
				datos.getCodigo());
		return resultado.esExito() ? RespuestaUtil.mensaje(HttpStatus.OK, "Segundo factor deshabilitado.")
				: RespuestaUtil.desde(resultado);
	}
}
