package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.AlojamientoDTO;
import co.edu.unbosque.marketplace.dto.HistorialEstadoAlojamientoDTO;
import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.dto.PoliticaCancelacionDTO;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.HistorialEstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.Imagen;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.entity.ServicioAdicional;
import co.edu.unbosque.marketplace.repository.AlojamientoEspecificacion;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.HistorialEstadoAlojamientoRepository;
import co.edu.unbosque.marketplace.repository.ImagenRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Publicaciones de alojamiento: creación, edición, ciclo de estados, servicios
 * adicionales y búsqueda pública.
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

    private final AlojamientoRepository alojamientoRepo;
    private final ImagenRepository imagenRepo;
    private final ServicioAdicionalRepository servicioRepo;
    private final PoliticaCancelacionRepository politicaRepo;
    private final HistorialEstadoAlojamientoRepository historialRepo;
    private final ReservaRepository reservaRepo;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;
    private final ModelMapper modelMapper;

    public PublicacionService(AlojamientoRepository alojamientoRepo, ImagenRepository imagenRepo,
            ServicioAdicionalRepository servicioRepo, PoliticaCancelacionRepository politicaRepo,
            HistorialEstadoAlojamientoRepository historialRepo, ReservaRepository reservaRepo,
            AuditoriaService auditoriaService, NotificacionService notificacionService, ModelMapper modelMapper) {
        this.alojamientoRepo = alojamientoRepo;
        this.imagenRepo = imagenRepo;
        this.servicioRepo = servicioRepo;
        this.politicaRepo = politicaRepo;
        this.historialRepo = historialRepo;
        this.reservaRepo = reservaRepo;
        this.auditoriaService = auditoriaService;
        this.notificacionService = notificacionService;
        this.modelMapper = modelMapper;
    }

    // ----- consultas -----

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
        return Resultado.exito(convertirDTO(alojamiento.get()));
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
                : alojamientoRepo.findByEstado(estado, pagina), this::convertirDTO);
    }

    /**
     * Búsqueda pública: solo publicaciones ACTIVA que cumplan todos los filtros
     * enviados. Con fechas, excluye las que no estén libres en ese período.
     *
     * @return 0 con la página de resultados; 2 si los filtros no son válidos.
     * @see "RF-024, CA-087 a CA-092, CA-055, CA-077, CA-190"
     */
    public Resultado<PaginaDTO<AlojamientoDTO>> buscar(String ciudad, LocalDate llegada, LocalDate salida,
            Integer huespedes, String tipo, BigDecimal precioMinimo, BigDecimal precioMaximo,
            Collection<String> amenidades, Pageable pagina) {
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
        return Resultado.exito(PaginaDTO.de(alojamientoRepo.findAll(filtro, pagina), this::convertirDTO));
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
     * Crea una publicación en BORRADOR para el anfitrión autenticado. El id,
     * el estado y el propietario enviados se ignoran.
     *
     * @return 0 con la publicación; 2 si los datos no son válidos.
     * @see "RF-017, CA-094, CA-095, CA-096, CA-098, CA-099"
     */
    @Transactional
    public Resultado<AlojamientoDTO> crear(AlojamientoDTO datos, Long idAnfitrion) {
        String error = validar(datos, true);
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
        alojamiento = alojamientoRepo.save(alojamiento);
        guardarImagenes(alojamiento.getIdAlojamiento(), datos.getImagenes());
        registrarHistorial(alojamiento.getIdAlojamiento(), null, EstadoAlojamiento.BORRADOR, idAnfitrion, null);

        auditoriaService.registrar(idAnfitrion, AuditoriaService.ALOJAMIENTO_CREADO,
                AuditoriaService.ENTIDAD_ALOJAMIENTO, alojamiento.getIdAlojamiento(), ResultadoAuditoria.EXITOSO,
                null);
        return Resultado.exito(convertirDTO(alojamiento));
    }

    /**
     * Edita una publicación (propietario o administrador). Los campos en null
     * se dejan como están; las listas enviadas (imágenes, amenidades)
     * reemplazan a las anteriores. El estado y el propietario no se cambian
     * por esta vía.
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
        alojamiento = alojamientoRepo.save(alojamiento);
        if (datos.getImagenes() != null) {
            imagenRepo.deleteByIdAlojamiento(idAlojamiento);
            guardarImagenes(idAlojamiento, datos.getImagenes());
        }

        auditoriaService.registrar(idActor, AuditoriaService.ALOJAMIENTO_ACTUALIZADO,
                AuditoriaService.ENTIDAD_ALOJAMIENTO, idAlojamiento, ResultadoAuditoria.EXITOSO, null);
        return Resultado.exito(convertirDTO(alojamiento));
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
     * ciudad, tipo, capacidad, precio, reglas, política de cancelación y al
     * menos una imagen.
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

    /** Convierte la entidad a DTO con imágenes, servicios adicionales y nombre de la política. */
    public AlojamientoDTO convertirDTO(Alojamiento alojamiento) {
        AlojamientoDTO dto = modelMapper.map(alojamiento, AlojamientoDTO.class);
        dto.setAmenidades(new HashSet<>(alojamiento.getAmenidades()));
        dto.setImagenes(imagenRepo.findByIdAlojamientoOrderByIdImagen(alojamiento.getIdAlojamiento()).stream()
                .map(Imagen::getUrl).toList());
        dto.setServiciosAdicionales(servicioRepo.findByIdAlojamientoOrderByIdServicio(alojamiento.getIdAlojamiento())
                .stream().map(s -> modelMapper.map(s, ServicioAdicionalDTO.class)).toList());
        if (alojamiento.getIdPolitica() != null) {
            politicaRepo.findById(alojamiento.getIdPolitica()).ifPresent(p -> dto.setNombrePolitica(p.getNombre()));
        }
        return dto;
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

    private void guardarImagenes(Long idAlojamiento, List<String> urls) {
        if (urls == null) {
            return;
        }
        for (String url : urls) {
            if (url != null && !url.isBlank()) {
                Imagen imagen = new Imagen();
                imagen.setIdAlojamiento(idAlojamiento);
                imagen.setUrl(url.trim());
                imagenRepo.save(imagen);
            }
        }
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
