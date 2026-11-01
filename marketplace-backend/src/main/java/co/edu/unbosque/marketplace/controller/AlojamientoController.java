package co.edu.unbosque.marketplace.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.unbosque.marketplace.dto.AlojamientoDTO;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalDTO;
import co.edu.unbosque.marketplace.service.PublicacionService;
import co.edu.unbosque.marketplace.util.Resultado;
import co.edu.unbosque.marketplace.util.RespuestaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de alojamientos (publicaciones). Ruta base:
 * {@code /alojamientos}. La búsqueda, el detalle de publicaciones ACTIVA y las
 * políticas son públicos; lo demás requiere token.
 *
 * @see "RF-017, RF-018, RF-019, RF-024, RF-027"
 */
@RestController
@RequestMapping("/alojamientos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Alojamientos", description = "Publicaciones, ciclo de estados, servicios adicionales y búsqueda")
@CrossOrigin(origins = "*")
public class AlojamientoController {

	private static final Sort RECIENTES = Sort.by("fechaCreacion").descending();

	private final PublicacionService publicacionService;
	private final ObjectMapper objectMapper;

	public AlojamientoController(PublicacionService publicacionService, ObjectMapper objectMapper) {
		this.publicacionService = publicacionService;
		this.objectMapper = objectMapper;
	}

	/** Solo para Swagger: los dos campos del formulario de {@link #crear}. */
	@Schema(name = "FormularioAlojamiento")
	static final class FormularioAlojamiento {

		private AlojamientoDTO datos;
		private List<MultipartFile> archivos;

		@Schema(description = "JSON del alojamiento (solo el título es obligatorio en el borrador). El id, el "
				+ "estado y el anfitrión se ignoran.", requiredMode = Schema.RequiredMode.REQUIRED)
		public AlojamientoDTO getDatos() {
			return datos;
		}

		@ArraySchema(schema = @Schema(type = "string", format = "binary"), arraySchema = @Schema(description = "De 1 a "
				+ "15 imágenes JPG, PNG o WEBP de hasta 5 MB; la primera queda como principal"))
		public List<MultipartFile> getArchivos() {
			return archivos;
		}
	}

	@Operation(summary = "Catálogo: todos los alojamientos disponibles para reservar (público)", description = "Publicaciones ACTIVA con fechas abiertas desde hoy, para la página Explorar o el inicio. "
			+ "orden: recomendados (por defecto), precio_menor, precio_mayor o mejor_calificados.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Página de alojamientos que se pueden reservar")
	@ApiResponse(responseCode = "400", description = "El orden debe ser uno de: recomendados, precio_menor, "
			+ "precio_mayor, mejor_calificados.")
	@GetMapping("/disponibles")
	public ResponseEntity<Object> disponibles(@RequestParam(required = false) String orden,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
		return RespuestaUtil
				.desde(publicacionService.disponibles(orden, RespuestaUtil.pagina(pagina, tamanio, Sort.unsorted())));
	}

