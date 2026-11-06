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

import co.edu.unbosque.marketplace.dto.DemandaCiudadDTO;
import co.edu.unbosque.marketplace.dto.ReservaAdminDTO;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.Reserva;

/**
 * Acceso a las reservas.
 * <p>
 * Una reserva "ocupa" fechas si está en un estado activo, o si está
 * PENDIENTE_PAGO y se creó después del límite: las pendientes más viejas se
 * consideran abandonadas y ya no bloquean el calendario.
 * </p>
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	/**
	 * Condición JPQL de una reserva que ocupa fechas: está en un estado
	 * activo, o está PENDIENTE_PAGO y se creó después del límite.
	 */
	String OCUPA = "(r.estado in :activos or (r.estado = co.edu.unbosque.marketplace.entity.EstadoReserva.PENDIENTE_PAGO "
			+ "and r.fechaCreacion >= :limite))";

	/**
	 * Indica si alguna reserva que ocupa fechas se cruza con la estancia
	 * [llegada, salida).
	 *
	 * @param idAlojamiento alojamiento
	 * @param llegada día de llegada
	 * @param salida día de salida
	 * @param activos estados que ocupan fechas
	 * @param limite fecha de creación mínima para que una reserva pendiente
	 *               cuente
	 * @return {@code true} si hay cruce
	 */
	@Query("select count(r) > 0 from Reserva r where r.idAlojamiento = :id and r.fechaEntrada < :salida "
			+ "and r.fechaSalida > :llegada and " + OCUPA)
	public boolean existeCruce(@Param("id") Long idAlojamiento, @Param("llegada") LocalDate llegada,
			@Param("salida") LocalDate salida, @Param("activos") Collection<EstadoReserva> activos,
			@Param("limite") LocalDateTime limite);

	/**
	 * Reservas que ocupan fechas de un alojamiento desde un día, para pintar
	 * el calendario.
	 *
	 * @param idAlojamiento alojamiento
	 * @param desde fecha desde la que se consulta
	 * @param activos estados que ocupan fechas
	 * @param limite fecha de creación mínima para que una reserva pendiente
	 *               cuente
	 * @return reservas ordenadas por fecha de entrada
	 */
	@Query("select r from Reserva r where r.idAlojamiento = :id and r.fechaSalida > :desde and " + OCUPA
			+ " order by r.fechaEntrada")
	public List<Reserva> buscarOcupadas(@Param("id") Long idAlojamiento, @Param("desde") LocalDate desde,
			@Param("activos") Collection<EstadoReserva> activos, @Param("limite") LocalDateTime limite);

	/**
	 * Reservas de un huésped, paginadas.
	 *
	 * @param idHuesped huésped
	 * @param pageable página y orden
	 * @return página de reservas
	 */
	public Page<Reserva> findByIdHuesped(Long idHuesped, Pageable pageable);

	/**
	 * Cantidad de reservas de un alojamiento en los estados indicados.
	 *
	 * @param idAlojamiento alojamiento
	 * @param estados estados a contar
	 * @return cantidad
	 */
	public long countByIdAlojamientoAndEstadoIn(Long idAlojamiento, Collection<EstadoReserva> estados);

	/**
	 * Reservas de todos los alojamientos de un anfitrión, paginadas.
	 *
	 * @param idAnfitrion anfitrión
	 * @param pageable página y orden
	 * @return página de reservas
	 */
	@Query("select r from Reserva r where r.idAlojamiento in "
			+ "(select a.idAlojamiento from Alojamiento a where a.idAnfitrion = :idAnfitrion)")
	public Page<Reserva> buscarDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion, Pageable pageable);

	/**
	 * Reservas futuras de un alojamiento en los estados indicados. Se usa al
	 * bloquear un alojamiento para pasar sus reservas a revisión.
	 *
	 * @param idAlojamiento alojamiento
	 * @param fecha fecha a partir de la cual se buscan las llegadas
	 * @param estados estados a incluir
	 * @return lista de reservas
	 */
	public List<Reserva> findByIdAlojamientoAndFechaEntradaAfterAndEstadoIn(Long idAlojamiento, LocalDate fecha,
			Collection<EstadoReserva> estados);

	/**
	 * Reservas en un estado creadas antes de un límite; sirve para encontrar
	 * las PENDIENTE_PAGO abandonadas.
	 *
	 * @param estado estado a filtrar
	 * @param limite fecha de creación máxima
	 * @return lista de reservas
	 */
	public List<Reserva> findByEstadoAndFechaCreacionBefore(EstadoReserva estado, LocalDateTime limite);

	/**
	 * Cantidad de reservas por estado, para el dashboard.
	 *
	 * @return filas con la forma [estado, cantidad]
	 */
	@Query("select r.estado, count(r) from Reserva r group by r.estado")
	public List<Object[]> contarPorEstado();

	/**
	 * Cantidad de reservas no canceladas por ciudad, de mayor a menor, para el
	 * reporte de demanda.
	 *
	 * @return filas con la forma [ciudad, cantidad]
	 */
	@Query("select a.ciudad, count(r) from Reserva r, Alojamiento a where r.idAlojamiento = a.idAlojamiento "
			+ "and r.estado <> co.edu.unbosque.marketplace.entity.EstadoReserva.CANCELADA "
			+ "group by a.ciudad order by count(r) desc")
	public List<Object[]> contarPorCiudad();

	/**
	 * Ciudades con más reservas, contando todos los estados: de mayor a menor
	 * y, si hay empate, por nombre. El {@code Pageable} limita cuántas se
	 * devuelven.
	 *
	 * @param limite cantidad máxima de ciudades
	 * @return ciudades con su cantidad de reservas
	 */
	@Query("select new co.edu.unbosque.marketplace.dto.DemandaCiudadDTO(coalesce(a.ciudad, 'Sin ciudad'), count(r)) "
			+ "from Reserva r join r.alojamiento a group by a.ciudad order by count(r) desc, a.ciudad asc")
	public List<DemandaCiudadDTO> contarCiudadesTop(Pageable limite);

	/**
	 * Cantidad de reservas en un estado con salida anterior a una fecha (por
	 * ejemplo, las confirmadas que ya terminaron).
	 *
	 * @param estado estado a contar
	 * @param fecha fecha de referencia
	 * @return cantidad
	 */
	public long countByEstadoAndFechaSalidaBefore(EstadoReserva estado, LocalDate fecha);

	/**
	 * Listado resumido de reservas para el administrador, en los estados
	 * indicados. Arma cada fila en una sola consulta por página, con los
	 * nombres del huésped, el alojamiento y el anfitrión.
	 *
	 * @param estados estados a incluir
	 * @param pageable página y orden
	 * @return página de filas resumidas
	 */
	@Query(value = "select new co.edu.unbosque.marketplace.dto.ReservaAdminDTO(r.idReserva, h.nombre, a.titulo, "
			+ "a.ciudad, an.nombre, r.fechaEntrada, r.fechaSalida, r.noches, r.total, r.estado, "
			+ "case when r.estado = co.edu.unbosque.marketplace.entity.EstadoReserva.CONFIRMADA "
			+ "and r.fechaSalida <= current_date then true else false end) "
			+ "from Reserva r left join r.huesped h left join r.alojamiento a left join a.anfitrion an "
			+ "where r.estado in :estados", countQuery = "select count(r) from Reserva r where r.estado in :estados")
	public Page<ReservaAdminDTO> listarResumenAdmin(@Param("estados") Collection<EstadoReserva> estados,
			Pageable pageable);

	/**
	 * Suma de los totales de las reservas en los estados indicados, para el
	 * reporte de ingresos.
	 *
	 * @param estados estados a incluir
	 * @return suma; 0 si no hay reservas
	 */
	@Query("select coalesce(sum(r.total), 0) from Reserva r where r.estado in :estados")
	public BigDecimal sumarTotales(@Param("estados") Collection<EstadoReserva> estados);
}
