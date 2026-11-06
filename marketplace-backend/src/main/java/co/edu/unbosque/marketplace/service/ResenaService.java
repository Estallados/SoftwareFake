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
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Reseñas y calificaciones de los alojamientos.
 * <p>
 * Solo el huésped de una reserva completada puede reseñar, una vez por
 * reserva, con una calificación de 1 a 5. La consulta pública y la del
 * anfitrión no incluyen las reseñas que ocultó un administrador; el huésped ve
 * todas las suyas con su estado y el administrador ve todas.
 * </p>
 */
@Service
public class ResenaService {

	/** Calificación mínima permitida. */
	private static final int CALIFICACION_MINIMA = 1;

	/** Calificación máxima permitida. */
	private static final int CALIFICACION_MAXIMA = 5;

	/** Largo máximo del comentario. */
	private static final int LARGO_MAXIMO = 2000;

	/** Repositorio de reseñas. */
	private final ResenaRepository resenaRepo;

	/** Repositorio de reservas. */
	private final ReservaRepository reservaRepo;

	/** Repositorio de alojamientos. */
	private final AlojamientoRepository alojamientoRepo;

	/** Repositorio de usuarios, para los nombres. */
	private final UsuarioRepository usuarioRepo;

	/** Avisa al anfitrión y al autor. */
	private final NotificacionService notificacionService;

	/** Servicio de auditoría. */
	private final AuditoriaService auditoriaService;

	/** Copia los datos de la entidad al DTO. */
	private final ModelMapper modelMapper;

	/**
	 * Crea el servicio.
	 *
	 * @param resenaRepo repositorio de reseñas
	 * @param reservaRepo repositorio de reservas
	 * @param alojamientoRepo repositorio de alojamientos
	 * @param usuarioRepo repositorio de usuarios
	 * @param notificacionService servicio de notificaciones
	 * @param auditoriaService servicio de auditoría
	 * @param modelMapper copiador de entidades a DTO
	 */
	public ResenaService(ResenaRepository resenaRepo, ReservaRepository reservaRepo,
			AlojamientoRepository alojamientoRepo, UsuarioRepository usuarioRepo,
			NotificacionService notificacionService, AuditoriaService auditoriaService, ModelMapper modelMapper) {
		this.resenaRepo = resenaRepo;
		this.reservaRepo = reservaRepo;
		this.alojamientoRepo = alojamientoRepo;
		this.usuarioRepo = usuarioRepo;
		this.notificacionService = notificacionService;
		this.auditoriaService = auditoriaService;
		this.modelMapper = modelMapper;
	}

	/**
	 * Registra la reseña del huésped sobre una reserva completada y avisa al
	 * anfitrión.
	 *
	 * @param datos reserva, calificación y comentario
	 * @param idHuesped huésped autenticado
	 * @return la reseña; NO_ENCONTRADO si la reserva no existe o es de otro
	 *         usuario; DATOS_INVALIDOS si la calificación o el comentario no
	 *         son válidos; ESTADO_INVALIDO si la reserva no está completada o
	 *         ya tiene reseña
	 */
	@Transactional
	public Resultado<ResenaDTO> crear(ResenaDTO datos, Long idHuesped) {
		if (datos == null || datos.getIdReserva() == null || datos.getCalificacion() == null
				|| datos.getCalificacion() < CALIFICACION_MINIMA || datos.getCalificacion() > CALIFICACION_MAXIMA) {
			return Resultado.error(Resultado.DATOS_INVALIDOS, "Indica la reserva y una calificación entre 1 y 5.");
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
		return Resultado.exito(convertirDTO(resena));
	}

	/**
	 * Reseñas visibles de un alojamiento, con su promedio y cantidad.
	 *
	 * @param idAlojamiento alojamiento
	 * @param pagina página y orden
	 * @return el resumen; o NO_ENCONTRADO si el alojamiento no existe
	 */
	public Resultado<ResumenResenasDTO> resumen(Long idAlojamiento, Pageable pagina) {
		if (!alojamientoRepo.existsById(idAlojamiento)) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe.");
		}
		Double promedio = resenaRepo.promedio(idAlojamiento, EstadoResena.PUBLICADA);
		ResumenResenasDTO resumen = new ResumenResenasDTO();
		resumen.setIdAlojamiento(idAlojamiento);
		resumen.setPromedio(redondear(promedio));
		resumen.setCantidad(resenaRepo.countByIdAlojamientoAndEstado(idAlojamiento, EstadoResena.PUBLICADA));
		resumen.setResenas(
				PaginaDTO.de(resenaRepo.findByIdAlojamientoAndEstado(idAlojamiento, EstadoResena.PUBLICADA, pagina),
						this::convertirDTO));
		return Resultado.exito(resumen);
	}

	/**
	 * Reseñas que escribió el huésped autenticado, incluidas las ocultas (con
	 * su estado).
	 *
	 * @param idHuesped huésped
	 * @param pagina página y orden
	 * @return página de reseñas
	 */
	public PaginaDTO<ResenaDTO> listarMias(Long idHuesped, Pageable pagina) {
		return PaginaDTO.de(resenaRepo.findByIdUsuario(idHuesped, pagina), this::convertirDTO);
	}

