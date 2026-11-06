package co.edu.unbosque.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.RegistroExportacion;

/**
 * Acceso al historial de exportaciones de reportes. Solo usa las operaciones
 * básicas de {@link JpaRepository}.
 */
public interface RegistroExportacionRepository extends JpaRepository<RegistroExportacion, Long> {
}
