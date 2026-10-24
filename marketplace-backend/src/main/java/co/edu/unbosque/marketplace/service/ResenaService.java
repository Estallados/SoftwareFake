package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.dto.ResenaDTO;
import co.edu.unbosque.marketplace.dto.ResumenResenasDTO;
import co.edu.unbosque.marketplace.entity.EstadoResena;
import co.edu.unbosque.marketplace.entity.Resena;
import co.edu.unbosque.marketplace.entity.Reserva;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Reseñas y calificaciones. Solo el huésped de una reserva completada puede
 * reseñar, una vez por reserva, con calificación de 1 a 5. La consulta pública
 * no incluye las reseñas ocultas por un administrador.
 *
 * @see "RF-004, RF-016, CA-016 a CA-020, CA-066, CA-067"
 */
@Service
public class ResenaService {

    private static final int CALIFICACION_MINIMA = 1;
    private static final int CALIFICACION_MAXIMA = 5;
    private static final int LARGO_MAXIMO = 2000;

    private final ResenaRepository resenaRepo;
    private final ReservaRepository reservaRepo;
    private final AlojamientoRepository alojamientoRepo;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final ModelMapper modelMapper;

    public ResenaService(ResenaRepository resenaRepo, ReservaRepository reservaRepo,
            AlojamientoRepository alojamientoRepo, NotificacionService notificacionService,
            AuditoriaService auditoriaService, ModelMapper modelMapper) {
        this.resenaRepo = resenaRepo;
        this.reservaRepo = reservaRepo;
        this.alojamientoRepo = alojamientoRepo;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.modelMapper = modelMapper;
    }

    /**
     * Registra la reseña del huésped sobre una reserva completada.
     *
     * @return 0 con la reseña; 1 la reserva no existe o es de otro usuario;
     *         2 calificación o comentario inválidos; 4 la reserva no está
     *         completada o ya tiene reseña.
     * @see "CA-016, CA-017, CA-018"
     */
    @Transactional
    public Resultado<ResenaDTO> crear(ResenaDTO datos, Long idHuesped) {
        if (datos == null || datos.getIdReserva() == null || datos.getCalificacion() == null
                || datos.getCalificacion() < CALIFICACION_MINIMA || datos.getCalificacion() > CALIFICACION_MAXIMA) {
            return Resultado.error(Resultado.DATOS_INVALIDOS,
                    "Indica la reserva y una calificación entre 1 y 5.");
        }
        if (datos.getComentario() != null && datos.getComentario().length() > LARGO_MAXIMO) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "El comentario no puede superar 2000 caracteres.");
        }
        Optional<Reserva> reserva = reservaRepo.findById(datos.getIdReserva())
                .filter(r -> r.getIdHuesped().equals(idHuesped));
        if (reserva.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "La reserva no existe.");
        }
        if (!ReservaService.esCompletada(reserva.get())) {
            return rechazar(idHuesped, "Solo puedes reseñar una estancia completada.");
        }
        if (resenaRepo.existsByIdReserva(datos.getIdReserva())) {
            return rechazar(idHuesped, "Ya registraste una reseña para esta reserva.");
        }

        Resena resena = new Resena();
        resena.setIdReserva(reserva.get().getIdReserva());
        resena.setIdAlojamiento(reserva.get().getIdAlojamiento());
        resena.setIdUsuario(idHuesped);
        resena.setCalificacion(datos.getCalificacion());
        resena.setComentario(datos.getComentario() == null ? null : datos.getComentario().trim());
        resena.setEstado(EstadoResena.PUBLICADA);
        resena.setFecha(LocalDateTime.now());
        try {
            resena = resenaRepo.saveAndFlush(resena);
        } catch (DataIntegrityViolationException e) {
            return rechazar(idHuesped, "Ya registraste una reseña para esta reserva.");
        }

        alojamientoRepo.findById(resena.getIdAlojamiento()).ifPresent(a -> notificacionService.enviar(
                a.getIdAnfitrion(), NotificacionService.RESENA_RECIBIDA,
                "Recibiste una reseña de " + datos.getCalificacion() + " estrellas en \"" + a.getTitulo() + "\"."));
        auditoriaService.registrar(idHuesped, AuditoriaService.RESENA_CREADA, AuditoriaService.ENTIDAD_RESENA,
                resena.getIdResena(), ResultadoAuditoria.EXITOSO, "Reserva " + resena.getIdReserva());
        return Resultado.exito(modelMapper.map(resena, ResenaDTO.class));
    }

    /**
     * Reseñas visibles de un alojamiento con su promedio y cantidad.
     *
     * @return 0 con el resumen; 1 si el alojamiento no existe.
     * @see "CA-019, CA-020, CA-067"
     */
    public Resultado<ResumenResenasDTO> resumen(Long idAlojamiento, Pageable pagina) {
        if (!alojamientoRepo.existsById(idAlojamiento)) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe.");
        }
        Double promedio = resenaRepo.promedio(idAlojamiento, EstadoResena.PUBLICADA);
        ResumenResenasDTO resumen = new ResumenResenasDTO();
        resumen.setIdAlojamiento(idAlojamiento);
        resumen.setPromedio(promedio == null ? null
                : BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP).doubleValue());
        resumen.setCantidad(resenaRepo.countByIdAlojamientoAndEstado(idAlojamiento, EstadoResena.PUBLICADA));
        resumen.setResenas(PaginaDTO.de(resenaRepo.findByIdAlojamientoAndEstado(idAlojamiento,
                EstadoResena.PUBLICADA, pagina), r -> modelMapper.map(r, ResenaDTO.class)));
        return Resultado.exito(resumen);
    }

    /**
     * Oculta o vuelve a mostrar una reseña (administrador). No la borra.
     *
     * @param oculta  true para ocultar, false para mostrar.
     * @param motivo  Motivo obligatorio al ocultar.
     * @return 0 con la reseña; 1 no existe; 2 falta el motivo.
     * @see "RF-016, CA-066, CA-067"
     */
    @Transactional
    public Resultado<ResenaDTO> moderar(Long idResena, boolean oculta, String motivo, Long idAdmin) {
        if (oculta && (motivo == null || motivo.isBlank())) {
            return Resultado.error(Resultado.DATOS_INVALIDOS, "El motivo para ocultar la reseña es obligatorio.");
        }
        Optional<Resena> resena = resenaRepo.findById(idResena);
        if (resena.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "La reseña no existe.");
        }
        resena.get().setEstado(oculta ? EstadoResena.OCULTA : EstadoResena.PUBLICADA);
        Resena guardada = resenaRepo.save(resena.get());
        auditoriaService.registrar(idAdmin, AuditoriaService.RESENA_MODERADA, AuditoriaService.ENTIDAD_RESENA,
                idResena, ResultadoAuditoria.EXITOSO, (oculta ? "Ocultada: " + motivo.trim() : "Visible de nuevo"));
        return Resultado.exito(modelMapper.map(guardada, ResenaDTO.class));
    }

    private Resultado<ResenaDTO> rechazar(Long idHuesped, String mensaje) {
        auditoriaService.registrar(idHuesped, AuditoriaService.RESENA_CREADA, AuditoriaService.ENTIDAD_RESENA, null,
                ResultadoAuditoria.RECHAZADO, mensaje);
        return Resultado.error(Resultado.ESTADO_INVALIDO, mensaje);
    }
}
