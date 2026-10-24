package co.edu.unbosque.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.RegistroExportacion;

/**
 * Repositorio de {@link RegistroExportacion}.
 *
 * @see "RF-006, CA-029"
 */
public interface RegistroExportacionRepository extends JpaRepository<RegistroExportacion, Long> {
}
