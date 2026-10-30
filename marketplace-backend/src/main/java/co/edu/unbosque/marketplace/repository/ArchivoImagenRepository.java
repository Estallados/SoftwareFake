package co.edu.unbosque.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.ArchivoImagen;

/**
 * Repositorio de {@link ArchivoImagen}; la clave es la ruta del archivo.
 *
 * @see "RF-017"
 */
public interface ArchivoImagenRepository extends JpaRepository<ArchivoImagen, String> {
}
