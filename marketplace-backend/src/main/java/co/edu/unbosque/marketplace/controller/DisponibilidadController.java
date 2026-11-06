package co.edu.unbosque.marketplace.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.PeriodoDisponibilidadDTO;
import co.edu.unbosque.marketplace.service.CalendarioService;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de la disponibilidad de un alojamiento. Ruta base:
 * {@code /alojamientos/{idAlojamiento}/disponibilidad}. El calendario y la
 * consulta por fechas son públicos para los alojamientos ACTIVA; registrar,
 * modificar y eliminar períodos lo hacen el anfitrión dueño o un
 * administrador.
 */
@RestController
@RequestMapping("/alojamientos/{idAlojamiento}/disponibilidad")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Disponibilidad", description = "Períodos ofrecidos, fechas reservadas y consulta por fechas")
@CrossOrigin(origins = "*")
public class DisponibilidadController {

	/** Servicio del calendario. */
	private final CalendarioService calendarioService;

	/**
	 * Crea el controlador.
	 *
	 * @param calendarioService servicio del calendario
	 */
	public DisponibilidadController(CalendarioService calendarioService) {
		this.calendarioService = calendarioService;
	}

	/**
	 * Calendario: períodos disponibles y rangos reservados. Es público si el
	 * alojamiento está ACTIVA; en otro estado solo lo ven su anfitrión o un
	 * administrador. Ruta:
	 * {@code GET /alojamientos/{idAlojamiento}/disponibilidad}.
	 *
	 * @param idAlojamiento alojamiento
	 * @param authentication usuario autenticado; puede ser {@code null}
	 * @return 200 con el calendario; 404 si no existe o no se puede ver
	 */
	@Operation(summary = "Calendario: períodos disponibles y rangos reservados", description = "Público si el alojamiento está ACTIVA; en otro estado solo para su anfitrión o un administrador.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Períodos ofrecidos y fechas reservadas")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@GetMapping
	public ResponseEntity<Object> calendario(@PathVariable Long idAlojamiento, Authentication authentication) {
		return RespuestaUtil.desde(calendarioService.calendario(idAlojamiento, RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.esAdmin(authentication)));
	}

	/**
	 * Consulta si el alojamiento está disponible en unas fechas. Ruta pública:
	 * {@code GET /alojamientos/{idAlojamiento}/disponibilidad/consultar}.
	 *
	 * @param idAlojamiento alojamiento
	 * @param llegada día de llegada (yyyy-MM-dd)
	 * @param salida día de salida (yyyy-MM-dd)
	 * @return 200 con la respuesta y, si no está disponible, el motivo; 400 si
	 *         las fechas no son válidas; 404 si no existe
	 */
	@Operation(summary = "Consultar disponibilidad en unas fechas (público)", description = "La salida debe ser posterior a la llegada.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Si está disponible en esas fechas y, si no, el motivo")
	@ApiResponse(responseCode = "400", description = "Debes indicar la fecha de llegada y la de salida. | Las fechas "
			+ "son inválidas: la salida debe ser posterior a la llegada. | Las fechas son inválidas: la llegada no "
			+ "puede estar en el pasado.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@GetMapping("/consultar")
	public ResponseEntity<Object> consultar(@PathVariable Long idAlojamiento,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate llegada,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida) {
		return RespuestaUtil.desde(calendarioService.consultar(idAlojamiento, llegada, salida));
	}

	/**
	 * Registra un período disponible (dueño o administrador). Ruta:
	 * {@code POST /alojamientos/{idAlojamiento}/disponibilidad}.
	 *
	 * @param idAlojamiento alojamiento
	 * @param datos fechas del período
	 * @param authentication usuario autenticado
	 * @return 201 con el período; 400 si las fechas no son válidas; 403 si no
	 *         es el dueño; 404 si no existe; 409 si se cruza con otro período
	 */
	@Operation(summary = "Registrar período disponible (propietario o ADMIN)")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "201", description = "Período creado")
	@ApiResponse(responseCode = "400", description = "Debes indicar la fecha de llegada y la de salida. | Las fechas "
			+ "son inválidas: la salida debe ser posterior a la llegada. | Las fechas son inválidas: la llegada no "
			+ "puede estar en el pasado.")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "la disponibilidad.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "El período se cruza con otro período ya registrado; modifícalo "
			+ "en lugar de crear uno nuevo.")
	@PostMapping
	public ResponseEntity<Object> agregar(@PathVariable Long idAlojamiento, @RequestBody PeriodoDisponibilidadDTO datos,
			Authentication authentication) {
		return RespuestaUtil.desde(calendarioService.agregarPeriodo(idAlojamiento, datos,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)), HttpStatus.CREATED);
	}

	/**
	 * Modifica un período; no puede dejar por fuera reservas existentes. Ruta:
	 * {@code PUT /alojamientos/{idAlojamiento}/disponibilidad/{idPeriodo}}.
	 *
	 * @param idAlojamiento alojamiento
	 * @param idPeriodo período
	 * @param datos fechas nuevas
	 * @param authentication usuario autenticado
	 * @return 200 con el período; 400, 403, 404 o 409 según el caso
	 */
	@Operation(summary = "Modificar período (no puede afectar reservas existentes)")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "200", description = "Período actualizado")
	@ApiResponse(responseCode = "400", description = "Debes indicar la fecha de llegada y la de salida. | Las fechas "
			+ "son inválidas: la salida debe ser posterior a la llegada. | Las fechas son inválidas: la llegada no "
			+ "puede estar en el pasado.")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "la disponibilidad.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe. | El período no existe.")
	@ApiResponse(responseCode = "409", description = "El período se cruza con otro período ya registrado. | No puedes "
			+ "modificar el período: afecta la reserva del 2026-12-01 al 2026-12-05.")
	@PutMapping("/{idPeriodo}")
	public ResponseEntity<Object> actualizar(@PathVariable Long idAlojamiento, @PathVariable Long idPeriodo,
			@RequestBody PeriodoDisponibilidadDTO datos, Authentication authentication) {
		return RespuestaUtil.desde(calendarioService.actualizarPeriodo(idAlojamiento, idPeriodo, datos,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)));
	}

	/**
	 * Elimina un período sin reservas. Ruta:
	 * {@code DELETE /alojamientos/{idAlojamiento}/disponibilidad/{idPeriodo}}.
	 *
	 * @param idAlojamiento alojamiento
	 * @param idPeriodo período
	 * @param authentication usuario autenticado
	 * @return 200 si se eliminó; 403 si no es el dueño; 404 si no existe; 409
	 *         si tiene reservas activas
	 */
	@Operation(summary = "Eliminar período sin reservas")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "200", description = "Período eliminado.")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "la disponibilidad.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe. | El período no existe.")
	@ApiResponse(responseCode = "409", description = "No puedes eliminar el período: tiene reservas activas.")
	@DeleteMapping("/{idPeriodo}")
	public ResponseEntity<Object> eliminar(@PathVariable Long idAlojamiento, @PathVariable Long idPeriodo,
			Authentication authentication) {
		Resultado<Void> resultado = calendarioService.eliminarPeriodo(idAlojamiento, idPeriodo,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication));
		return resultado.esExito() ? RespuestaUtil.mensaje(HttpStatus.OK, "Período eliminado.")
				: RespuestaUtil.desde(resultado);
	}
}
