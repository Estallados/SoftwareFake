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

    public boolean existsByIdReserva(Long idReserva);

    public Page<Resena> findByIdAlojamientoAndEstado(Long idAlojamiento, EstadoResena estado, Pageable pageable);

    @Query("select avg(r.calificacion) from Resena r where r.idAlojamiento = :id and r.estado = :estado")
    public Double promedio(@Param("id") Long idAlojamiento, @Param("estado") EstadoResena estado);

    public long countByIdAlojamientoAndEstado(Long idAlojamiento, EstadoResena estado);
}
