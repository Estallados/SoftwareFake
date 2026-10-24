package co.edu.unbosque.marketplace.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.marketplace.dto.ConfiguracionTarifaDTO;
import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.ConfiguracionTarifa;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;
import co.edu.unbosque.marketplace.repository.ConfiguracionTarifaRepository;
import co.edu.unbosque.marketplace.util.Resultado;

/**
 * Porcentajes de limpieza y servicio que se aplican al cotizar.
 * <p>
 * Orden de prioridad: el porcentaje propio del alojamiento (lo fija su
 * anfitrión); si no tiene, el global vigente (lo fija un administrador); si no
 * hay configuración guardada, los valores por defecto documentados:
 * {@value #LIMPIEZA_POR_DEFECTO} % de limpieza y {@value #SERVICIO_POR_DEFECTO} % de servicio.
 * </p>
 *
 * @see "RF-023, CA-084, CA-085, CA-180"
 */
@Service
public class TarifaService {

    /** Porcentaje de limpieza por defecto (CA-085). */
    public static final int LIMPIEZA_POR_DEFECTO = 5;
    /** Porcentaje de servicio por defecto (CA-085). */
    public static final int SERVICIO_POR_DEFECTO = 10;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ConfiguracionTarifaRepository configuracionRepo;
    private final AuditoriaService auditoriaService;

    public TarifaService(ConfiguracionTarifaRepository configuracionRepo, AuditoriaService auditoriaService) {
        this.configuracionRepo = configuracionRepo;
        this.auditoriaService = auditoriaService;
    }

    /** Configuración global vigente, o la de por defecto si no hay ninguna guardada. */
    public ConfiguracionTarifaDTO vigente() {
        Optional<ConfiguracionTarifa> guardada = configuracionRepo.findTopByOrderByFechaDescIdConfiguracionDesc();
        ConfiguracionTarifaDTO dto = new ConfiguracionTarifaDTO();
        if (guardada.isPresent()) {
            dto.setPorcentajeLimpieza(guardada.get().getPorcentajeLimpieza());
            dto.setPorcentajeServicio(guardada.get().getPorcentajeServicio());
            dto.setIdUsuario(guardada.get().getIdUsuario());
            dto.setFecha(guardada.get().getFecha());
            dto.setPorDefecto(false);
        } else {
            dto.setPorcentajeLimpieza(BigDecimal.valueOf(LIMPIEZA_POR_DEFECTO));
            dto.setPorcentajeServicio(BigDecimal.valueOf(SERVICIO_POR_DEFECTO));
            dto.setPorDefecto(true);
        }
        return dto;
    }

    /**
     * Cambia los porcentajes globales. Inserta una configuración nueva (se
     * conserva el historial); aplica a las cotizaciones siguientes.
     *
     * @return 0 con la configuración nueva; 2 si algún porcentaje falta o no
     *         está entre 0 y 100.
     */
    @Transactional
    public Resultado<ConfiguracionTarifaDTO> actualizar(ConfiguracionTarifaDTO datos, Long idActor) {
        if (datos == null || !porcentajeValido(datos.getPorcentajeLimpieza())
                || !porcentajeValido(datos.getPorcentajeServicio())) {
            return Resultado.error(Resultado.DATOS_INVALIDOS,
                    "Los porcentajes de limpieza y servicio son obligatorios y deben estar entre 0 y 100.");
        }
        ConfiguracionTarifa configuracion = new ConfiguracionTarifa();
        configuracion.setPorcentajeLimpieza(datos.getPorcentajeLimpieza());
        configuracion.setPorcentajeServicio(datos.getPorcentajeServicio());
        configuracion.setIdUsuario(idActor);
        configuracion.setFecha(LocalDateTime.now());
        configuracion = configuracionRepo.save(configuracion);

        auditoriaService.registrar(idActor, AuditoriaService.TARIFA_ACTUALIZADA, AuditoriaService.ENTIDAD_CONFIGURACION,
                configuracion.getIdConfiguracion(), ResultadoAuditoria.EXITOSO,
                "Limpieza " + datos.getPorcentajeLimpieza() + "%, servicio " + datos.getPorcentajeServicio() + "%");
        return Resultado.exito(vigente());
    }

    /** Porcentaje de limpieza que aplica a un alojamiento. */
    public BigDecimal porcentajeLimpieza(Alojamiento alojamiento) {
        return alojamiento.getPorcentajeLimpieza() != null ? alojamiento.getPorcentajeLimpieza()
                : vigente().getPorcentajeLimpieza();
    }

    /** Porcentaje de servicio que aplica a un alojamiento. */
    public BigDecimal porcentajeServicio(Alojamiento alojamiento) {
        return alojamiento.getPorcentajeServicio() != null ? alojamiento.getPorcentajeServicio()
                : vigente().getPorcentajeServicio();
    }

    /** Un porcentaje es válido si está entre 0 y 100. */
    public static boolean porcentajeValido(BigDecimal porcentaje) {
        return porcentaje != null && porcentaje.signum() >= 0 && porcentaje.compareTo(CIEN) <= 0;
    }
}
