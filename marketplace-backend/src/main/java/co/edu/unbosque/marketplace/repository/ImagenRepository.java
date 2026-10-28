package co.edu.unbosque.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Imagen;

/**
 * Repositorio de {@link Imagen}.
 *
 * @see "RF-017"
 */
public interface ImagenRepository extends JpaRepository<Imagen, Long> {

	public List<Imagen> findByIdAlojamientoOrderByIdImagen(Long idAlojamiento);

	/**
	 * Imagen de un alojamiento concreto (evita tocar imágenes de otro alojamiento).
	 */
	public Optional<Imagen> findByIdImagenAndIdAlojamiento(Long idImagen, Long idAlojamiento);
}
