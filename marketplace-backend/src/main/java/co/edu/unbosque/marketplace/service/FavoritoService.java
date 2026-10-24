package co.edu.unbosque.marketplace.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.FavoritoDTO;
import co.edu.unbosque.marketplace.dto.PaginaDTO;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.Favorito;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.FavoritoRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Alojamientos favoritos de cada usuario, sin duplicados.
 *
 * @see "RF-007, CA-170, CA-171, CA-172, CA-173, CA-198"
 */
@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepo;
    private final AlojamientoRepository alojamientoRepo;
    private final PublicacionService publicacionService;
    private final ModelMapper modelMapper;

    public FavoritoService(FavoritoRepository favoritoRepo, AlojamientoRepository alojamientoRepo,
            PublicacionService publicacionService, ModelMapper modelMapper) {
        this.favoritoRepo = favoritoRepo;
        this.alojamientoRepo = alojamientoRepo;
        this.publicacionService = publicacionService;
        this.modelMapper = modelMapper;
    }

    /**
     * Agrega un alojamiento ACTIVA a los favoritos. Si ya estaba, se conserva
     * el favorito existente.
     *
     * @return 0 con el favorito; 1 si el alojamiento no existe o no está publicado.
     * @see "CA-172, CA-173"
     */
    @Transactional
    public Resultado<FavoritoDTO> agregar(Long idUsuario, Long idAlojamiento) {
        if (alojamientoRepo.findById(idAlojamiento).filter(a -> a.getEstado() == EstadoAlojamiento.ACTIVA)
                .isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe.");
        }
        Optional<Favorito> existente = favoritoRepo.findByIdUsuarioAndIdAlojamiento(idUsuario, idAlojamiento);
        if (existente.isPresent()) {
            return Resultado.exito(convertirDTO(existente.get()));
        }
        Favorito favorito = new Favorito();
        favorito.setIdUsuario(idUsuario);
        favorito.setIdAlojamiento(idAlojamiento);
        favorito.setFechaAgregado(LocalDateTime.now());
        try {
            return Resultado.exito(convertirDTO(favoritoRepo.saveAndFlush(favorito)));
        } catch (DataIntegrityViolationException e) {
            // Dos solicitudes iguales al mismo tiempo: el índice único deja solo una
            return favoritoRepo.findByIdUsuarioAndIdAlojamiento(idUsuario, idAlojamiento)
                    .map(f -> Resultado.exito(convertirDTO(f)))
                    .orElseGet(() -> Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no existe."));
        }
    }

    /**
     * Quita un alojamiento de los favoritos del usuario.
     *
     * @return 0 eliminado; 1 si no estaba en sus favoritos.
     * @see "CA-171"
     */
    @Transactional
    public Resultado<Void> eliminar(Long idUsuario, Long idAlojamiento) {
        Optional<Favorito> favorito = favoritoRepo.findByIdUsuarioAndIdAlojamiento(idUsuario, idAlojamiento);
        if (favorito.isEmpty()) {
            return Resultado.error(Resultado.NO_ENCONTRADO, "El alojamiento no está en tus favoritos.");
        }
        favoritoRepo.delete(favorito.get());
        return Resultado.exito(null);
    }

    /**
     * Favoritos del usuario autenticado (solo los suyos); vacía si no tiene.
     *
     * @see "CA-170, CA-198"
     */
    public PaginaDTO<FavoritoDTO> listar(Long idUsuario, Pageable pagina) {
        return PaginaDTO.de(favoritoRepo.findByIdUsuario(idUsuario, pagina), this::convertirDTO);
    }

    private FavoritoDTO convertirDTO(Favorito favorito) {
        FavoritoDTO dto = modelMapper.map(favorito, FavoritoDTO.class);
        alojamientoRepo.findById(favorito.getIdAlojamiento())
                .ifPresent(a -> dto.setAlojamiento(publicacionService.convertirDTO(a)));
        return dto;
    }
}
