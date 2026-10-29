package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.edu.unbosque.marketplace.dto.AlojamientoDTO;
import co.edu.unbosque.marketplace.dto.HistorialEstadoAlojamientoDTO;
import co.edu.unbosque.marketplace.dto.ImagenDTO;
import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.dto.PoliticaCancelacionDTO;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoResena;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.HistorialEstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.Imagen;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.ServicioAdicional;
import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.AlojamientoEspecificacion;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.HistorialEstadoAlojamientoRepository;
import co.edu.unbosque.marketplace.repository.ImagenRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Publicaciones de alojamiento: creación, edición, imágenes, ciclo de estados,
 * servicios adicionales y búsqueda pública.
 * <p>
 * Transiciones permitidas (cualquier otra se rechaza y queda en la auditoría):
 * </p>
 * <ul>
 * <li>BORRADOR → PENDIENTE_REVISION: anfitrión, con la información mínima completa.</li>
 * <li>PENDIENTE_REVISION → ACTIVA: administrador aprueba.</li>
 * <li>PENDIENTE_REVISION → BORRADOR: administrador rechaza con motivo (se notifica).</li>
 * <li>ACTIVA → PAUSADA y PAUSADA → ACTIVA: anfitrión.</li>
 * <li>cualquiera → BLOQUEADA: administrador con motivo; las reservas futuras pasan a EN_REVISION.</li>
 * <li>BLOQUEADA → PAUSADA: administrador.</li>
 * </ul>
 * Cada cambio guarda {@link HistorialEstadoAlojamiento} y auditoría.
 *
 * @see "RF-013, RF-017, RF-018, RF-019, RF-024, RF-027, RNF-005"
 */
@Service
public class PublicacionService {

    private static final String NO_EXISTE = "El alojamiento no existe.";
    private static final String SIN_PERMISO = "Solo el anfitrión propietario o un administrador puede modificar "
            + "este alojamiento.";
    private static final int LATITUD_MAXIMA = 90;
    private static final int LONGITUD_MAXIMA = 180;
    /** Máximo de imágenes por alojamiento. */
    public static final int MAXIMO_IMAGENES = 15;
    private static final List<EstadoReserva> RESERVAS_VIGENTES = List.of(EstadoReserva.CONFIRMADA,
            EstadoReserva.EN_REVISION);

    /** Órdenes del catálogo y la búsqueda; son los de la página Explorar. */
    public static final String ORDEN_RECOMENDADOS = "recomendados";
    public static final String ORDEN_PRECIO_MENOR = "precio_menor";
    public static final String ORDEN_PRECIO_MAYOR = "precio_mayor";
    public static final String ORDEN_MEJOR_CALIFICADOS = "mejor_calificados";
    private static final String ORDENES = String.join(", ", ORDEN_RECOMENDADOS, ORDEN_PRECIO_MENOR,
            ORDEN_PRECIO_MAYOR, ORDEN_MEJOR_CALIFICADOS);
    private static final String PRECIO_NOCHE = "precioNoche";

    private final AlojamientoRepository alojamientoRepo;
    private final ImagenRepository imagenRepo;
    private final ServicioAdicionalRepository servicioRepo;
    private final PoliticaCancelacionRepository politicaRepo;
    private final HistorialEstadoAlojamientoRepository historialRepo;
    private final ReservaRepository reservaRepo;
    private final ResenaRepository resenaRepo;
    private final UsuarioRepository usuarioRepo;
    private final AlmacenamientoService almacenamientoService;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;
    private final ModelMapper modelMapper;

    public PublicacionService(AlojamientoRepository alojamientoRepo, ImagenRepository imagenRepo,
            ServicioAdicionalRepository servicioRepo, PoliticaCancelacionRepository politicaRepo,
            HistorialEstadoAlojamientoRepository historialRepo, ReservaRepository reservaRepo,
            ResenaRepository resenaRepo, UsuarioRepository usuarioRepo, AlmacenamientoService almacenamientoService,
            AuditoriaService auditoriaService, NotificacionService notificacionService, ModelMapper modelMapper) {
        this.alojamientoRepo = alojamientoRepo;
        this.imagenRepo = imagenRepo;
        this.servicioRepo = servicioRepo;
        this.politicaRepo = politicaRepo;
        this.historialRepo = historialRepo;
        this.reservaRepo = reservaRepo;
        this.resenaRepo = resenaRepo;
        this.usuarioRepo = usuarioRepo;
        this.almacenamientoService = almacenamientoService;
        this.auditoriaService = auditoriaService;
        this.notificacionService = notificacionService;
        this.modelMapper = modelMapper;
    }



    /**
     * Cantidad de alojamientos de cada tipo, en todos los estados: {"Apartamento": 5,
     * "Casa": 7, ...}, en orden alfabético.
     */
    public Map<String, Long> cantidadPorTipo() {
        Map<String, Long> cantidades = new LinkedHashMap<>();
        for (Object[] fila : alojamientoRepo.contarPorTipo()) {
            cantidades.put((String) fila[0], (Long) fila[1]);
        }
        return cantidades;
    }

    /** Políticas de cancelación disponibles para asignar a un alojamiento. */
    public List<PoliticaCancelacionDTO> politicas() {
        return politicaRepo.findAll().stream().map(p -> modelMapper.map(p, PoliticaCancelacionDTO.class)).toList();
    }

