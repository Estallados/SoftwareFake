package co.edu.unbosque.marketplace.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.ConversacionDTO;
import co.edu.unbosque.marketplace.dto.MensajeDTO;
import co.edu.unbosque.marketplace.service.MensajeriaService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de mensajería huésped-anfitrión. Ruta base:
 * {@code /conversaciones}. Solo los participantes acceden a cada conversación.
 *
 * @see "RF-029"
 */
@RestController
@RequestMapping("/conversaciones")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mensajería", description = "Conversaciones entre huésped y anfitrión")
public class MensajeriaController {

    private final MensajeriaService mensajeriaService;

    public MensajeriaController(MensajeriaService mensajeriaService) {
        this.mensajeriaService = mensajeriaService;
    }

    @Operation(summary = "Iniciar o retomar una conversación con el anfitrión de un alojamiento")
    @ApiResponse(responseCode = "201", description = "Conversación con el anfitrión (la existente si ya había una)")
    @ApiResponse(responseCode = "400", description = "Debes indicar el alojamiento. | No puedes iniciar una "
            + "conversación con tu propio alojamiento. | El mensaje no puede superar 2000 caracteres.")
    @ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
    @PostMapping
    public ResponseEntity<Object> iniciar(@RequestBody ConversacionDTO datos, Authentication authentication) {
        return RespuestaUtil.desde(mensajeriaService.iniciar(datos, RespuestaUtil.idUsuario(authentication)),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Mis conversaciones")
    @ApiResponse(responseCode = "200", description = "Página de conversaciones del usuario")
    @GetMapping
    public ResponseEntity<Object> listar(Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(mensajeriaService.listar(RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.pagina(pagina, tamanio, Sort.by("fechaCreacion").descending())));
    }

    @Operation(summary = "Mensajes de una conversación (solo participantes)")
    @ApiResponse(responseCode = "200", description = "Página de mensajes")
    @ApiResponse(responseCode = "403", description = "No participas en esta conversación.")
    @ApiResponse(responseCode = "404", description = "La conversación no existe.")
    @GetMapping("/{id}/mensajes")
    public ResponseEntity<Object> mensajes(@PathVariable Long id, Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {
        return RespuestaUtil.desde(mensajeriaService.mensajes(id, RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.pagina(pagina, tamanio, Sort.by("fechaEnvio").and(Sort.by("idMensaje")))));
    }

    @Operation(summary = "Enviar un mensaje (solo participantes)")
    @ApiResponse(responseCode = "201", description = "Mensaje enviado; se notifica al otro participante")
    @ApiResponse(responseCode = "400", description = "El mensaje debe tener entre 1 y 2000 caracteres.")
    @ApiResponse(responseCode = "403", description = "No participas en esta conversación.")
    @ApiResponse(responseCode = "404", description = "La conversación no existe.")
    @PostMapping("/{id}/mensajes")
    public ResponseEntity<Object> enviar(@PathVariable Long id, @RequestBody MensajeDTO datos,
            Authentication authentication) {
        return RespuestaUtil.desde(mensajeriaService.enviar(id, datos.getContenido(),
                RespuestaUtil.idUsuario(authentication)), HttpStatus.CREATED);
    }
}
