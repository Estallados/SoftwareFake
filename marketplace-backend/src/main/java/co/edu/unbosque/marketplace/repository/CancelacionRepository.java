package co.edu.unbosque.marketplace.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.edu.unbosque.marketplace.entity.Cancelacion;

/**
 * Acceso a las cancelaciones de reservas.
 */
public interface CancelacionRepository extends JpaRepository<Cancelacion, Long> {

	/**
	 * Cancelación de una reserva.
	 *
	 * @param idReserva reserva a consultar
	 * @return la cancelación, si la reserva fue cancelada
	 */
	public Optional<Cancelacion> findByIdReserva(Long idReserva);

	/**
	 * Suma de todo lo devuelto por cancelaciones, para el reporte de ingresos.
	 *
	 * @return total devuelto; 0 si no hay cancelaciones
	 */
	@Query("select coalesce(sum(c.valorReembolso), 0) from Cancelacion c")
	public BigDecimal sumarReembolsos();
}
