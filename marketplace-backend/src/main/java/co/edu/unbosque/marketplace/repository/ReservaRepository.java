package co.edu.unbosque.marketplace.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.Reserva;

/**
 * Repositorio de {@link Reserva}. Una reserva "ocupa" fechas si está en un
 * estado activo, o si está PENDIENTE_PAGO y se creó después del límite (las
 * pendientes viejas se consideran abandonadas).
 *
 * @see "RF-020, RF-022, RNF-028"
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	String OCUPA = "(r.estado in :activos or (r.estado = co.edu.unbosque.marketplace.entity.EstadoReserva.PENDIENTE_PAGO "
			+ "and r.fechaCreacion >= :limite))";

	/** Indica si alguna reserva que ocupa fechas se cruza con [llegada, salida). */
	@Query("select count(r) > 0 from Reserva r where r.idAlojamiento = :id and r.fechaEntrada < :salida "
			+ "and r.fechaSalida > :llegada and " + OCUPA)
	public boolean existeCruce(@Param("id") Long idAlojamiento, @Param("llegada") LocalDate llegada,
			@Param("salida") LocalDate salida, @Param("activos") Collection<EstadoReserva> activos,
			@Param("limite") LocalDateTime limite);

	/** Reservas que ocupan fechas de un alojamiento (calendario). */
	@Query("select r from Reserva r where r.idAlojamiento = :id and r.fechaSalida > :desde and " + OCUPA
			+ " order by r.fechaEntrada")
	public List<Reserva> buscarOcupadas(@Param("id") Long idAlojamiento, @Param("desde") LocalDate desde,
			@Param("activos") Collection<EstadoReserva> activos, @Param("limite") LocalDateTime limite);

	public Page<Reserva> findByIdHuesped(Long idHuesped, Pageable pageable);

	/** Todas las reservas en un estado (vista del administrador). */
	public Page<Reserva> findByEstado(EstadoReserva estado, Pageable pageable);

	/** Cantidad de reservas de un alojamiento en los estados indicados. */
	public long countByIdAlojamientoAndEstadoIn(Long idAlojamiento, Collection<EstadoReserva> estados);

	/** Reservas de los alojamientos de un anfitrión. */
	@Query("select r from Reserva r where r.idAlojamiento in "
			+ "(select a.idAlojamiento from Alojamiento a where a.idAnfitrion = :idAnfitrion)")
	public Page<Reserva> buscarDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion, Pageable pageable);

	/**
	 * Reservas futuras de un alojamiento en los estados indicados (bloqueo
	 * administrativo).
	 */
	public List<Reserva> findByIdAlojamientoAndFechaEntradaAfterAndEstadoIn(Long idAlojamiento, LocalDate fecha,
			Collection<EstadoReserva> estados);

	/** Reservas PENDIENTE_PAGO creadas antes del límite (abandonadas). */
	public List<Reserva> findByEstadoAndFechaCreacionBefore(EstadoReserva estado, LocalDateTime limite);

	@Query("select r.estado, count(r) from Reserva r group by r.estado")
	public List<Object[]> contarPorEstado();

	@Query("select a.ciudad, count(r) from Reserva r, Alojamiento a where r.idAlojamiento = a.idAlojamiento "
			+ "and r.estado <> co.edu.unbosque.marketplace.entity.EstadoReserva.CANCELADA "
			+ "group by a.ciudad order by count(r) desc")
	public List<Object[]> contarPorCiudad();

	@Query("select coalesce(sum(r.total), 0) from Reserva r where r.estado in :estados")
	public BigDecimal sumarTotales(@Param("estados") Collection<EstadoReserva> estados);
}
