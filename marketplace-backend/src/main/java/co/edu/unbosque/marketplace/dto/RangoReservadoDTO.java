package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

/**
 * Rango de fechas ocupado por una reserva activa (llegada inclusive, salida exclusiva).
 *
 * @see "RF-020, CA-148"
 */
public class RangoReservadoDTO {
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;

    public RangoReservadoDTO() {
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }
    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }
    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }
}
