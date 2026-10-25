package co.edu.unbosque.marketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.EstadoResena;
import co.edu.unbosque.marketplace.entity.Resena;

/**
 * Repositorio de {@link Resena}.
 *
 * @see "RF-004, RF-016"
 */
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    /** Reseñas de los alojamientos de un anfitrión, opcionalmente de uno solo. */
    String DE_ANFITRION = "r.estado = :estado and r.idAlojamiento in (select a.idAlojamiento from Alojamiento a "
            + "where a.idAnfitrion = :idAnfitrion) and (:idAlojamiento is null or r.idAlojamiento = :idAlojamiento)";

    public boolean existsByIdReserva(Long idReserva);

    public Page<Resena> findByIdAlojamientoAndEstado(Long idAlojamiento, EstadoResena estado, Pageable pageable);

    @Query("select avg(r.calificacion) from Resena r where r.idAlojamiento = :id and r.estado = :estado")
    public Double promedio(@Param("id") Long idAlojamiento, @Param("estado") EstadoResena estado);

    public long countByIdAlojamientoAndEstado(Long idAlojamiento, EstadoResena estado);

    /** Reseñas escritas por un huésped, en cualquier estado. */
    public Page<Resena> findByIdUsuario(Long idUsuario, Pageable pageable);

    /** Reseñas en un estado (moderación del administrador). */
    public Page<Resena> findByEstado(EstadoResena estado, Pageable pageable);

    @Query("select r from Resena r where " + DE_ANFITRION)
    public Page<Resena> buscarDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion,
            @Param("idAlojamiento") Long idAlojamiento, @Param("estado") EstadoResena estado, Pageable pageable);

    @Query("select avg(r.calificacion) from Resena r where " + DE_ANFITRION)
    public Double promedioDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion,
            @Param("idAlojamiento") Long idAlojamiento, @Param("estado") EstadoResena estado);

    @Query("select count(r) from Resena r where " + DE_ANFITRION)
    public long contarDeAnfitrion(@Param("idAnfitrion") Long idAnfitrion,
            @Param("idAlojamiento") Long idAlojamiento, @Param("estado") EstadoResena estado);
}
