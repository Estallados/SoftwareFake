package co.edu.unbosque.marketplace.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.ConfiguracionTarifaDTO;
import co.edu.unbosque.marketplace.dto.MotivoDTO;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoResena;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import co.edu.unbosque.marketplace.service.PublicacionService;
import co.edu.unbosque.marketplace.service.ResenaService;
import co.edu.unbosque.marketplace.service.ReservaService;
import co.edu.unbosque.marketplace.service.TarifaService;
import co.edu.unbosque.marketplace.service.UsuarioService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de administración (solo ADMIN). Ruta base: {@code /admin}.
 * Moderación de alojamientos y reseñas, restricción de usuarios, consulta de
 * todas las reservas, tarifas globales y consulta de la auditoría.
 *
 * @see "RF-013, RF-014, RF-015, RF-016, RF-023, RF-027"
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración", description = "Moderación, usuarios, tarifas y auditoría")
@CrossOrigin(origins = "*")
public class AdminController {

	private static final Sort RECIENTES = Sort.by("fechaCreacion").descending();
	private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado.";

	private final UsuarioService usuarioService;
	private final PublicacionService publicacionService;
	private final ResenaService resenaService;
	private final ReservaService reservaService;
	private final TarifaService tarifaService;
	private final AuditoriaService auditoriaService;

	public AdminController(UsuarioService usuarioService, PublicacionService publicacionService,
			ResenaService resenaService, ReservaService reservaService, TarifaService tarifaService,
			AuditoriaService auditoriaService) {
		this.usuarioService = usuarioService;
		this.publicacionService = publicacionService;
		this.resenaService = resenaService;
		this.reservaService = reservaService;
		this.tarifaService = tarifaService;
		this.auditoriaService = auditoriaService;
	}

	// ----- usuarios -----

	@Operation(summary = "Usuarios (paginado)")
	@ApiResponse(responseCode = "200", description = "Página de usuarios")
	@GetMapping("/usuarios")
	public ResponseEntity<Object> usuarios(@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity
				.ok(usuarioService.listarPaginado(RespuestaUtil.pagina(pagina, tamanio, Sort.by("idUsuario"))));
	}

