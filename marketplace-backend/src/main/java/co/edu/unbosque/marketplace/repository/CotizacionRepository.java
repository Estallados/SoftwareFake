package co.edu.unbosque.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Cotizacion;

/**
 * Repositorio de {@link Cotizacion}.
 *
 * @see "RF-021"
 */
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
}
