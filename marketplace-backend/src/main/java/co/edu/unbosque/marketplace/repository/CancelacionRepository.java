package co.edu.unbosque.marketplace.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.edu.unbosque.marketplace.entity.Cancelacion;

/**
 * Repositorio de {@link Cancelacion}.
 *
 * @see "RF-002"
 */
public interface CancelacionRepository extends JpaRepository<Cancelacion, Long> {

    public Optional<Cancelacion> findByIdReserva(Long idReserva);

    @Query("select coalesce(sum(c.valorReembolso), 0) from Cancelacion c")
    public BigDecimal sumarReembolsos();
}
