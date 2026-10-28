package co.edu.unbosque.marketplace.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.EstadoPago;
import co.edu.unbosque.marketplace.entity.Pago;

/**
 * Repositorio de {@link Pago}.
 *
 * @see "RF-028"
 */
public interface PagoRepository extends JpaRepository<Pago, Long> {

	public Optional<Pago> findTopByIdReservaOrderByFechaDescIdPagoDesc(Long idReserva);

	@Query("select coalesce(sum(p.valor), 0) from Pago p where p.estado in :estados")
	public BigDecimal sumarValores(@Param("estados") Collection<EstadoPago> estados);
}
