package co.edu.unbosque.marketplace.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.PeriodoDisponibilidad;

/**
 * Repositorio de {@link PeriodoDisponibilidad}.
 *
 * @see "RF-025, RF-026"
 */
public interface PeriodoDisponibilidadRepository extends JpaRepository<PeriodoDisponibilidad, Long> {

	public List<PeriodoDisponibilidad> findByIdAlojamientoOrderByFechaInicio(Long idAlojamiento);

	/** Indica si algún período ofrecido contiene la estancia [llegada, salida). */
	@Query("select count(p) > 0 from PeriodoDisponibilidad p where p.idAlojamiento = :id "
			+ "and p.fechaInicio <= :llegada and p.fechaFin >= :salida")
	public boolean contiene(@Param("id") Long idAlojamiento, @Param("llegada") LocalDate llegada,
			@Param("salida") LocalDate salida);

	/**
	 * Períodos del alojamiento que se cruzan con [inicio, fin], excepto el
	 * indicado.
	 */
	@Query("select count(p) > 0 from PeriodoDisponibilidad p where p.idAlojamiento = :id "
			+ "and p.fechaInicio <= :fin and p.fechaFin >= :inicio and (:excluir is null or p.idPeriodo <> :excluir)")
	public boolean existeCruce(@Param("id") Long idAlojamiento, @Param("inicio") LocalDate inicio,
			@Param("fin") LocalDate fin, @Param("excluir") Long excluir);
}
