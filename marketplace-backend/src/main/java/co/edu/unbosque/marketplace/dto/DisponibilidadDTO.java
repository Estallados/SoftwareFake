package co.edu.unbosque.marketplace.dto;

import java.util.List;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;

/**
 * Calendario de un alojamiento: períodos ofrecidos y rangos ya reservados.
 *
 * @see "RF-026, CA-148"
 */
public class DisponibilidadDTO {
    private Long idAlojamiento;
    private EstadoAlojamiento estadoAlojamiento;
    private List<PeriodoDisponibilidadDTO> periodos;
    private List<RangoReservadoDTO> reservados;

    public DisponibilidadDTO() {
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public EstadoAlojamiento getEstadoAlojamiento() {
        return estadoAlojamiento;
    }
    public void setEstadoAlojamiento(EstadoAlojamiento estadoAlojamiento) {
        this.estadoAlojamiento = estadoAlojamiento;
    }

    public List<PeriodoDisponibilidadDTO> getPeriodos() {
        return periodos;
    }
    public void setPeriodos(List<PeriodoDisponibilidadDTO> periodos) {
        this.periodos = periodos;
    }

    public List<RangoReservadoDTO> getReservados() {
        return reservados;
    }
    public void setReservados(List<RangoReservadoDTO> reservados) {
        this.reservados = reservados;
    }
}
