package co.edu.unbosque.marketplace.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.PeriodoDisponibilidad;

/**
 * Acceso a los períodos de disponibilidad de los alojamientos.
 */
public interface PeriodoDisponibilidadRepository extends JpaRepository<PeriodoDisponibilidad, Long> {

	/**
	 * Períodos de un alojamiento, ordenados por fecha de inicio.
	 *
	 * @param idAlojamiento alojamiento a consultar
	 * @return lista de períodos
	 */
	public List<PeriodoDisponibilidad> findByIdAlojamientoOrderByFechaInicio(Long idAlojamiento);

	/**
	 * Indica si el alojamiento tiene algún período con al menos una noche
	 * después de una fecha.
	 *
	 * @param idAlojamiento alojamiento
	 * @param fecha fecha de referencia (normalmente hoy)
	 * @return {@code true} si hay disponibilidad futura
	 */
	public boolean existsByIdAlojamientoAndFechaFinAfter(Long idAlojamiento, LocalDate fecha);

	/**
	 * Indica si algún período ofrecido contiene la estancia [llegada, salida).
	 *
	 * @param idAlojamiento alojamiento
	 * @param llegada día de llegada
	 * @param salida día de salida
	 * @return {@code true} si la estancia cabe en un período
	 */
	@Query("select count(p) > 0 from PeriodoDisponibilidad p where p.idAlojamiento = :id "
			+ "and p.fechaInicio <= :llegada and p.fechaFin >= :salida")
	public boolean contiene(@Param("id") Long idAlojamiento, @Param("llegada") LocalDate llegada,
			@Param("salida") LocalDate salida);

	/**
	 * Indica si algún período del alojamiento se cruza con [inicio, fin], sin
	 * contar el período indicado. Evita que el anfitrión cree períodos
	 * solapados.
	 *
	 * @param idAlojamiento alojamiento
	 * @param inicio inicio del período nuevo
	 * @param fin fin del período nuevo
	 * @param excluir período que se está editando (no cuenta como cruce);
	 *                {@code null} al crear
	 * @return {@code true} si hay cruce
	 */
	@Query("select count(p) > 0 from PeriodoDisponibilidad p where p.idAlojamiento = :id "
			+ "and p.fechaInicio <= :fin and p.fechaFin >= :inicio and (:excluir is null or p.idPeriodo <> :excluir)")
	public boolean existeCruce(@Param("id") Long idAlojamiento, @Param("inicio") LocalDate inicio,
			@Param("fin") LocalDate fin, @Param("excluir") Long excluir);
}