	/**
	 * Reseñas visibles de los alojamientos del anfitrión autenticado, con su
	 * promedio y cantidad. Si se indica un alojamiento, solo las de ese, y
	 * debe ser suyo.
	 *
	 * @param idAnfitrion anfitrión
	 * @param idAlojamiento alojamiento a filtrar; {@code null} para todos
	 * @param pagina página y orden
	 * @return el resumen; o NO_ENCONTRADO si el alojamiento no existe o es de
	 *         otro anfitrión
	 */
	public Resultado<ResumenResenasDTO> resumenDeAnfitrion(Long idAnfitrion, Long idAlojamiento, Pageable pagina) {
		if (idAlojamiento != null && alojamientoRepo.findById(idAlojamiento)
				.filter(a -> a.getIdAnfitrion().equals(idAnfitrion)).isEmpty()) {
			return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe.");
		}
		Double promedio = resenaRepo.promedioDeAnfitrion(idAnfitrion, idAlojamiento, EstadoResena.PUBLICADA);
		ResumenResenasDTO resumen = new ResumenResenasDTO();
		resumen.setIdAlojamiento(idAlojamiento);
		resumen.setPromedio(redondear(promedio));
		resumen.setCantidad(resenaRepo.contarDeAnfitrion(idAnfitrion, idAlojamiento, EstadoResena.PUBLICADA));
		resumen.setResenas(
				PaginaDTO.de(resenaRepo.buscarDeAnfitrion(idAnfitrion, idAlojamiento, EstadoResena.PUBLICADA, pagina),
						this::convertirDTO));
		return Resultado.exito(resumen);
	}

	/**
	 * Todas las reseñas, o solo las de un estado (moderación del
	 * administrador).
	 *
	 * @param estado estado a filtrar; {@code null} para todas
	 * @param pagina página y orden
	 * @return página de reseñas
	 */
	public PaginaDTO<ResenaDTO> listarTodas(EstadoResena estado, Pageable pagina) {
		return PaginaDTO.de(estado == null ? resenaRepo.findAll(pagina) : resenaRepo.findByEstado(estado, pagina),
				this::convertirDTO);
	}

	/**
	 * Oculta o vuelve a mostrar una reseña (administrador). No la borra. Al
	 * ocultarla se notifica al autor con el motivo.
	 *
	 * @param idResena reseña
	 * @param oculta {@code true} para ocultar, {@code false} para mostrar
	 * @param motivo motivo (obligatorio al ocultar)
	 * @param idAdmin administrador que modera
	 * @return la reseña; NO_ENCONTRADO si no existe; DATOS_INVALIDOS si falta
	 *         el motivo
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
		if (oculta) {
			notificacionService.enviar(guardada.getIdUsuario(), NotificacionService.RESENA_OCULTA,
					"Un administrador ocultó tu reseña de la reserva " + guardada.getIdReserva() + ". Motivo: "
							+ motivo.trim());
		}
		auditoriaService.registrar(idAdmin, AuditoriaService.RESENA_MODERADA, AuditoriaService.ENTIDAD_RESENA, idResena,
				ResultadoAuditoria.EXITOSO, (oculta ? "Ocultada: " + motivo.trim() : "Visible de nuevo"));
		return Resultado.exito(convertirDTO(guardada));
	}

	/**
	 * Convierte la reseña a DTO con el nombre del huésped y el título del
	 * alojamiento.
	 *
	 * @param resena reseña
	 * @return reseña para la API
	 */
	public ResenaDTO convertirDTO(Resena resena) {
		ResenaDTO dto = modelMapper.map(resena, ResenaDTO.class);
		usuarioRepo.findById(resena.getIdUsuario())
				.ifPresent(u -> dto.setNombreUsuario(u.getNombre()));
		alojamientoRepo.findById(resena.getIdAlojamiento()).ifPresent(a -> dto.setTituloAlojamiento(a.getTitulo()));
		return dto;
	}

	/**
	 * Redondea el promedio a una cifra decimal.
	 *
	 * @param promedio promedio; puede ser {@code null}
	 * @return promedio redondeado, o {@code null}
	 */
	private static Double redondear(Double promedio) {
		return promedio == null ? null : BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Registra en la auditoría una reseña rechazada y arma el error.
	 *
	 * @param idHuesped huésped que intentó reseñar
	 * @param mensaje causa del rechazo
	 * @return resultado con ESTADO_INVALIDO
	 */
	private Resultado<ResenaDTO> rechazar(Long idHuesped, String mensaje) {
		auditoriaService.registrar(idHuesped, AuditoriaService.RESENA_CREADA, AuditoriaService.ENTIDAD_RESENA, null,
				ResultadoAuditoria.RECHAZADO, mensaje);
		return Resultado.error(Resultado.ESTADO_INVALIDO, mensaje);
	}
}