	@Operation(summary = "Reservas de un usuario como huésped (detalle de usuario)")
	@ApiResponse(responseCode = "200", description = "Reservas del usuario como huésped (vacía si no tiene)")
	@ApiResponse(responseCode = "404", description = USUARIO_NO_ENCONTRADO)
	@GetMapping("/usuarios/{id}/reservas")
	public ResponseEntity<Object> reservasDeUsuario(@PathVariable Long id, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		if (!usuarioService.existe(id)) {
			return RespuestaUtil.mensaje(HttpStatus.NOT_FOUND, USUARIO_NO_ENCONTRADO);
		}
		return ResponseEntity.ok(reservaService.listarMias(id, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	@Operation(summary = "Alojamientos de un usuario como anfitrión, en cualquier estado (detalle de usuario)")
	@ApiResponse(responseCode = "200", description = "Alojamientos del usuario como anfitrión, en cualquier estado")
	@ApiResponse(responseCode = "404", description = USUARIO_NO_ENCONTRADO)
	@GetMapping("/usuarios/{id}/alojamientos")
	public ResponseEntity<Object> alojamientosDeUsuario(@PathVariable Long id,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		if (!usuarioService.existe(id)) {
			return RespuestaUtil.mensaje(HttpStatus.NOT_FOUND, USUARIO_NO_ENCONTRADO);
		}
		return ResponseEntity
				.ok(publicacionService.listarDeAnfitrion(id, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	@Operation(summary = "Restringir un usuario con motivo")
	@ApiResponse(responseCode = "200", description = "Usuario restringido; se le notifica el motivo")
	@ApiResponse(responseCode = "400", description = "El motivo de la restricción es obligatorio.")
	@ApiResponse(responseCode = "403", description = "No se puede restringir a un administrador.")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@PatchMapping("/usuarios/{id}/restringir")
	public ResponseEntity<Object> restringir(@PathVariable Long id, @RequestBody MotivoDTO datos,
			Authentication authentication) {
		return RespuestaUtil
				.desde(usuarioService.restringir(id, datos.getMotivo(), RespuestaUtil.idUsuario(authentication)));
	}

	@Operation(summary = "Liberar un usuario restringido (motivo opcional)")
	@ApiResponse(responseCode = "200", description = "Usuario liberado")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@ApiResponse(responseCode = "409", description = "La cuenta no está restringida.")
	@PatchMapping("/usuarios/{id}/liberar")
	public ResponseEntity<Object> liberar(@PathVariable Long id, @RequestBody(required = false) MotivoDTO datos,
			Authentication authentication) {
		return RespuestaUtil.desde(usuarioService.liberar(id, datos == null ? null : datos.getMotivo(),
				RespuestaUtil.idUsuario(authentication)));
	}

	// ----- alojamientos -----

	@Operation(summary = "Alojamientos por estado (por ejemplo, la cola PENDIENTE_REVISION)")
	@ApiResponse(responseCode = "200", description = "Página de alojamientos, todos o los del estado indicado")
	@ApiResponse(responseCode = "400", description = "El valor de estado no es válido.")
	@GetMapping("/alojamientos")
	public ResponseEntity<Object> alojamientos(@RequestParam(required = false) EstadoAlojamiento estado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity
				.ok(publicacionService.listarPorEstado(estado, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	@Operation(summary = "Aprobar: PENDIENTE_REVISION → ACTIVA")
	@ApiResponse(responseCode = "200", description = "Alojamiento ACTIVA; se notifica al anfitrión")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de BORRADOR a ACTIVA.")
	@PostMapping("/alojamientos/{id}/aprobar")
	public ResponseEntity<Object> aprobar(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.aprobar(id, RespuestaUtil.idUsuario(authentication)));
	}

	@Operation(summary = "Rechazar con motivo: PENDIENTE_REVISION → BORRADOR (notifica al anfitrión)")
	@ApiResponse(responseCode = "200", description = "Alojamiento devuelto a BORRADOR; se notifica el motivo al "
			+ "anfitrión")
	@ApiResponse(responseCode = "400", description = "El motivo del rechazo es obligatorio.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de ACTIVA a BORRADOR.")
	@PostMapping("/alojamientos/{id}/rechazar")
	public ResponseEntity<Object> rechazar(@PathVariable Long id, @RequestBody MotivoDTO datos,
			Authentication authentication) {
		return RespuestaUtil
				.desde(publicacionService.rechazar(id, RespuestaUtil.idUsuario(authentication), datos.getMotivo()));
	}

	@Operation(summary = "Bloquear con motivo (las reservas futuras pasan a EN_REVISION)")
	@ApiResponse(responseCode = "200", description = "Alojamiento BLOQUEADA; sus reservas futuras pasan a EN_REVISION")
	@ApiResponse(responseCode = "400", description = "El motivo del bloqueo es obligatorio.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de BLOQUEADA a BLOQUEADA.")
	@PostMapping("/alojamientos/{id}/bloquear")
	public ResponseEntity<Object> bloquear(@PathVariable Long id, @RequestBody MotivoDTO datos,
			Authentication authentication) {
		return RespuestaUtil
				.desde(publicacionService.bloquear(id, RespuestaUtil.idUsuario(authentication), datos.getMotivo()));
	}

	@Operation(summary = "Desbloquear: BLOQUEADA → PAUSADA")
	@ApiResponse(responseCode = "200", description = "Alojamiento PAUSADA")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de ACTIVA a PAUSADA.")
	@PostMapping("/alojamientos/{id}/desbloquear")
	public ResponseEntity<Object> desbloquear(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.desbloquear(id, RespuestaUtil.idUsuario(authentication)));
	}

	// ----- reservas -----

	@Operation(summary = "Todas las reservas, opcionalmente filtradas por estado")
	@ApiResponse(responseCode = "200", description = "Página de reservas")
	@ApiResponse(responseCode = "400", description = "El valor de estado no es válido.")
	@GetMapping("/reservas")
	public ResponseEntity<Object> reservas(@RequestParam(required = false) EstadoReserva estado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(reservaService.listarTodas(estado, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	// ----- reseñas -----

	@Operation(summary = "Todas las reseñas para moderar, opcionalmente filtradas por estado")
	@ApiResponse(responseCode = "200", description = "Página de reseñas, de la más reciente a la más antigua")
	@ApiResponse(responseCode = "400", description = "El valor de estado no es válido.")
	@GetMapping("/resenas")
	public ResponseEntity<Object> resenas(@RequestParam(required = false) EstadoResena estado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(resenaService.listarTodas(estado,
				RespuestaUtil.pagina(pagina, tamanio, Sort.by("fecha").descending())));
	}

	@Operation(summary = "Ocultar una reseña con motivo (no se borra)")
	@ApiResponse(responseCode = "200", description = "Reseña OCULTA; se notifica el motivo a su autor")
	@ApiResponse(responseCode = "400", description = "El motivo para ocultar la reseña es obligatorio.")
	@ApiResponse(responseCode = "404", description = "La reseña no existe.")
	@PatchMapping("/resenas/{id}/ocultar")
	public ResponseEntity<Object> ocultarResena(@PathVariable Long id, @RequestBody MotivoDTO datos,
			Authentication authentication) {
		return RespuestaUtil
				.desde(resenaService.moderar(id, true, datos.getMotivo(), RespuestaUtil.idUsuario(authentication)));
	}

	@Operation(summary = "Volver a mostrar una reseña oculta")
	@ApiResponse(responseCode = "200", description = "Reseña PUBLICADA de nuevo")
	@ApiResponse(responseCode = "404", description = "La reseña no existe.")
	@PatchMapping("/resenas/{id}/mostrar")
	public ResponseEntity<Object> mostrarResena(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(resenaService.moderar(id, false, null, RespuestaUtil.idUsuario(authentication)));
	}

	// ----- tarifas -----

	@Operation(summary = "Porcentajes globales vigentes de limpieza y servicio")
	@ApiResponse(responseCode = "200", description = "Porcentajes vigentes de limpieza y servicio")
	@GetMapping("/tarifas")
	public ResponseEntity<Object> tarifas() {
		return ResponseEntity.ok(tarifaService.vigente());
	}

	@Operation(summary = "Cambiar los porcentajes globales (aplican a cotizaciones nuevas)")
	@ApiResponse(responseCode = "200", description = "Configuración nueva; aplica a las cotizaciones siguientes")
	@ApiResponse(responseCode = "400", description = "Los porcentajes de limpieza y servicio son obligatorios y deben "
			+ "estar entre 0 y 100.")
	@PutMapping("/tarifas")
	public ResponseEntity<Object> actualizarTarifas(@RequestBody ConfiguracionTarifaDTO datos,
			Authentication authentication) {
		return RespuestaUtil.desde(tarifaService.actualizar(datos, RespuestaUtil.idUsuario(authentication)));
	}

	// ----- auditoría -----

	@Operation(summary = "Consultar la auditoría con filtros por acción, actor y fechas")
	@ApiResponse(responseCode = "200", description = "Página de eventos, del más reciente al más antiguo")
	@ApiResponse(responseCode = "400", description = "El valor de desde no es válido.")
	@GetMapping("/auditoria")
	public ResponseEntity<Object> auditoria(@RequestParam(required = false) String accion,
			@RequestParam(required = false) Long idActor,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "50") int tamanio) {
		return ResponseEntity.ok(auditoriaService.consultar(accion, idActor, desde, hasta, RespuestaUtil.pagina(pagina,
				tamanio, Sort.by("fecha").descending().and(Sort.by("idEvento").descending()))));
	}

	@Operation(summary = "Detalle de un evento de auditoría")
	@ApiResponse(responseCode = "200", description = "Evento de auditoría")
	@ApiResponse(responseCode = "404", description = "El evento de auditoría no existe.")
	@GetMapping("/auditoria/{id}")
	public ResponseEntity<Object> evento(@PathVariable Long id) {
		return RespuestaUtil.desde(auditoriaService.obtener(id));
	}
}