    /**
     * Detalle de un alojamiento. Si está ACTIVA lo ve cualquiera; en otro
     * estado solo su anfitrión o un administrador.
     *
     * @return 0 con el alojamiento; 1 si no existe o no es visible.
     * @see "RF-019, CA-093, CA-102"
     */
    public Resultado<AlojamientoDTO> obtener(Long idAlojamiento, Long idActor, boolean esAdmin) {
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty() || !esVisible(alojamiento.get(), idActor, esAdmin)) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        return Resultado.exito(esAdmin ? convertirDTOAdmin(alojamiento.get()) : convertirDTO(alojamiento.get()));
    }

    /**
     * Publicaciones del anfitrión con el estado de cada una.
     *
     * @see "RF-019, CA-103"
     */
    public PaginaDTO<AlojamientoDTO> mios(Long idAnfitrion, Pageable pagina) {
        return PaginaDTO.de(alojamientoRepo.findByIdAnfitrion(idAnfitrion, pagina), this::convertirDTO);
    }

    /** Publicaciones en un estado, o todas si es null (vista del administrador). */
    public PaginaDTO<AlojamientoDTO> listarPorEstado(EstadoAlojamiento estado, Pageable pagina) {
        return PaginaDTO.de(estado == null ? alojamientoRepo.findAll(pagina)
                : alojamientoRepo.findByEstado(estado, pagina), this::convertirDTOAdmin);
    }

    /**
     * Publicaciones de un anfitrión en cualquier estado (detalle de usuario del
     * administrador).
     */
    public PaginaDTO<AlojamientoDTO> listarDeAnfitrion(Long idAnfitrion, Pageable pagina) {
        return PaginaDTO.de(alojamientoRepo.findByIdAnfitrion(idAnfitrion, pagina), this::convertirDTOAdmin);
    }

    /**
     * Catálogo público: todos los alojamientos que un visitante puede
     * reservar, es decir, publicaciones ACTIVA con al menos una noche ofrecida
     * desde hoy. Sin filtros; para filtrar se usa {@link #buscar}.
     *
     * @param orden {@link #ORDEN_RECOMENDADOS} (por defecto: más reseñas y
     *              mejor promedio), {@link #ORDEN_PRECIO_MENOR},
     *              {@link #ORDEN_PRECIO_MAYOR} o {@link #ORDEN_MEJOR_CALIFICADOS}.
     * @return 0 con la página de alojamientos; 2 si el orden no existe.
     * @see "RF-019, RF-024, CA-087"
     */
    public Resultado<PaginaDTO<AlojamientoDTO>> disponibles(String orden, Pageable pagina) {
        return listarOrdenado(AlojamientoEspecificacion.conEstado(EstadoAlojamiento.ACTIVA)
                .and(AlojamientoEspecificacion.conFechasDesde(LocalDate.now())),
                orden == null || orden.isBlank() ? ORDEN_RECOMENDADOS : orden, pagina);
    }

    /**
     * Búsqueda pública: solo publicaciones ACTIVA que cumplan todos los filtros
     * enviados. Con fechas, excluye las que no estén libres en ese período.
     * Sin orden, las más recientes primero.
     *
     * @param orden Opcional; los mismos valores de {@link #disponibles}.
     * @return 0 con la página de resultados; 2 si los filtros o el orden no son
     *         válidos.
     * @see "RF-024, CA-087 a CA-092, CA-055, CA-077, CA-190"
     */
    public Resultado<PaginaDTO<AlojamientoDTO>> buscar(String ciudad, LocalDate llegada, LocalDate salida,
            Integer huespedes, String tipo, BigDecimal precioMinimo, BigDecimal precioMaximo,
            Collection<String> amenidades, String orden, Pageable pagina) {
        if (llegada != null || salida != null) {
            String errorFechas = CalendarioService.validarFechas(llegada, salida);
            if (errorFechas != null) {
                return Resultado.error(Resultado.DATOS_INVALIDOS, errorFechas);
            }
        }
        if (huespedes != null && huespedes <= 0) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "La cantidad de huéspedes debe ser mayor que cero.");
        }
        if (precioMinimo != null && precioMaximo != null && precioMinimo.compareTo(precioMaximo) > 0) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "El precio mínimo no puede ser mayor que el máximo.");
        }

        Specification<Alojamiento> filtro = AlojamientoEspecificacion.conEstado(EstadoAlojamiento.ACTIVA)
                .and(AlojamientoEspecificacion.enCiudad(ciudad))
                .and(AlojamientoEspecificacion.deTipo(tipo))
                .and(AlojamientoEspecificacion.conCapacidadPara(huespedes))
                .and(AlojamientoEspecificacion.precioDesde(precioMinimo))
                .and(AlojamientoEspecificacion.precioHasta(precioMaximo))
                .and(AlojamientoEspecificacion.conAmenidades(amenidades))
                .and(AlojamientoEspecificacion.libreEntre(llegada, salida, CalendarioService.ESTADOS_OCUPAN,
                        CalendarioService.limitePendiente()));
        if (orden != null && !orden.isBlank()) {
            return listarOrdenado(filtro, orden, pagina);
        }
        return Resultado.exito(PaginaDTO.de(alojamientoRepo.findAll(filtro, pagina), this::convertirDTO));
    }

    /**
     * Aplica uno de los órdenes del catálogo. El precio lo ordena la base de
     * datos con la página; las reseñas, con subconsultas de
     * {@link AlojamientoEspecificacion#ordenadoPorResenas}.
     */
    private Resultado<PaginaDTO<AlojamientoDTO>> listarOrdenado(Specification<Alojamiento> filtro, String orden,
            Pageable pagina) {
        Pageable sinOrden = PageRequest.of(pagina.getPageNumber(), pagina.getPageSize());
        Specification<Alojamiento> consulta;
        Pageable paginaOrdenada;
        switch (orden.trim().toLowerCase()) {
        case ORDEN_PRECIO_MENOR -> {
            consulta = filtro;
            paginaOrdenada = PageRequest.of(pagina.getPageNumber(), pagina.getPageSize(),
                    Sort.by(PRECIO_NOCHE).ascending().and(Sort.by("idAlojamiento")));
        }
        case ORDEN_PRECIO_MAYOR -> {
            consulta = filtro;
            paginaOrdenada = PageRequest.of(pagina.getPageNumber(), pagina.getPageSize(),
                    Sort.by(PRECIO_NOCHE).descending().and(Sort.by("idAlojamiento")));
        }
        case ORDEN_RECOMENDADOS -> {
            consulta = filtro.and(AlojamientoEspecificacion.ordenadoPorResenas(true));
            paginaOrdenada = sinOrden;
        }
        case ORDEN_MEJOR_CALIFICADOS -> {
            consulta = filtro.and(AlojamientoEspecificacion.ordenadoPorResenas(false));
            paginaOrdenada = sinOrden;
        }
        default -> {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "El orden debe ser uno de: " + ORDENES + ".");
        }
        }
        return Resultado.exito(PaginaDTO.de(alojamientoRepo.findAll(consulta, paginaOrdenada), this::convertirDTO));
    }

    /**
     * Historial de estados de un alojamiento (propietario o administrador).
     *
     * @see "RNF-026, CA-153"
     */
    public Resultado<List<HistorialEstadoAlojamientoDTO>> historial(Long idAlojamiento, Long idActor,
            boolean esAdmin) {
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty() || !(esAdmin || esPropietario(alojamiento.get(), idActor))) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        return Resultado.exito(historialRepo.findByIdAlojamientoOrderByFechaAscIdHistorialAsc(idAlojamiento).stream()
                .map(h -> modelMapper.map(h, HistorialEstadoAlojamientoDTO.class)).toList());
    }

    // ----- creación y edición -----

    /**
     * Crea una publicación en BORRADOR para el anfitrión autenticado con al
     * menos una imagen: el borrador puede guardarse con otros datos
     * incompletos, pero nunca sin imagen, la misma regla que se exige al
     * enviarlo a revisión. La primera imagen queda como principal. El id, el
     * estado y el propietario enviados se ignoran.
     *
     * @param archivos Imágenes (JPG, PNG o WEBP de hasta 5 MB), entre 1 y
     *                 {@link #MAXIMO_IMAGENES}.
     * @return 0 con la publicación; 2 si los datos o las imágenes no son válidos.
     * @see "RF-017, CA-094, CA-095, CA-096, CA-098, CA-099"
     */
    @Transactional
    public Resultado<AlojamientoDTO> crear(AlojamientoDTO datos, List<MultipartFile> archivos, Long idAnfitrion) {
        String error = validar(datos, true);
        if (error == null) {
            error = errorArchivos(archivos, 0);
        }
        if (error != null) {
            auditoriaService.registrar(idAnfitrion, AuditoriaService.ALOJAMIENTO_CREADO,
                    AuditoriaService.ENTIDAD_ALOJAMIENTO, null, ResultadoAuditoria.RECHAZADO, error);
            return Resultado.error(Resultado.DATOS_INVALIDOS, error);
        }

        Alojamiento alojamiento = new Alojamiento();
        aplicar(datos, alojamiento);
        alojamiento.setIdAnfitrion(idAnfitrion);
        alojamiento.setEstado(EstadoAlojamiento.BORRADOR);
        alojamiento.setFechaCreacion(LocalDateTime.now());
        alojamiento.setFechaActualizacion(alojamiento.getFechaCreacion());
        alojamiento = alojamientoRepo.save(alojamiento);
        guardarArchivos(alojamiento.getIdAlojamiento(), archivos, false);
        registrarHistorial(alojamiento.getIdAlojamiento(), null, EstadoAlojamiento.BORRADOR, idAnfitrion, null);

        auditoriaService.registrar(idAnfitrion, AuditoriaService.ALOJAMIENTO_CREADO,
                AuditoriaService.ENTIDAD_ALOJAMIENTO, alojamiento.getIdAlojamiento(), ResultadoAuditoria.EXITOSO,
                null);
        return Resultado.exito(convertirDTO(alojamiento));
    }

    /**
     * Edita una publicación (propietario o administrador). Los campos en null
     * se dejan como están; las amenidades enviadas reemplazan a las
     * anteriores. El estado, el propietario y las imágenes no se cambian por
     * esta vía: las imágenes se gestionan con {@link #subirImagenes},
     * {@link #eliminarImagen} y {@link #marcarPrincipal}.
     *
     * @return 0 con la publicación; 1 no existe; 2 datos inválidos; 3 sin permiso.
     * @see "RF-018, CA-100, CA-101, CA-113, CA-114"
     */
    @Transactional
    public Resultado<AlojamientoDTO> actualizar(Long idAlojamiento, AlojamientoDTO datos, Long idActor,
            boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin,
                AuditoriaService.ALOJAMIENTO_ACTUALIZADO);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        String error = validar(datos, false);
        if (error != null) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, error);
        }

        Alojamiento alojamiento = carga.getDato();
        aplicar(datos, alojamiento);
        alojamiento.setFechaActualizacion(LocalDateTime.now());
        alojamiento = alojamientoRepo.save(alojamiento);

        auditoriaService.registrar(idActor, AuditoriaService.ALOJAMIENTO_ACTUALIZADO,
                AuditoriaService.ENTIDAD_ALOJAMIENTO, idAlojamiento, ResultadoAuditoria.EXITOSO, null);
        return Resultado.exito(convertirDTO(alojamiento));
    }

    // ----- imágenes -----

    /**
     * Sube una o varias imágenes (JPG, PNG o WEBP de hasta 5 MB) al
     * alojamiento. Se validan todas antes de guardar alguna. Si el alojamiento
     * no tenía portada, la primera imagen subida queda como principal.
     *
     * @return 0 con todas las imágenes del alojamiento (la principal primero);
     *         1 no existe; 2 archivos inválidos o se supera el máximo; 3 sin
     *         permiso.
     * @see "RF-017, RF-018"
     */
    @Transactional
    public Resultado<List<ImagenDTO>> subirImagenes(Long idAlojamiento, List<MultipartFile> archivos, Long idActor,
            boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin,
                AuditoriaService.IMAGEN_ALOJAMIENTO);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        List<Imagen> actuales = imagenRepo.findByIdAlojamientoOrderByIdImagen(idAlojamiento);
        String error = errorArchivos(archivos, actuales.size());
        if (error != null) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, error);
        }

        guardarArchivos(idAlojamiento, archivos,
                actuales.stream().anyMatch(i -> Boolean.TRUE.equals(i.getPrincipal())));
        marcarActualizado(carga.getDato());
        auditoriaService.registrar(idActor, AuditoriaService.IMAGEN_ALOJAMIENTO, AuditoriaService.ENTIDAD_ALOJAMIENTO,
                idAlojamiento, ResultadoAuditoria.EXITOSO, "Subidas: " + archivos.size());
        return Resultado.exito(imagenesDTO(idAlojamiento));
    }

    /**
     * Elimina una imagen (y su archivo, si fue subido). Si era la principal,
     * la siguiente pasa a serlo. Todo alojamiento, también en BORRADOR, debe
     * conservar al menos una imagen.
     *
     * @return 0 con las imágenes restantes; 1 no existe; 3 sin permiso;
     *         4 es la última imagen.
     * @see "RF-018"
     */
    @Transactional
    public Resultado<List<ImagenDTO>> eliminarImagen(Long idAlojamiento, Long idImagen, Long idActor,
            boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin,
                AuditoriaService.IMAGEN_ALOJAMIENTO);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        Optional<Imagen> imagen = imagenRepo.findByIdImagenAndIdAlojamiento(idImagen, idAlojamiento);
        if (imagen.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "La imagen no existe.");
        }
        List<Imagen> actuales = imagenRepo.findByIdAlojamientoOrderByIdImagen(idAlojamiento);
        if (actuales.size() == 1) {
            return Resultado.error(Resultado.ESTADO_INVALIDO,
                    "El alojamiento debe conservar al menos una imagen. Sube otra antes de borrar esta.");
        }

        imagenRepo.delete(imagen.get());
        almacenamientoService.eliminar(imagen.get().getRutaArchivo());
        if (Boolean.TRUE.equals(imagen.get().getPrincipal())) {
            actuales.stream().filter(i -> !i.getIdImagen().equals(idImagen)).findFirst().ifPresent(siguiente -> {
                siguiente.setPrincipal(true);
                imagenRepo.save(siguiente);
            });
        }
        marcarActualizado(carga.getDato());
        auditoriaService.registrar(idActor, AuditoriaService.IMAGEN_ALOJAMIENTO, AuditoriaService.ENTIDAD_ALOJAMIENTO,
                idAlojamiento, ResultadoAuditoria.EXITOSO, "Eliminada la imagen " + idImagen);
        return Resultado.exito(imagenesDTO(idAlojamiento));
    }

    /**
     * Marca una imagen como principal (portada); las demás dejan de serlo.
     *
     * @return 0 con las imágenes (la principal primero); 1 no existe; 3 sin permiso.
     * @see "RF-018"
     */
    @Transactional
    public Resultado<List<ImagenDTO>> marcarPrincipal(Long idAlojamiento, Long idImagen, Long idActor,
            boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin,
                AuditoriaService.IMAGEN_ALOJAMIENTO);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        if (imagenRepo.findByIdImagenAndIdAlojamiento(idImagen, idAlojamiento).isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "La imagen no existe.");
        }
        for (Imagen imagen : imagenRepo.findByIdAlojamientoOrderByIdImagen(idAlojamiento)) {
            imagen.setPrincipal(imagen.getIdImagen().equals(idImagen));
            imagenRepo.save(imagen);
        }
        marcarActualizado(carga.getDato());
        auditoriaService.registrar(idActor, AuditoriaService.IMAGEN_ALOJAMIENTO, AuditoriaService.ENTIDAD_ALOJAMIENTO,
                idAlojamiento, ResultadoAuditoria.EXITOSO, "Principal: imagen " + idImagen);
        return Resultado.exito(imagenesDTO(idAlojamiento));
    }

    // ----- ciclo de estados -----

    /**
     * BORRADOR → PENDIENTE_REVISION (anfitrión). Exige la información mínima
     * completa e indica los datos faltantes.
     *
     * @return 0 enviada; 1 no existe; 2 faltan datos; 3 sin permiso; 4 estado no permitido.
     * @see "RF-017, RF-027, CA-097, CA-157"
     */
    @Transactional
    public Resultado<AlojamientoDTO> enviarRevision(Long idAlojamiento, Long idActor) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, false,
                AuditoriaService.ALOJAMIENTO_CAMBIO_ESTADO);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        Alojamiento alojamiento = carga.getDato();
        if (alojamiento.getEstado() != EstadoAlojamiento.BORRADOR) {
            return rechazarTransicion(alojamiento, EstadoAlojamiento.PENDIENTE_REVISION, idActor);
        }
        List<String> faltantes = camposFaltantes(alojamiento);
        if (!faltantes.isEmpty()) {
            String mensaje = "Completa los datos antes de enviar a revisión. Falta: " + String.join(", ", faltantes)
                    + ".";
            auditoriaService.registrar(idActor, AuditoriaService.ALOJAMIENTO_CAMBIO_ESTADO,
                    AuditoriaService.ENTIDAD_ALOJAMIENTO, idAlojamiento, ResultadoAuditoria.RECHAZADO, mensaje);
            return Resultado.error(Resultado.DATOS_INVALIDOS, mensaje);
        }
        return Resultado.exito(cambiarEstado(alojamiento, EstadoAlojamiento.PENDIENTE_REVISION, idActor, null));
    }

    /**
     * ACTIVA → PAUSADA (anfitrión). Una publicación pausada no recibe reservas.
     *
     * @see "RF-018, CA-108, CA-109"
     */
    @Transactional
    public Resultado<AlojamientoDTO> pausar(Long idAlojamiento, Long idActor) {
        return transicionDelAnfitrion(idAlojamiento, idActor, EstadoAlojamiento.ACTIVA, EstadoAlojamiento.PAUSADA);
    }

    /** PAUSADA → ACTIVA (anfitrión). */
    @Transactional
    public Resultado<AlojamientoDTO> reanudar(Long idAlojamiento, Long idActor) {
        return transicionDelAnfitrion(idAlojamiento, idActor, EstadoAlojamiento.PAUSADA, EstadoAlojamiento.ACTIVA);
    }

    /**
     * PENDIENTE_REVISION → ACTIVA (administrador): queda disponible para
     * reservas y visible en búsquedas.
     *
     * @see "RF-027, CA-158, CA-194, CA-196"
     */
    @Transactional
    public Resultado<AlojamientoDTO> aprobar(Long idAlojamiento, Long idAdmin) {
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        if (alojamiento.get().getEstado() != EstadoAlojamiento.PENDIENTE_REVISION) {
            return rechazarTransicion(alojamiento.get(), EstadoAlojamiento.ACTIVA, idAdmin);
        }
        AlojamientoDTO dto = cambiarEstado(alojamiento.get(), EstadoAlojamiento.ACTIVA, idAdmin, "Aprobada");
        notificacionService.enviar(alojamiento.get().getIdAnfitrion(), NotificacionService.ALOJAMIENTO_APROBADO,
                "Tu alojamiento \"" + alojamiento.get().getTitulo() + "\" fue aprobado y ya está publicado.");
        return Resultado.exito(dto);
    }

    /**
     * PENDIENTE_REVISION → BORRADOR (administrador) con motivo obligatorio. Se
     * notifica al anfitrión para que corrija y vuelva a enviarla.
     *
     * @see "RF-027, CA-195, CA-196"
     */
    @Transactional
    public Resultado<AlojamientoDTO> rechazar(Long idAlojamiento, Long idAdmin, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "El motivo del rechazo es obligatorio.");
        }
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        if (alojamiento.get().getEstado() != EstadoAlojamiento.PENDIENTE_REVISION) {
            return rechazarTransicion(alojamiento.get(), EstadoAlojamiento.BORRADOR, idAdmin);
        }
        AlojamientoDTO dto = cambiarEstado(alojamiento.get(), EstadoAlojamiento.BORRADOR, idAdmin,
                "Rechazada: " + motivo.trim());
        notificacionService.enviar(alojamiento.get().getIdAnfitrion(), NotificacionService.ALOJAMIENTO_RECHAZADO,
                "Tu alojamiento \"" + alojamiento.get().getTitulo() + "\" fue rechazado. Motivo: " + motivo.trim()
                        + ". Corrígelo y vuelve a enviarlo a revisión.");
        return Resultado.exito(dto);
    }

    /**
     * Cualquier estado → BLOQUEADA (administrador) con motivo. Las reservas
     * futuras se conservan y pasan a EN_REVISION; se notifica al anfitrión y a
     * cada huésped afectado.
     *
     * @see "RF-013, CA-054, CA-111, CA-112"
     */
    @Transactional
    public Resultado<AlojamientoDTO> bloquear(Long idAlojamiento, Long idAdmin, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "El motivo del bloqueo es obligatorio.");
        }
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        if (alojamiento.get().getEstado() == EstadoAlojamiento.BLOQUEADA) {
            return rechazarTransicion(alojamiento.get(), EstadoAlojamiento.BLOQUEADA, idAdmin);
        }

        List<Reserva> futuras = reservaRepo.findByIdAlojamientoAndFechaEntradaAfterAndEstadoIn(idAlojamiento,
                LocalDate.now().minusDays(1), List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE_PAGO));
        for (Reserva reserva : futuras) {
            reserva.setEstado(EstadoReserva.EN_REVISION);
            reservaRepo.save(reserva);
            notificacionService.enviar(reserva.getIdHuesped(), NotificacionService.RESERVA_EN_REVISION,
                    "Tu reserva " + reserva.getIdReserva() + " quedó en revisión porque el alojamiento fue "
                            + "bloqueado por un administrador.");
        }

        AlojamientoDTO dto = cambiarEstado(alojamiento.get(), EstadoAlojamiento.BLOQUEADA, idAdmin,
                "Bloqueada: " + motivo.trim() + " (" + futuras.size() + " reservas futuras en revisión)");
        notificacionService.enviar(alojamiento.get().getIdAnfitrion(), NotificacionService.ALOJAMIENTO_BLOQUEADO,
                "Tu alojamiento \"" + alojamiento.get().getTitulo() + "\" fue bloqueado. Motivo: " + motivo.trim()
                        + ". Reservas futuras en revisión: " + futuras.size() + ".");
        return Resultado.exito(dto);
    }

    /** BLOQUEADA → PAUSADA (administrador). */
    @Transactional
    public Resultado<AlojamientoDTO> desbloquear(Long idAlojamiento, Long idAdmin) {
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        if (alojamiento.get().getEstado() != EstadoAlojamiento.BLOQUEADA) {
            return rechazarTransicion(alojamiento.get(), EstadoAlojamiento.PAUSADA, idAdmin);
        }
        return Resultado.exito(cambiarEstado(alojamiento.get(), EstadoAlojamiento.PAUSADA, idAdmin, "Desbloqueada"));
    }

    // ----- servicios adicionales -----

    /**
     * Agrega un servicio adicional con nombre y costo positivo.
     *
     * @see "RF-018, CA-185, CA-187"
     */
    @Transactional
    public Resultado<ServicioAdicionalDTO> agregarServicio(Long idAlojamiento, ServicioAdicionalDTO datos,
            Long idActor, boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin,
                AuditoriaService.SERVICIO_ADICIONAL);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        if (!servicioValido(datos)) {
            return Resultado.error(Resultado.DATOS_INVALIDOS,
                    "El servicio adicional necesita un nombre y un costo positivo.");
        }
        ServicioAdicional servicio = new ServicioAdicional();
        servicio.setIdAlojamiento(idAlojamiento);
        servicio.setNombre(datos.getNombre().trim());
        servicio.setCosto(datos.getCosto());
        servicio = servicioRepo.save(servicio);
        auditoriaService.registrar(idActor, AuditoriaService.SERVICIO_ADICIONAL, AuditoriaService.ENTIDAD_ALOJAMIENTO,
                idAlojamiento, ResultadoAuditoria.EXITOSO, "Agregado: " + servicio.getNombre());
        return Resultado.exito(modelMapper.map(servicio, ServicioAdicionalDTO.class));
    }

    /**
     * Modifica un servicio adicional. Aplica a cotizaciones nuevas; las
     * reservas ya confirmadas conservan su desglose congelado.
     *
     * @see "RF-018, CA-186, CA-187"
     */
    @Transactional
    public Resultado<ServicioAdicionalDTO> actualizarServicio(Long idAlojamiento, Long idServicio,
            ServicioAdicionalDTO datos, Long idActor, boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin,
                AuditoriaService.SERVICIO_ADICIONAL);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        Optional<ServicioAdicional> servicio = servicioRepo.findByIdServicioAndIdAlojamiento(idServicio,
                idAlojamiento);
        if (servicio.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El servicio adicional no existe.");
        }
        if (!servicioValido(datos)) {
            return Resultado.error(Resultado.DATOS_INVALIDOS,
                    "El servicio adicional necesita un nombre y un costo positivo.");
        }
        servicio.get().setNombre(datos.getNombre().trim());
        servicio.get().setCosto(datos.getCosto());
        ServicioAdicional guardado = servicioRepo.save(servicio.get());
        auditoriaService.registrar(idActor, AuditoriaService.SERVICIO_ADICIONAL, AuditoriaService.ENTIDAD_ALOJAMIENTO,
                idAlojamiento, ResultadoAuditoria.EXITOSO, "Modificado: " + guardado.getNombre());
        return Resultado.exito(modelMapper.map(guardado, ServicioAdicionalDTO.class));
    }

    /**
     * Elimina un servicio adicional; las reservas confirmadas no cambian.
     *
     * @see "RF-018, CA-186, CA-187"
     */
    @Transactional
    public Resultado<Void> eliminarServicio(Long idAlojamiento, Long idServicio, Long idActor, boolean esAdmin) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, esAdmin,
                AuditoriaService.SERVICIO_ADICIONAL);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        Optional<ServicioAdicional> servicio = servicioRepo.findByIdServicioAndIdAlojamiento(idServicio,
                idAlojamiento);
        if (servicio.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El servicio adicional no existe.");
        }
        servicioRepo.delete(servicio.get());
        auditoriaService.registrar(idActor, AuditoriaService.SERVICIO_ADICIONAL, AuditoriaService.ENTIDAD_ALOJAMIENTO,
                idAlojamiento, ResultadoAuditoria.EXITOSO, "Eliminado: " + servicio.get().getNombre());
        return Resultado.exito(null);
    }

    // ----- reglas compartidas -----

    /**
     * Datos mínimos para enviar a revisión: título, descripción, ubicación,
     * ciudad, tipo, capacidad, habitaciones, baños, horas de entrada y salida,
     * precio, reglas, política de cancelación y al menos una imagen.
     *
     * @return nombres de los datos que faltan (vacía si está completa).
     * @see "CA-096, CA-097"
     */
    public List<String> camposFaltantes(Alojamiento alojamiento) {
        List<String> faltantes = new ArrayList<>();
        agregarSiFalta(faltantes, alojamiento.getTitulo(), "título");
        agregarSiFalta(faltantes, alojamiento.getDescripcion(), "descripción");
        agregarSiFalta(faltantes, alojamiento.getUbicacion(), "ubicación");
        agregarSiFalta(faltantes, alojamiento.getCiudad(), "ciudad");
        agregarSiFalta(faltantes, alojamiento.getTipo(), "tipo");
        agregarSiFalta(faltantes, alojamiento.getReglas(), "reglas");
        if (alojamiento.getCapacidad() == null) {
            faltantes.add("capacidad");
        }
        if (alojamiento.getHabitaciones() == null) {
            faltantes.add("habitaciones");
        }
        if (alojamiento.getBanos() == null) {
            faltantes.add("baños");
        }
        if (alojamiento.getHoraEntrada() == null) {
            faltantes.add("hora de entrada");
        }
        if (alojamiento.getHoraSalida() == null) {
            faltantes.add("hora de salida");
        }
        if (alojamiento.getPrecioNoche() == null) {
            faltantes.add("precio por noche");
        }
        if (alojamiento.getIdPolitica() == null) {
            faltantes.add("política de cancelación");
        }
        if (imagenRepo.findByIdAlojamientoOrderByIdImagen(alojamiento.getIdAlojamiento()).isEmpty()) {
            faltantes.add("al menos una imagen");
        }
        return faltantes;
    }

    /**
     * Convierte la entidad a DTO con imágenes (la principal primero),
     * servicios adicionales, nombre de la política, calificación, cantidad de
     * reseñas y reservas, y nombre del anfitrión. Así el front arma tarjetas y
     * detalle con una sola llamada.
     */
    public AlojamientoDTO convertirDTO(Alojamiento alojamiento) {
        Long id = alojamiento.getIdAlojamiento();
        AlojamientoDTO dto = modelMapper.map(alojamiento, AlojamientoDTO.class);
        dto.setAmenidades(new HashSet<>(alojamiento.getAmenidades()));
        List<ImagenDTO> imagenes = imagenesDTO(id);
        dto.setDetalleImagenes(imagenes);
        dto.setImagenes(imagenes.stream().map(ImagenDTO::getUrl).toList());
        dto.setServiciosAdicionales(servicioRepo.findByIdAlojamientoOrderByIdServicio(id)
                .stream().map(s -> modelMapper.map(s, ServicioAdicionalDTO.class)).toList());
        if (alojamiento.getIdPolitica() != null) {
            politicaRepo.findById(alojamiento.getIdPolitica()).ifPresent(p -> dto.setNombrePolitica(p.getNombre()));
        }
        Double promedio = resenaRepo.promedio(id, EstadoResena.PUBLICADA);
        dto.setPromedioCalificacion(promedio == null ? null
                : BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP).doubleValue());
        dto.setCantidadResenas(resenaRepo.countByIdAlojamientoAndEstado(id, EstadoResena.PUBLICADA));
        dto.setCantidadReservas(reservaRepo.countByIdAlojamientoAndEstadoIn(id, RESERVAS_VIGENTES));
        usuarioRepo.findById(alojamiento.getIdAnfitrion())
                .ifPresent(u -> dto.setNombreAnfitrion(u.getNombre()));
        return dto;
    }

    /** Igual que {@link #convertirDTO(Alojamiento)} más el correo y el estado del anfitrión (solo ADMIN). */
    public AlojamientoDTO convertirDTOAdmin(Alojamiento alojamiento) {
        AlojamientoDTO dto = convertirDTO(alojamiento);
        Optional<Usuario> anfitrion = usuarioRepo.findById(alojamiento.getIdAnfitrion());
        anfitrion.ifPresent(u -> {
            dto.setCorreoAnfitrion(AESUtil.decrypt(u.getCorreo()));
            dto.setEstadoAnfitrion(u.getEstado());
        });
        return dto;
    }

    /** Imágenes del alojamiento con la principal primero y luego en orden de carga. */
    public List<ImagenDTO> imagenesDTO(Long idAlojamiento) {
        return imagenRepo.findByIdAlojamientoOrderByIdImagen(idAlojamiento).stream()
                .sorted(Comparator.comparing((Imagen i) -> !Boolean.TRUE.equals(i.getPrincipal())))
                .map(i -> modelMapper.map(i, ImagenDTO.class)).toList();
    }

    private static boolean esVisible(Alojamiento alojamiento, Long idActor, boolean esAdmin) {
        return alojamiento.getEstado() == EstadoAlojamiento.ACTIVA || esAdmin || esPropietario(alojamiento, idActor);
    }

    private static boolean esPropietario(Alojamiento alojamiento, Long idActor) {
        return idActor != null && idActor.equals(alojamiento.getIdAnfitrion());
    }

    /** Carga el alojamiento si el actor puede modificarlo; si no, audita el intento (CA-152). */
    private Resultado<Alojamiento> cargarModificable(Long idAlojamiento, Long idActor, boolean esAdmin,
            String accion) {
        Optional<Alojamiento> alojamiento = alojamientoRepo.findById(idAlojamiento);
        if (alojamiento.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, NO_EXISTE);
        }
        if (!esAdmin && !esPropietario(alojamiento.get(), idActor)) {
            auditoriaService.registrar(idActor, accion, AuditoriaService.ENTIDAD_ALOJAMIENTO, idAlojamiento,
                    ResultadoAuditoria.RECHAZADO, "Sin permiso sobre el alojamiento");
            return Resultado.error(Resultado.SIN_PERMISO, SIN_PERMISO);
        }
        return Resultado.exito(alojamiento.get());
    }

    private Resultado<AlojamientoDTO> transicionDelAnfitrion(Long idAlojamiento, Long idActor,
            EstadoAlojamiento desde, EstadoAlojamiento hacia) {
        Resultado<Alojamiento> carga = cargarModificable(idAlojamiento, idActor, false,
                AuditoriaService.ALOJAMIENTO_CAMBIO_ESTADO);
        if (!carga.esExito()) {
            return Resultado.error(carga.getCodigo(), carga.getMensaje());
        }
        if (carga.getDato().getEstado() != desde) {
            return rechazarTransicion(carga.getDato(), hacia, idActor);
        }
        return Resultado.exito(cambiarEstado(carga.getDato(), hacia, idActor, null));
    }

    private AlojamientoDTO cambiarEstado(Alojamiento alojamiento, EstadoAlojamiento nuevo, Long idActor,
            String motivo) {
        EstadoAlojamiento anterior = alojamiento.getEstado();
        alojamiento.setEstado(nuevo);
        Alojamiento guardado = alojamientoRepo.save(alojamiento);
        registrarHistorial(guardado.getIdAlojamiento(), anterior, nuevo, idActor, motivo);
        auditoriaService.registrar(idActor, AuditoriaService.ALOJAMIENTO_CAMBIO_ESTADO,
                AuditoriaService.ENTIDAD_ALOJAMIENTO, guardado.getIdAlojamiento(), ResultadoAuditoria.EXITOSO,
                anterior + " -> " + nuevo + (motivo == null ? "" : ". " + motivo));
        return convertirDTO(guardado);
    }

    private Resultado<AlojamientoDTO> rechazarTransicion(Alojamiento alojamiento, EstadoAlojamiento destino,
            Long idActor) {
        String mensaje = "No se puede pasar de " + alojamiento.getEstado() + " a " + destino + ".";
        auditoriaService.registrar(idActor, AuditoriaService.ALOJAMIENTO_CAMBIO_ESTADO,
                AuditoriaService.ENTIDAD_ALOJAMIENTO, alojamiento.getIdAlojamiento(), ResultadoAuditoria.RECHAZADO,
                mensaje);
        return Resultado.error(Resultado.ESTADO_INVALIDO, mensaje);
    }

    private void registrarHistorial(Long idAlojamiento, EstadoAlojamiento anterior, EstadoAlojamiento nuevo,
            Long idActor, String motivo) {
        HistorialEstadoAlojamiento historial = new HistorialEstadoAlojamiento();
        historial.setIdAlojamiento(idAlojamiento);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setIdUsuario(idActor);
        historial.setMotivo(motivo);
        historial.setFecha(LocalDateTime.now());
        historialRepo.save(historial);
    }

    /** Valida los datos enviados; devuelve el mensaje de error o null si son válidos. */
    private String validar(AlojamientoDTO datos, boolean creacion) {
        if (datos == null) {
            return "Debes enviar los datos del alojamiento.";
        }
        if (creacion && (datos.getTitulo() == null || datos.getTitulo().isBlank())) {
            return "El título es obligatorio.";
        }
        if (datos.getCapacidad() != null && datos.getCapacidad() <= 0) {
            return "La capacidad debe ser mayor que cero.";
        }
        if ((datos.getHabitaciones() != null && datos.getHabitaciones() < 0)
                || (datos.getBanos() != null && datos.getBanos() < 0)) {
            return "Las habitaciones y los baños no pueden ser negativos.";
        }
        if (datos.getPrecioNoche() != null && datos.getPrecioNoche().signum() <= 0) {
            return "El precio por noche debe ser positivo.";
        }
        if ((datos.getPorcentajeLimpieza() != null && !TarifaService.porcentajeValido(datos.getPorcentajeLimpieza()))
                || (datos.getPorcentajeServicio() != null
                        && !TarifaService.porcentajeValido(datos.getPorcentajeServicio()))) {
            return "Los porcentajes de limpieza y servicio deben estar entre 0 y 100.";
        }
        if ((datos.getLatitud() != null && Math.abs(datos.getLatitud()) > LATITUD_MAXIMA)
                || (datos.getLongitud() != null && Math.abs(datos.getLongitud()) > LONGITUD_MAXIMA)) {
            return "La latitud debe estar entre -90 y 90 y la longitud entre -180 y 180.";
        }
        if (datos.getIdPolitica() != null && !politicaRepo.existsById(datos.getIdPolitica())) {
            return "La política de cancelación no existe.";
        }
        return null;
    }

    private static void aplicar(AlojamientoDTO datos, Alojamiento alojamiento) {
        if (datos.getTitulo() != null) {
            alojamiento.setTitulo(datos.getTitulo().trim());
        }
        if (datos.getDescripcion() != null) {
            alojamiento.setDescripcion(datos.getDescripcion().trim());
        }
        if (datos.getUbicacion() != null) {
            alojamiento.setUbicacion(datos.getUbicacion().trim());
        }
        if (datos.getCiudad() != null) {
            alojamiento.setCiudad(datos.getCiudad().trim());
        }
        if (datos.getTipo() != null) {
            alojamiento.setTipo(datos.getTipo().trim());
        }
        if (datos.getReglas() != null) {
            alojamiento.setReglas(datos.getReglas().trim());
        }
        if (datos.getLatitud() != null) {
            alojamiento.setLatitud(datos.getLatitud());
        }
        if (datos.getLongitud() != null) {
            alojamiento.setLongitud(datos.getLongitud());
        }
        if (datos.getCapacidad() != null) {
            alojamiento.setCapacidad(datos.getCapacidad());
        }
        if (datos.getHabitaciones() != null) {
            alojamiento.setHabitaciones(datos.getHabitaciones());
        }
        if (datos.getBanos() != null) {
            alojamiento.setBanos(datos.getBanos());
        }
        if (datos.getHoraEntrada() != null) {
            alojamiento.setHoraEntrada(datos.getHoraEntrada());
        }
        if (datos.getHoraSalida() != null) {
            alojamiento.setHoraSalida(datos.getHoraSalida());
        }
        if (datos.getPrecioNoche() != null) {
            alojamiento.setPrecioNoche(datos.getPrecioNoche());
        }
        if (datos.getPorcentajeLimpieza() != null) {
            alojamiento.setPorcentajeLimpieza(datos.getPorcentajeLimpieza());
        }
        if (datos.getPorcentajeServicio() != null) {
            alojamiento.setPorcentajeServicio(datos.getPorcentajeServicio());
        }
        if (datos.getIdPolitica() != null) {
            alojamiento.setIdPolitica(datos.getIdPolitica());
        }
        if (datos.getAmenidades() != null && !datos.getAmenidades().isEmpty()) {
            Set<String> amenidades = new HashSet<>();
            for (String amenidad : datos.getAmenidades()) {
                if (amenidad != null && !amenidad.isBlank()) {
                    amenidades.add(amenidad.trim().toLowerCase());
                }
            }
            alojamiento.setAmenidades(amenidades);
        }
    }

    /**
     * Valida un lote de imágenes antes de guardar alguna: que venga al menos
     * una, que no se supere {@link #MAXIMO_IMAGENES} y que cada archivo sea
     * una imagen válida.
     *
     * @param actuales Imágenes que ya tiene el alojamiento.
     * @return el mensaje de error o null si el lote es válido.
     */
    private String errorArchivos(List<MultipartFile> archivos, int actuales) {
        if (archivos == null || archivos.isEmpty()) {
            return "Debes enviar al menos una imagen.";
        }
        if (actuales + archivos.size() > MAXIMO_IMAGENES) {
            return "Un alojamiento puede tener máximo " + MAXIMO_IMAGENES + " imágenes.";
        }
        for (MultipartFile archivo : archivos) {
            String error = almacenamientoService.errorImagen(archivo);
            if (error != null) {
                return archivo == null ? error : error + " (" + archivo.getOriginalFilename() + ")";
            }
        }
        return null;
    }

    /** Guarda los archivos ya validados; si el alojamiento no tenía portada, el primero queda como principal. */
    private void guardarArchivos(Long idAlojamiento, List<MultipartFile> archivos, boolean tienePrincipal) {
        for (MultipartFile archivo : archivos) {
            String ruta = almacenamientoService.guardarImagen(archivo, "alojamientos/" + idAlojamiento);
            Imagen imagen = new Imagen();
            imagen.setIdAlojamiento(idAlojamiento);
            imagen.setRutaArchivo(ruta);
            imagen.setUrl(AlmacenamientoService.urlPublica(ruta));
            imagen.setPrincipal(!tienePrincipal);
            imagenRepo.save(imagen);
            tienePrincipal = true;
        }
    }

    private void marcarActualizado(Alojamiento alojamiento) {
        alojamiento.setFechaActualizacion(LocalDateTime.now());
        alojamientoRepo.save(alojamiento);
    }

    private static boolean servicioValido(ServicioAdicionalDTO datos) {
        return datos != null && datos.getNombre() != null && !datos.getNombre().isBlank() && datos.getCosto() != null
                && datos.getCosto().signum() > 0;
    }

    private static void agregarSiFalta(List<String> faltantes, String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            faltantes.add(nombre);
        }
    }
}
