package co.edu.unbosque.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Cotizacion;

/**
 * Acceso a la tabla de cotizaciones. Solo usa las operaciones básicas de
 * {@link JpaRepository}.
 */
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
}
