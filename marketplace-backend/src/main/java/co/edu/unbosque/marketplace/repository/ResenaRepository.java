package co.edu.unbosque.marketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.EstadoResena;
import co.edu.unbosque.marketplace.entity.Resena;

/**
 * Acceso a las reseñas. Las consultas "de anfitrión" juntan las reseñas de
 * todos sus alojamientos, o de uno solo si se indica.
 */
public interface ResenaRepository extends JpaRepository<Resena, Long> {

	/** Reseñas de los alojamientos de un anfitrión, opcionalmente de uno solo. */
	String DE_ANFITRION = "r.estado = :estado and r.idAlojamiento in (select a.idAlojamiento from Alojamiento a "
			+ "where a.idAnfitrion = :idAnfitrion) and (:idAlojamiento is null or r.idAlojamiento = :idAlojamiento)";

	/**
	 * Indica si una reserva ya tiene reseña (solo se permite una por reserva).
	 *
	 * @param idReserva reserva
	 * @return {@code true} si ya fue reseñada
	 */
	public boolean existsByIdReserva(Long idReserva);

	/**
	 * Reseñas de un alojamiento en un estado, paginadas.
	 *
	 * @param idAlojamiento alojamiento
	 * @param estado estado de las reseñas (normalmente PUBLICADA)
	 * @param pageable página y orden
	 * @return página de reseñas
	 */
	public Page<Resena> findByIdAlojamientoAndEstado(Long idAlojamiento, EstadoResena estado, Pageable pageable);

	/**
	 * Calificación promedio de un alojamiento, contando solo las reseñas en el
	 * estado indicado.
	 *
	 * @param idAlojamiento alojamiento
	 * @param estado estado de las reseñas que cuentan
	 * @return promedio, o {@code null} si no hay reseñas
	 */
	@Query("select avg(r.calificacion) from Resena r where r.idAlojamiento = :id and r.estado = :estado")
	public Double promedio(@Param("id") Long idAlojamiento, @Param("estado") EstadoResena estado);

	/**
	 * Cantidad de reseñas de un alojamiento en un estado.
	 *
	 * @param idAlojamiento alojamiento
	 * @param estado estado de las reseñas
	 * @return cantidad
	 */
	public long countByIdAlojamientoAndEstado(Long idAlojamiento, EstadoResena estado);

	/**
	 * Reseñas escritas por un huésped, en cualquier estado.
	 *
	 * @param idUsuario huésped
	 * @param pageable página y orden
	 * @return página de reseñas
	 */
	public Page<Resena> findByIdUsuario(Long idUsuario, Pageable pageable);

	/**
	 * Reseñas en un estado, para la moderación del administrador.
	 *
	 * @param estado estado a filtrar
	 * @param pageable página y orden
	 * @return página de reseñas
	 */
	public Page<Resena> findByEstado(EstadoResena estado, Pageable pageable);

	/**
	 * Reseñas de los alojamientos de un anfitrión, o de uno solo.
	 *
	 * @param idAnfitrion anfitrión
	 * @param idAlojamiento alojamiento a filtrar; {@code null} para todos
	 * @param estado estado de las reseñas
	 * @param pageable página y orden
	 * @return página de reseñas
	 */
	@Query("select r from Resena r where " + DE_ANFITRION)
	public Page<Resena> buscarDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion,
			@Param("idAlojamiento") Long idAlojamiento, @Param("estado") EstadoResena estado, Pageable pageable);

	/**
	 * Calificación promedio de los alojamientos de un anfitrión, o de uno
	 * solo.
	 *
	 * @param idAnfitrion anfitrión
	 * @param idAlojamiento alojamiento a filtrar; {@code null} para todos
	 * @param estado estado de las reseñas que cuentan
	 * @return promedio, o {@code null} si no hay reseñas
	 */
	@Query("select avg(r.calificacion) from Resena r where " + DE_ANFITRION)
	public Double promedioDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion,
			@Param("idAlojamiento") Long idAlojamiento, @Param("estado") EstadoResena estado);

	/**
	 * Cantidad de reseñas de los alojamientos de un anfitrión, o de uno solo.
	 *
	 * @param idAnfitrion anfitrión
	 * @param idAlojamiento alojamiento a filtrar; {@code null} para todos
	 * @param estado estado de las reseñas
	 * @return cantidad
	 */
	@Query("select count(r) from Resena r where " + DE_ANFITRION)
	public long contarDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion, @Param("idAlojamiento") Long idAlojamiento,
			@Param("estado") EstadoResena estado);
}
