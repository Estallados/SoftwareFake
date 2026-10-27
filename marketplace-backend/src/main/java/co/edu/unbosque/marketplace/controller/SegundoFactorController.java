package co.edu.unbosque.marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
 * Controlador REST del segundo factor (TOTP). Ruta base:
 * {@code /usuarios/segundofactor}.
 *
 * @see "RNF-033"
 */
@RestController
@RequestMapping("/usuarios/segundofactor")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Segundo factor", description = "Autenticación en dos pasos con app autenticadora")
public class SegundoFactorController {

    private final SegundoFactorService segundoFactorService;

    public SegundoFactorController(SegundoFactorService segundoFactorService) {
        this.segundoFactorService = segundoFactorService;
    }

    @Operation(summary = "Generar el secreto TOTP (paso 1)",
            description = "Devuelve el secreto y la URI otpauth para registrarlo en la app autenticadora.")
    @ApiResponse(responseCode = "200", description = "{secreto, uri}: la URI otpauth se muestra como código QR")
    @ApiResponse(responseCode = "409", description = "El segundo factor ya está habilitado.")
    @PostMapping("/activar")
    public ResponseEntity<Object> activar(Authentication authentication) {
        return RespuestaUtil.desde(segundoFactorService.activar(RespuestaUtil.idUsuario(authentication)));
    }

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
