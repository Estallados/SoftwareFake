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
 * Acceso a los alojamientos. La búsqueda pública usa especificaciones de JPA
 * ({@link AlojamientoEspecificacion}) para combinar solo los filtros que mande
 * el usuario.
 */
public interface AlojamientoRepository extends JpaRepository<Alojamiento, Long>, JpaSpecificationExecutor<Alojamiento> {

	/**
	 * Publicaciones de un anfitrión, paginadas.
	 *
	 * @param idAnfitrion anfitrión dueño
	 * @param pageable página y orden
	 * @return página de alojamientos
	 */
	public Page<Alojamiento> findByIdAnfitrion(Long idAnfitrion, Pageable pageable);

	/**
	 * Publicaciones en un estado, por ejemplo la cola de revisión del
	 * administrador.
	 *
	 * @param estado estado a filtrar
	 * @param pageable página y orden
	 * @return página de alojamientos
	 */
	public Page<Alojamiento> findByEstado(EstadoAlojamiento estado, Pageable pageable);

	/**
	 * Cantidad de publicaciones en un estado.
	 *
	 * @param estado estado a contar
	 * @return cantidad
	 */
	public long countByEstado(EstadoAlojamiento estado);

	/**
	 * Cantidad de alojamientos por estado, para el dashboard.
	 *
	 * @return filas con la forma [estado, cantidad]
	 */
	@Query("select a.estado, count(a) from Alojamiento a group by a.estado")
	public List<Object[]> contarPorEstado();

	/**
	 * Cantidad de alojamientos por tipo, en orden alfabético, para los filtros
	 * de la página de inicio.
	 *
	 * @return filas con la forma [tipo, cantidad]
	 */
	@Query("select a.tipo, count(a) from Alojamiento a where a.tipo is not null group by a.tipo order by a.tipo")
	public List<Object[]> contarPorTipo();

	/**
	 * Lee el alojamiento con bloqueo pesimista de escritura. Así dos reservas
	 * que llegan al mismo tiempo sobre el mismo alojamiento se procesan una
	 * después de la otra y no pueden ocupar las mismas fechas.
	 *
	 * @param id alojamiento a bloquear
	 * @return el alojamiento, si existe
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Alojamiento a where a.idAlojamiento = :id")
	public Optional<Alojamiento> buscarParaReservar(@Param("id") Long id);
}
