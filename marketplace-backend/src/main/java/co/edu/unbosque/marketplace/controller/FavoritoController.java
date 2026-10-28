package co.edu.unbosque.marketplace.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.service.FavoritoService;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de favoritos. Ruta base: {@code /favoritos}.
 *
 * @see "RF-007"
 */
@RestController
@RequestMapping("/favoritos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Favoritos", description = "Alojamientos guardados por el usuario")
@CrossOrigin(origins = "*")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @Operation(summary = "Mis favoritos")
    @ApiResponse(responseCode = "200", description = "Página de favoritos")
    @GetMapping
    public ResponseEntity<Object> listar(Authentication authentication,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(favoritoService.listar(RespuestaUtil.idUsuario(authentication),
                RespuestaUtil.pagina(pagina, tamanio, Sort.by("fechaAgregado").descending())));
    }

    @Operation(summary = "Agregar a favoritos (sin duplicados)")
    @ApiResponse(responseCode = "200", description = "Favorito guardado (si ya estaba, el mismo)")
    @ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
    @PostMapping("/{idAlojamiento}")
    public ResponseEntity<Object> agregar(@PathVariable Long idAlojamiento, Authentication authentication) {
        return RespuestaUtil.desde(favoritoService.agregar(RespuestaUtil.idUsuario(authentication), idAlojamiento));
    }

    @Operation(summary = "Quitar de favoritos")
    @ApiResponse(responseCode = "200", description = "Alojamiento quitado de favoritos.")
    @ApiResponse(responseCode = "404", description = "El alojamiento no está en tus favoritos.")
    @DeleteMapping("/{idAlojamiento}")
    public ResponseEntity<Object> eliminar(@PathVariable Long idAlojamiento, Authentication authentication) {
        Resultado<Void> resultado = favoritoService.eliminar(RespuestaUtil.idUsuario(authentication), idAlojamiento);
        return resultado.esExito() ? RespuestaUtil.mensaje(HttpStatus.OK, "Alojamiento quitado de favoritos.")
                : RespuestaUtil.desde(resultado);
    }
}
