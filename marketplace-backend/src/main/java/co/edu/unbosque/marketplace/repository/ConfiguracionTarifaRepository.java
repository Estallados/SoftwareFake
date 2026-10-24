package co.edu.unbosque.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.ConfiguracionTarifa;

/**
 * Repositorio de {@link ConfiguracionTarifa}. La configuración vigente es la
 * más reciente.
 *
 * @see "RF-023"
 */
public interface ConfiguracionTarifaRepository extends JpaRepository<ConfiguracionTarifa, Long> {

    public Optional<ConfiguracionTarifa> findTopByOrderByFechaDescIdConfiguracionDesc();
}
