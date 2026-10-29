package co.edu.unbosque.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import jakarta.persistence.LockModeType;

/**
 * Repositorio de {@link Alojamiento}. La búsqueda pública usa especificaciones
 * ({@link AlojamientoEspecificacion}).
 *
 * @see "RF-017, RF-019, RF-024"
 */
public interface AlojamientoRepository extends JpaRepository<Alojamiento, Long>, JpaSpecificationExecutor<Alojamiento> {

	/** Publicaciones de un anfitrión. */
	public Page<Alojamiento> findByIdAnfitrion(Long idAnfitrion, Pageable pageable);

	/** Publicaciones en un estado (cola de revisión del administrador). */
	public Page<Alojamiento> findByEstado(EstadoAlojamiento estado, Pageable pageable);

	/** Cantidad de publicaciones en un estado (dashboard). */
	public long countByEstado(EstadoAlojamiento estado);

	/**
	 * Cantidad de alojamientos por tipo, en orden alfabético. Cada fila es
	 * [tipo, cantidad].
	 */
	@Query("select a.tipo, count(a) from Alojamiento a where a.tipo is not null group by a.tipo order by a.tipo")
	public List<Object[]> contarPorTipo();

	/**
	 * Lee el alojamiento con bloqueo pesimista de escritura: dos reservas
	 * simultáneas sobre el mismo alojamiento se procesan una después de la otra.
	 *
	 * @see "RNF-028, RNF-029, CA-073"
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Alojamiento a where a.idAlojamiento = :id")
	public Optional<Alojamiento> buscarParaReservar(@Param("id") Long id);
}
