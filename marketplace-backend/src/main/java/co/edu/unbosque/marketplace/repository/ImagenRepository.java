package co.edu.unbosque.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import co.edu.unbosque.marketplace.entity.Imagen;

/**
 * Repositorio de {@link Imagen}.
 *
 * @see "RF-017"
 */
public interface ImagenRepository extends JpaRepository<Imagen, Long> {

    public List<Imagen> findByIdAlojamientoOrderByIdImagen(Long idAlojamiento);

    @Modifying
    public void deleteByIdAlojamiento(Long idAlojamiento);
}
