package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Indicadores del dashboard administrativo.
 *
 * @see "RF-005"
 */
public class DashboardDTO {
    private Long totalUsuarios;
    private Long alojamientosActivos;
    private Map<String, Long> reservasPorEstado;
    private BigDecimal ingresosEstimados;
    private List<DemandaCiudadDTO> ciudadesMayorDemanda;

    public DashboardDTO() {
    }

    public Long getTotalUsuarios() {
        return totalUsuarios;
    }
    public void setTotalUsuarios(Long totalUsuarios) {
        this.totalUsuarios = totalUsuarios;
    }

    public Long getAlojamientosActivos() {
        return alojamientosActivos;
    }
    public void setAlojamientosActivos(Long alojamientosActivos) {
        this.alojamientosActivos = alojamientosActivos;
    }

    public Map<String, Long> getReservasPorEstado() {
        return reservasPorEstado;
    }
    public void setReservasPorEstado(Map<String, Long> reservasPorEstado) {
        this.reservasPorEstado = reservasPorEstado;
    }

    public BigDecimal getIngresosEstimados() {
        return ingresosEstimados;
    }
    public void setIngresosEstimados(BigDecimal ingresosEstimados) {
        this.ingresosEstimados = ingresosEstimados;
    }

    public List<DemandaCiudadDTO> getCiudadesMayorDemanda() {
        return ciudadesMayorDemanda;
    }
    public void setCiudadesMayorDemanda(List<DemandaCiudadDTO> ciudadesMayorDemanda) {
        this.ciudadesMayorDemanda = ciudadesMayorDemanda;
    }
}
