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
 * Controlador REST de administración. Ruta base: {@code /admin}; solo para
 * administradores. Agrupa la moderación de alojamientos y reseñas, la
 * restricción de usuarios, la consulta de todas las reservas, las tarifas
 * globales y la consulta de la auditoría.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración", description = "Moderación, usuarios, tarifas y auditoría")
@CrossOrigin(origins = "*")
public class AdminController {

	/** Orden de los listados: los más recientes primero. */
	private static final Sort RECIENTES = Sort.by("fechaCreacion").descending();

	/** Mensaje cuando el usuario no existe. */
	private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado.";

	/** Servicio de usuarios. */
	private final UsuarioService usuarioService;

	/** Servicio de publicaciones. */
	private final PublicacionService publicacionService;

	/** Servicio de reseñas. */
	private final ResenaService resenaService;

	/** Servicio de reservas. */
	private final ReservaService reservaService;

	/** Servicio de tarifas. */
	private final TarifaService tarifaService;

	/** Servicio de auditoría. */
	private final AuditoriaService auditoriaService;

	/**
	 * Crea el controlador.
	 *
	 * @param usuarioService servicio de usuarios
	 * @param publicacionService servicio de publicaciones
	 * @param resenaService servicio de reseñas
	 * @param reservaService servicio de reservas
	 * @param tarifaService servicio de tarifas
	 * @param auditoriaService servicio de auditoría
	 */
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

	/**
	 * Usuarios, paginados. Ruta: {@code GET /admin/usuarios}.
	 *
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de usuarios
	 */
	@Operation(summary = "Usuarios (paginado)")
	@ApiResponse(responseCode = "200", description = "Página de usuarios")
	@GetMapping("/usuarios")
	public ResponseEntity<Object> usuarios(@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity
				.ok(usuarioService.listarPaginado(RespuestaUtil.pagina(pagina, tamanio, Sort.by("idUsuario"))));
	}

	/**
	 * Reservas de un usuario como huésped, para el detalle de usuario. Ruta:
	 * {@code GET /admin/usuarios/{id}/reservas}.
	 *
	 * @param id usuario
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de reservas (vacía si no tiene); 404 si el
	 *         usuario no existe
	 */
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

	/**
	 * Alojamientos de un usuario como anfitrión, en cualquier estado, para el
	 * detalle de usuario. Ruta: {@code GET /admin/usuarios/{id}/alojamientos}.
	 *
	 * @param id usuario
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de alojamientos; 404 si el usuario no existe
	 */
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

	/**
	 * Restringe un usuario con un motivo, que se le notifica. Ruta:
	 * {@code PATCH /admin/usuarios/{id}/restringir}.
	 *
	 * @param id usuario
	 * @param datos DTO con el {@code motivo}
	 * @param authentication administrador autenticado
	 * @return 200 con el usuario; 400 si falta el motivo; 403 si es
	 *         administrador; 404 si no existe
	 */
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

	/**
	 * Activa un usuario restringido o inactivo; el motivo es opcional. Ruta:
	 * {@code PATCH /admin/usuarios/{id}/liberar}.
	 *
	 * @param id usuario
	 * @param datos DTO con el {@code motivo}; puede no enviarse
	 * @param authentication administrador autenticado
	 * @return 200 con el usuario; 404 si no existe; 409 si ya está activo
	 */
	@Operation(summary = "Activar un usuario restringido o inactivo (motivo opcional)")
	@ApiResponse(responseCode = "200", description = "Usuario activado")
	@ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
	@ApiResponse(responseCode = "409", description = "La cuenta ya está activa.")
	@PatchMapping("/usuarios/{id}/liberar")
	public ResponseEntity<Object> liberar(@PathVariable Long id, @RequestBody(required = false) MotivoDTO datos,
			Authentication authentication) {
		return RespuestaUtil.desde(usuarioService.liberar(id, datos == null ? null : datos.getMotivo(),
				RespuestaUtil.idUsuario(authentication)));
	}

	// ----- alojamientos -----

