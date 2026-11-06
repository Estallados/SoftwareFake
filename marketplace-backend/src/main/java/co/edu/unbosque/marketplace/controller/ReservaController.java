package co.edu.unbosque.marketplace.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.dto.CotizacionDTO;
import co.edu.unbosque.marketplace.dto.MotivoDTO;
import co.edu.unbosque.marketplace.dto.SolicitudReservaDTO;
import co.edu.unbosque.marketplace.service.CancelacionService;
import co.edu.unbosque.marketplace.service.CotizacionService;
import co.edu.unbosque.marketplace.service.IdempotenciaService;
import co.edu.unbosque.marketplace.service.ReservaService;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de cotizaciones, reservas y cancelaciones. Crear una
 * reserva y cancelarla son operaciones idempotentes con la cabecera
 * {@code Idempotency-Key}: si el cliente repite la petición, no se reserva ni
 * se cobra dos veces.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cotización y reservas", description = "Cotizar, reservar con pago de prueba, consultar y cancelar")
@CrossOrigin(origins = "*")
public class ReservaController {

	/** Orden de las reservas: las más recientes primero. */
	private static final Sort RECIENTES = Sort.by("fechaCreacion").descending();

	/** Servicio de cotizaciones. */
	private final CotizacionService cotizacionService;

	/** Servicio de reservas. */
	private final ReservaService reservaService;

	/** Servicio de cancelaciones. */
	private final CancelacionService cancelacionService;

	/** Aplica la cabecera Idempotency-Key. */
	private final IdempotenciaService idempotenciaService;

	/**
	 * Crea el controlador.
	 *
	 * @param cotizacionService servicio de cotizaciones
	 * @param reservaService servicio de reservas
	 * @param cancelacionService servicio de cancelaciones
	 * @param idempotenciaService servicio de idempotencia
	 */
	public ReservaController(CotizacionService cotizacionService, ReservaService reservaService,
			CancelacionService cancelacionService, IdempotenciaService idempotenciaService) {
		this.cotizacionService = cotizacionService;
		this.reservaService = reservaService;
		this.cancelacionService = cancelacionService;
		this.idempotenciaService = idempotenciaService;
	}