	@Operation(summary = "Buscar alojamientos ACTIVA (público)", description = "Filtros opcionales combinables: ciudad, fechas, huéspedes, tipo, rango de precio y "
			+ "amenidades. orden opcional como en /disponibles; sin él, los más recientes primero.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Página de alojamientos ACTIVA que cumplen los filtros")
	@ApiResponse(responseCode = "400", description = "Debes indicar la fecha de llegada y la de salida. | Las fechas "
			+ "son inválidas: la salida debe ser posterior a la llegada. | Las fechas son inválidas: la llegada no "
			+ "puede estar en el pasado. | La cantidad de huéspedes debe ser mayor que cero. | El precio mínimo no "
			+ "puede ser mayor que el máximo. | El orden debe ser uno de: recomendados, precio_menor, precio_mayor, "
			+ "mejor_calificados.")
	@GetMapping
	public ResponseEntity<Object> buscar(@RequestParam(required = false) String ciudad,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate llegada,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
			@RequestParam(required = false) Integer huespedes, @RequestParam(required = false) String tipo,
			@RequestParam(required = false) BigDecimal precioMinimo,
			@RequestParam(required = false) BigDecimal precioMaximo,
			@RequestParam(required = false) List<String> amenidades, @RequestParam(required = false) String orden,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "12") int tamanio) {
		return RespuestaUtil.desde(publicacionService.buscar(ciudad, llegada, salida, huespedes, tipo, precioMinimo,
				precioMaximo, amenidades, orden, RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	@Operation(summary = "Cantidad de alojamientos por tipo (público)", description = "Cuenta todos los alojamientos, en cualquier estado, agrupados por tipo: "
			+ "{\"Apartamento\": 5, \"Casa\": 7, ...}.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Cantidad de alojamientos de cada tipo")
	@GetMapping("/tipos")
	public ResponseEntity<Map<String, Long>> cantidadPorTipo() {
		return ResponseEntity.ok(publicacionService.cantidadPorTipo());
	}

	@Operation(summary = "Políticas de cancelación disponibles (público)")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Políticas de cancelación")
	@GetMapping("/politicas")
	public ResponseEntity<Object> politicas() {
		return ResponseEntity.ok(publicacionService.politicas());
	}

	@Operation(summary = "Mis alojamientos con su estado (ANFITRION)")
	@PreAuthorize("hasRole('ANFITRION')")
	@ApiResponse(responseCode = "200", description = "Página de alojamientos del anfitrión con su estado")
	@GetMapping("/mios")
	public ResponseEntity<Object> mios(Authentication authentication, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanio) {
		return ResponseEntity.ok(publicacionService.mios(RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.pagina(pagina, tamanio, RECIENTES)));
	}

	@Operation(summary = "Detalle de un alojamiento", description = "Público si está ACTIVA; en otro estado solo para su anfitrión o un administrador.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Alojamiento con imágenes, servicios, calificación y anfitrión")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@GetMapping("/{id}")
	public ResponseEntity<Object> detalle(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.obtener(id, RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.esAdmin(authentication)));
	}

	@Operation(summary = "Historial de estados (propietario o ADMIN)")
	@ApiResponse(responseCode = "200", description = "Cambios de estado, del primero al último")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@GetMapping("/{id}/historial")
	public ResponseEntity<Object> historial(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.historial(id, RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.esAdmin(authentication)));
	}

	@Operation(summary = "Crear alojamiento en BORRADOR con sus imágenes (ANFITRION)", description = "multipart/form-data con dos campos: 'datos', el JSON del alojamiento (solo el título es "
			+ "obligatorio en el borrador), y 'archivos', de 1 a 15 imágenes JPG, PNG o WEBP de hasta 5 MB. "
			+ "La primera imagen queda como principal.")
	@PreAuthorize("hasRole('ANFITRION')")
	@ApiResponse(responseCode = "201", description = "Alojamiento creado en BORRADOR con sus imágenes")
	@ApiResponse(responseCode = "400", description = "El título es obligatorio. | Debes enviar al menos una imagen. | "
			+ "Un alojamiento puede tener máximo 15 imágenes. | Solo se aceptan imágenes JPG, PNG o WEBP. "
			+ "(archivo.pdf) | El archivo está vacío. (foto.png) | El campo 'datos' debe ser el JSON del "
			+ "alojamiento: ... | La capacidad debe ser mayor que cero. | La política de cancelación no existe.")
	@ApiResponse(responseCode = "413", description = "El archivo supera el tamaño permitido (5 MB por imagen).")
	@ApiResponse(responseCode = "415", description = "El cuerpo debe enviarse como multipart/form-data.")
	@ApiResponse(responseCode = "503", description = "No se pudo guardar la imagen. Intenta de nuevo.")
	// Swagger muestra 'datos' como JSON del alojamiento; el método lo recibe como texto
	// porque el front lo envía en el FormData sin tipo de contenido
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = FormularioAlojamiento.class), encoding = @Encoding(name = "datos", contentType = MediaType.APPLICATION_JSON_VALUE)))
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> crear(@Parameter(hidden = true) @RequestPart("datos") String datos,
			@Parameter(hidden = true) @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos,
			Authentication authentication) {
		AlojamientoDTO alojamiento;
		try {
			alojamiento = objectMapper.readValue(datos, AlojamientoDTO.class);
		} catch (JsonProcessingException e) {
			return RespuestaUtil.mensaje(HttpStatus.BAD_REQUEST,
					"El campo 'datos' debe ser el JSON del alojamiento: " + e.getOriginalMessage());
		}
		return RespuestaUtil.desde(
				publicacionService.crear(alojamiento, archivos, RespuestaUtil.idUsuario(authentication)),
				HttpStatus.CREATED);
	}

	@Operation(summary = "Editar alojamiento (propietario o ADMIN)", description = "Los campos en null no cambian; las amenidades enviadas reemplazan las anteriores. Las "
			+ "imágenes se gestionan con los endpoints de /imagenes.")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "200", description = "Alojamiento actualizado")
	@ApiResponse(responseCode = "400", description = "La capacidad debe ser mayor que cero. | El precio por noche debe "
			+ "ser positivo. | Las habitaciones, las camas y los baños no pueden ser negativos. | La política de cancelación "
			+ "no existe.")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@PutMapping("/{id}")
	public ResponseEntity<Object> actualizar(@PathVariable Long id, @RequestBody AlojamientoDTO datos,
			Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.actualizar(id, datos, RespuestaUtil.idUsuario(authentication),
				RespuestaUtil.esAdmin(authentication)));
	}

	@Operation(summary = "Subir imágenes (propietario o ADMIN)", description = "multipart/form-data con uno o varios archivos en el campo 'archivos'. JPG, PNG o WEBP de "
			+ "hasta 5 MB, máximo 15 por alojamiento. Si no había portada, la primera queda como principal.")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "201", description = "Todas las imágenes del alojamiento, la principal primero")
	@ApiResponse(responseCode = "400", description = "Debes enviar al menos una imagen. | Un alojamiento puede tener "
			+ "máximo 15 imágenes. | Solo se aceptan imágenes JPG, PNG o WEBP. (archivo.pdf) | El archivo está "
			+ "vacío. (foto.png)")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "413", description = "El archivo supera el tamaño permitido (5 MB por imagen).")
	@ApiResponse(responseCode = "415", description = "El cuerpo debe enviarse como multipart/form-data.")
	@ApiResponse(responseCode = "503", description = "No se pudo guardar la imagen. Intenta de nuevo.")
	@PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> subirImagenes(@PathVariable Long id,
			@RequestParam("archivos") List<MultipartFile> archivos, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.subirImagenes(id, archivos,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)), HttpStatus.CREATED);
	}

	@Operation(summary = "Eliminar una imagen (propietario o ADMIN)", description = "Si era la principal, la siguiente pasa a serlo. El alojamiento debe conservar al menos "
			+ "una imagen (409 si es la última).")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "200", description = "Imágenes restantes, la principal primero")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe. | La imagen no existe.")
	@ApiResponse(responseCode = "409", description = "El alojamiento debe conservar al menos una imagen. Sube otra "
			+ "antes de borrar esta.")
	@DeleteMapping("/{id}/imagenes/{idImagen}")
	public ResponseEntity<Object> eliminarImagen(@PathVariable Long id, @PathVariable Long idImagen,
			Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.eliminarImagen(id, idImagen,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)));
	}

	@Operation(summary = "Marcar una imagen como principal (propietario o ADMIN)")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "200", description = "Imágenes, la principal primero")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe. | La imagen no existe.")
	@PatchMapping("/{id}/imagenes/{idImagen}/principal")
	public ResponseEntity<Object> marcarPrincipal(@PathVariable Long id, @PathVariable Long idImagen,
			Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.marcarPrincipal(id, idImagen,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)));
	}

	@Operation(summary = "Enviar a revisión: BORRADOR → PENDIENTE_REVISION (ANFITRION)")
	@PreAuthorize("hasRole('ANFITRION')")
	@ApiResponse(responseCode = "200", description = "Alojamiento PENDIENTE_REVISION")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "Completa los datos antes de enviar a revisión. Falta: "
			+ "descripción, capacidad. | No se puede pasar de ACTIVA a PENDIENTE_REVISION.")
	@PostMapping("/{id}/enviarrevision")
	public ResponseEntity<Object> enviarRevision(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.enviarRevision(id, RespuestaUtil.idUsuario(authentication)));
	}

	@Operation(summary = "Pausar: ACTIVA → PAUSADA (ANFITRION)")
	@PreAuthorize("hasRole('ANFITRION')")
	@ApiResponse(responseCode = "200", description = "Alojamiento PAUSADA: no recibe reservas nuevas")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de BORRADOR a PAUSADA.")
	@PostMapping("/{id}/pausar")
	public ResponseEntity<Object> pausar(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.pausar(id, RespuestaUtil.idUsuario(authentication)));
	}

	@Operation(summary = "Reanudar: PAUSADA → ACTIVA (ANFITRION)")
	@PreAuthorize("hasRole('ANFITRION')")
	@ApiResponse(responseCode = "200", description = "Alojamiento ACTIVA")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@ApiResponse(responseCode = "409", description = "No se puede pasar de BLOQUEADA a ACTIVA.")
	@PostMapping("/{id}/reanudar")
	public ResponseEntity<Object> reanudar(@PathVariable Long id, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.reanudar(id, RespuestaUtil.idUsuario(authentication)));
	}

	@Operation(summary = "Agregar servicio adicional con costo (propietario o ADMIN)")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "201", description = "Servicio adicional creado")
	@ApiResponse(responseCode = "400", description = "El servicio adicional necesita un nombre y un costo positivo.")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe.")
	@PostMapping("/{id}/servicios")
	public ResponseEntity<Object> agregarServicio(@PathVariable Long id, @RequestBody ServicioAdicionalDTO datos,
			Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.agregarServicio(id, datos,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)), HttpStatus.CREATED);
	}

	@Operation(summary = "Modificar servicio adicional (propietario o ADMIN)")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "200", description = "Servicio adicional actualizado")
	@ApiResponse(responseCode = "400", description = "El servicio adicional necesita un nombre y un costo positivo.")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe. | El servicio adicional no existe.")
	@PutMapping("/{id}/servicios/{idServicio}")
	public ResponseEntity<Object> actualizarServicio(@PathVariable Long id, @PathVariable Long idServicio,
			@RequestBody ServicioAdicionalDTO datos, Authentication authentication) {
		return RespuestaUtil.desde(publicacionService.actualizarServicio(id, idServicio, datos,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication)));
	}

	@Operation(summary = "Eliminar servicio adicional (propietario o ADMIN)")
	@PreAuthorize("hasAnyRole('ANFITRION','ADMIN')")
	@ApiResponse(responseCode = "200", description = "Servicio adicional eliminado.")
	@ApiResponse(responseCode = "403", description = "Solo el anfitrión propietario o un administrador puede modificar "
			+ "este alojamiento.")
	@ApiResponse(responseCode = "404", description = "El alojamiento no existe. | El servicio adicional no existe.")
	@DeleteMapping("/{id}/servicios/{idServicio}")
	public ResponseEntity<Object> eliminarServicio(@PathVariable Long id, @PathVariable Long idServicio,
			Authentication authentication) {
		Resultado<Void> resultado = publicacionService.eliminarServicio(id, idServicio,
				RespuestaUtil.idUsuario(authentication), RespuestaUtil.esAdmin(authentication));
		return resultado.esExito() ? RespuestaUtil.mensaje(HttpStatus.OK, "Servicio adicional eliminado.")
				: RespuestaUtil.desde(resultado);
	}
}