	/**
	 * Alojamientos por estado, por ejemplo la cola PENDIENTE_REVISION; sin
	 * estado, todos. Ruta: {@code GET /admin/alojamientos}.
	 *
	 * @param estado estado a filtrar; opcional
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de alojamientos
	 */
	@Operation(summary = "Alojamientos por estado (por ejemplo, la cola PENDIENTE_REVISION)")
	@ApiResponse(responseCode = "200", description = "Página de alojamientos, todos o los del estado indicado")
	@ApiResponse(responseCode = "400", description = "El valor de estado no es válido.")
	@GetMapping("/alojamientos")
	public ResponseEntity<Object> alojamientos(@RequestParam(required = false) EstadoAlojamiento estado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity
				.ok(publicacionService.listarPorEstado(estado, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	/**
	 * Aprueba una publicación: PENDIENTE_REVISION → ACTIVA, y se le avisa al
	 * anfitrión. Ruta: {@code POST /admin/alojamientos/{id}/aprobar}.
	 *
	 * @param id alojamiento
	 * @param authentication administrador autenticado
	 * @return 200 con el alojamiento; 404 si no existe; 409 si el estado no lo
	 *         permite
	 */
	@Operation(summary = "Aprobar: PENDIENTE_REVISION → ACTIVA")
	@ApiResponse(responseCode = "200", description = "Alojamiento ACTIVA; se notifica al anfitrión")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de BORRADOR a ACTIVA.")
	@PostMapping("/alojamientos/{id}/aprobar")
	public ResponseEntity<Object> aprobar(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.aprobar(id, RespuestaUtil.idUsuario(authentication)));
	}

	/**
	 * Rechaza una publicación con motivo: PENDIENTE_REVISION → BORRADOR, y se
	 * le avisa al anfitrión. Ruta:
	 * {@code POST /admin/alojamientos/{id}/rechazar}.
	 *
	 * @param id alojamiento
	 * @param datos DTO con el {@code motivo}
	 * @param authentication administrador autenticado
	 * @return 200 con el alojamiento; 400 si falta el motivo; 404 si no existe;
	 *         409 si el estado no lo permite
	 */
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

	/**
	 * Bloquea una publicación con motivo; sus reservas futuras pasan a
	 * EN_REVISION. Ruta: {@code POST /admin/alojamientos/{id}/bloquear}.
	 *
	 * @param id alojamiento
	 * @param datos DTO con el {@code motivo}
	 * @param authentication administrador autenticado
	 * @return 200 con el alojamiento; 400 si falta el motivo; 404 si no existe;
	 *         409 si ya estaba bloqueado
	 */
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

	/**
	 * Desbloquea una publicación: BLOQUEADA → PAUSADA. Ruta:
	 * {@code POST /admin/alojamientos/{id}/desbloquear}.
	 *
	 * @param id alojamiento
	 * @param authentication administrador autenticado
	 * @return 200 con el alojamiento; 404 si no existe; 409 si no estaba
	 *         bloqueado
	 */
	@Operation(summary = "Desbloquear: BLOQUEADA → PAUSADA")
	@ApiResponse(responseCode = "200", description = "Alojamiento PAUSADA")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de ACTIVA a PAUSADA.")
	@PostMapping("/alojamientos/{id}/desbloquear")
	public ResponseEntity<Object> desbloquear(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.desbloquear(id, RespuestaUtil.idUsuario(authentication)));
	}

	// ----- reservas -----

	/**
	 * Todas las reservas, opcionalmente filtradas por estado. Ruta:
	 * {@code GET /admin/reservas}.
	 *
	 * @param estado estado a filtrar; opcional
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de reservas
	 */
	@Operation(summary = "Todas las reservas, opcionalmente filtradas por estado")
	@ApiResponse(responseCode = "200", description = "Página de reservas")
	@ApiResponse(responseCode = "400", description = "El valor de estado no es válido.")
	@GetMapping("/reservas")
	public ResponseEntity<Object> reservas(@RequestParam(required = false) EstadoReserva estado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(reservaService.listarTodas(estado, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	// ----- reseñas -----

	/**
	 * Todas las reseñas para moderar, opcionalmente filtradas por estado.
	 * Ruta: {@code GET /admin/resenas}.
	 *
	 * @param estado estado a filtrar; opcional
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de reseñas, de la más reciente a la más antigua
	 */
	@Operation(summary = "Todas las reseñas para moderar, opcionalmente filtradas por estado")
	@ApiResponse(responseCode = "200", description = "Página de reseñas, de la más reciente a la más antigua")
	@ApiResponse(responseCode = "400", description = "El valor de estado no es válido.")
	@GetMapping("/resenas")
	public ResponseEntity<Object> resenas(@RequestParam(required = false) EstadoResena estado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(resenaService.listarTodas(estado,
				RespuestaUtil.pagina(pagina, tamanio, Sort.by("fecha").descending())));
	}

	/**
	 * Oculta una reseña con motivo (no se borra) y se le notifica a su autor.
	 * Ruta: {@code PATCH /admin/resenas/{id}/ocultar}.
	 *
	 * @param id reseña
	 * @param datos DTO con el {@code motivo}
	 * @param authentication administrador autenticado
	 * @return 200 con la reseña; 400 si falta el motivo; 404 si no existe
	 */
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

	/**
	 * Vuelve a mostrar una reseña oculta. Ruta:
	 * {@code PATCH /admin/resenas/{id}/mostrar}.
	 *
	 * @param id reseña
	 * @param authentication administrador autenticado
	 * @return 200 con la reseña; 404 si no existe
	 */
	@Operation(summary = "Volver a mostrar una reseña oculta")
	@ApiResponse(responseCode = "200", description = "Reseña PUBLICADA de nuevo")
	@ApiResponse(responseCode = "404", description = "La reseña no existe.")
	@PatchMapping("/resenas/{id}/mostrar")
	public ResponseEntity<Object> mostrarResena(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(resenaService.moderar(id, false, null, RespuestaUtil.idUsuario(authentication)));
	}

	// ----- tarifas -----

	/**
	 * Porcentajes globales vigentes de limpieza y servicio. Ruta:
	 * {@code GET /admin/tarifas}.
	 *
	 * @return 200 con los porcentajes vigentes
	 */
	@Operation(summary = "Porcentajes globales vigentes de limpieza y servicio")
	@ApiResponse(responseCode = "200", description = "Porcentajes vigentes de limpieza y servicio")
	@GetMapping("/tarifas")
	public ResponseEntity<Object> tarifas() {
		return ResponseEntity.ok(tarifaService.vigente());
	}

	/**
	 * Cambia los porcentajes globales; aplican a las cotizaciones nuevas.
	 * Ruta: {@code PUT /admin/tarifas}.
	 *
	 * @param datos porcentajes nuevos
	 * @param authentication administrador autenticado
	 * @return 200 con la configuración nueva; 400 si algún porcentaje falta o
	 *         no está entre 0 y 100
	 */
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

	/**
	 * Consulta la auditoría con filtros opcionales por acción, actor y fechas.
	 * Ruta: {@code GET /admin/auditoria}.
	 *
	 * @param accion acción exacta; opcional
	 * @param idActor usuario que hizo la acción; opcional
	 * @param desde fecha y hora mínima (ISO); opcional
	 * @param hasta fecha y hora máxima (ISO); opcional
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de eventos, del más reciente al más antiguo
	 */
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

	/**
	 * Detalle de un evento de auditoría. Ruta:
	 * {@code GET /admin/auditoria/{id}}.
	 *
	 * @param id evento
	 * @return 200 con el evento; 404 si no existe
	 */
	@Operation(summary = "Detalle de un evento de auditoría")
	@ApiResponse(responseCode = "200", description = "Evento de auditoría")
	@ApiResponse(responseCode = "404", description = "El evento de auditoría no existe.")
	@GetMapping("/auditoria/{id}")
	public ResponseEntity<Object> evento(@PathVariable Long id) {
		return RespuestaUtil.desde(auditoriaService.obtener(id));
	}
}