	/**
	 * Cotiza una estancia: noches, precio base, descuento, limpieza, servicio,
	 * adicionales y total. La cotización sirve 30 minutos para reservar. Ruta:
	 * {@code POST /cotizaciones}.
	 *
	 * @param solicitud alojamiento, fechas, huéspedes y servicios elegidos
	 * @param authentication usuario autenticado
	 * @return 201 con la cotización; 400 si los datos no son válidos; 404 si el
	 *         alojamiento no existe; 409 si no está publicado o las fechas no
	 *         están libres
	 */
	@Operation(summary = "Cotizar una estancia", description = "Devuelve noches, precio base, descuento, limpieza, servicio, adicionales y total. "
			+ "La cotización sirve 30 minutos para reservar.")
	@ApiResponse(responseCode = "201", description = "Cotización con el desglose; sirve 30 minutos para reservar")
	@ApiResponse(responseCode = "400", description = "Debes indicar el alojamiento a cotizar. | Debes indicar la fecha "
			+ "de llegada y la de salida. | Las fechas son inválidas: la salida debe ser posterior a la llegada. | "
			+ "Las fechas son inválidas: la llegada no puede estar en el pasado. | La cantidad de huéspedes (9) "
			+ "supera la capacidad del alojamiento (4). | El servicio adicional 7 no pertenece a este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "El alojamiento no está disponible para reservas (estado "
			+ "PAUSADA). | El alojamiento no está disponible en esas fechas.")
	@PostMapping("/cotizaciones")
	public ResponseEntity<Object> cotizar(@RequestBody CotizacionDTO solicitud, Authentication authentication) {
		return RespuestaUtil.desde(cotizacionService.cotizar(solicitud, RespuestaUtil.idUsuario(authentication)),
				HttpStatus.CREATED);
	}

	/**
	 * Crea y paga una reserva (solo huéspedes). Exige la cabecera
	 * Idempotency-Key. Ruta: {@code POST /reservas}.
	 *
	 * @param solicitud cotización y método de pago de prueba
	 * @param clave valor de la cabecera Idempotency-Key
	 * @param authentication huésped autenticado
	 * @return 201 con la reserva CONFIRMADA y su pago; 400, 402 (pago
	 *         rechazado), 403, 404, 409 o 502 (pago no confirmado) según el
	 *         caso
	 */
	@Operation(summary = "Crear y pagar una reserva (HUESPED)", description = "Requiere la cabecera Idempotency-Key. metodoPago es un PaymentMethod de Stripe: uno de "
			+ "prueba (pm_card_visa aprueba, pm_card_chargeDeclinedInsufficientFunds rechaza; la lista está "
			+ "en GET /pagos/configuracion) o el que crea Stripe Elements con una tarjeta de prueba. Con "
			+ "STRIPE_SECRET_KEY el cobro va al modo de prueba de Stripe; sin ella, al simulador.")
	@PreAuthorize("hasRole('HUESPED')")
	@ApiResponse(responseCode = "201", description = "Reserva CONFIRMADA y pagada (incluye el pago)")
	@ApiResponse(responseCode = "400", description = "Debes indicar la cotización a reservar. | El método de pago debe "
			+ "ser un PaymentMethod de Stripe (pm_...), por ejemplo pm_card_visa. | La cabecera Idempotency-Key es "
			+ "obligatoria y debe tener entre 1 y 100 caracteres.")
	@ApiResponse(responseCode = "402", description = "El pago fue rechazado: La tarjeta no tiene fondos suficientes. "
			+ "La reserva no quedó pagada.")
	@ApiResponse(responseCode = "403", description = "No puedes reservar tu propio alojamiento.")
	@ApiResponse(responseCode = "404", description = "La cotización no existe.")
	@ApiResponse(responseCode = "409", description = "La cotización venció o ya se usó; solicita una cotización nueva. "
			+ "| El período ya no está disponible: otra reserva lo tomó. | Esta Idempotency-Key ya se usó con una "
			+ "petición diferente. | La petición con esta Idempotency-Key todavía está en proceso.")
	@ApiResponse(responseCode = "502", description = "No se pudo confirmar el pago: No se pudo comunicar con la "
			+ "pasarela de pago. La reserva no quedó pagada.")
	@PostMapping("/reservas")
	public ResponseEntity<Object> crear(@RequestBody SolicitudReservaDTO solicitud,
			@RequestHeader(value = IdempotenciaService.CABECERA, required = false) String clave,
			Authentication authentication) {
		Long idHuesped = RespuestaUtil.idUsuario(authentication);
		return idempotenciaService.ejecutar(idHuesped, "CREAR_RESERVA", clave, solicitud,
				() -> RespuestaUtil.desde(reservaService.crear(solicitud, idHuesped), HttpStatus.CREATED));
	}

	/**
	 * Mis reservas como huésped. Ruta: {@code GET /reservas/mias}.
	 *
	 * @param authentication huésped autenticado
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de reservas
	 */
	@Operation(summary = "Mis reservas como huésped")
	@ApiResponse(responseCode = "200", description = "Página de reservas del huésped")
	@GetMapping("/reservas/mias")
	public ResponseEntity<Object> mias(Authentication authentication, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(reservaService.listarMias(RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	/**
	 * Reservas de mis alojamientos (solo anfitriones). Ruta:
	 * {@code GET /reservas/anfitrion}.
	 *
	 * @param authentication anfitrión autenticado
	 * @param pagina número de página (desde 0)
	 * @param tamanio elementos por página
	 * @return 200 con la página de reservas
	 */
	@Operation(summary = "Reservas de mis alojamientos (ANFITRION)")
	@PreAuthorize("hasRole('ANFITRION')")
	@ApiResponse(responseCode = "200", description = "Página de reservas de los alojamientos del anfitrión")
	@GetMapping("/reservas/anfitrion")
	public ResponseEntity<Object> deAnfitrion(Authentication authentication,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(reservaService.listarDeAnfitrion(RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	/**
	 * Detalle de una reserva; lo ven el huésped, el anfitrión del alojamiento
	 * y los administradores. Ruta: {@code GET /reservas/{id}}.
	 *
	 * @param id reserva
	 * @param authentication usuario autenticado
	 * @return 200 con la reserva, su pago, el alojamiento y la cancelación; 404
	 *         si no existe o no se puede ver
	 */
	@Operation(summary = "Detalle de una reserva (huésped, anfitrión o ADMIN)")
	@ApiResponse(responseCode = "200", description = "Reserva con pago, alojamiento y cancelación")
	@ApiResponse(responseCode = "404", description = "La reserva no existe.")
	@GetMapping("/reservas/{id}")
	public ResponseEntity<Object> detalle(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(reservaService.obtener(id, RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.esAdmin(authentication)));
	}

	/**
	 * Simula la cancelación de una reserva (solo huéspedes): devuelve la
	 * política, los días de anticipación, el porcentaje y el valor que se
	 * devolvería si se cancelara hoy. No cancela nada. Ruta:
	 * {@code GET /reservas/{id}/simularcancelacion}.
	 *
	 * @param id reserva
	 * @param authentication huésped autenticado
	 * @return 200 con la estimación; 404 si no existe; 409 si no se puede
	 *         cancelar
	 */
	@Operation(summary = "Simular la cancelación de una reserva (HUESPED)", description = "Devuelve la política, los días de anticipación, el porcentaje y el valor que se "
			+ "devolvería si se cancelara hoy. No cancela nada.")
	@PreAuthorize("hasRole('HUESPED')")
	@ApiResponse(responseCode = "200", description = "Devolución que recibiría si cancela hoy")
	@ApiResponse(responseCode = "404", description = "La reserva no existe.")
	@ApiResponse(responseCode = "409", description = "La reserva no se puede cancelar en estado CANCELADA. | Solo se "
			+ "pueden cancelar reservas futuras.")
	@GetMapping("/reservas/{id}/simularcancelacion")
	public ResponseEntity<Object> simularCancelacion(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(cancelacionService.simular(id, RespuestaUtil.idUsuario(authentication)));
	}

	/**
	 * Cancela una reserva futura (solo huéspedes), aplicando la política de
	 * cancelación del alojamiento. Exige la cabecera Idempotency-Key y un
	 * motivo. Ruta: {@code POST /reservas/{id}/cancelar}.
	 *
	 * @param id reserva
	 * @param datos DTO con el {@code motivo}
	 * @param clave valor de la cabecera Idempotency-Key
	 * @param authentication huésped autenticado
	 * @return 200 con la cancelación y la devolución; 400, 404 o 409 según el
	 *         caso
	 */
	@Operation(summary = "Cancelar una reserva futura (HUESPED)", description = "Aplica la política de cancelación del alojamiento. Requiere Idempotency-Key y motivo.")
	@PreAuthorize("hasRole('HUESPED')")
	@ApiResponse(responseCode = "200", description = "Cancelación con la devolución según la política")
	@ApiResponse(responseCode = "400", description = "El motivo de la cancelación es obligatorio. | La cabecera "
			+ "Idempotency-Key es obligatoria y debe tener entre 1 y 100 caracteres.")
	@ApiResponse(responseCode = "404", description = "La reserva no existe.")
	@ApiResponse(responseCode = "409", description = "La reserva no se puede cancelar en estado CANCELADA. | Solo se "
			+ "pueden cancelar reservas futuras. | Esta Idempotency-Key ya se usó con una petición diferente. | La "
			+ "petición con esta Idempotency-Key todavía está en proceso.")
	@PostMapping("/reservas/{id}/cancelar")
	public ResponseEntity<Object> cancelar(@PathVariable Long id, @RequestBody MotivoDTO datos,
			@RequestHeader(value = IdempotenciaService.CABECERA, required = false) String clave,
			Authentication authentication) {
		Long idHuesped = RespuestaUtil.idUsuario(authentication);
		return idempotenciaService.ejecutar(idHuesped, "CANCELAR_RESERVA", clave,
				new Object[] { id, datos.getMotivo() },
				() -> RespuestaUtil.desde(cancelacionService.cancelar(id, datos.getMotivo(), idHuesped)));
	}
}
