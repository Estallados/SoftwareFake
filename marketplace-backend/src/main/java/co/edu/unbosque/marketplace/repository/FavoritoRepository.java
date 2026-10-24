package co.edu.unbosque.marketplace.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Favorito;

/**
 * Repositorio de {@link Favorito}.
 *
 * @see "RF-007"
 */
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    public Page<Favorito> findByIdUsuario(Long idUsuario, Pageable pageable);

    public Optional<Favorito> findByIdUsuarioAndIdAlojamiento(Long idUsuario, Long idAlojamiento);
}
