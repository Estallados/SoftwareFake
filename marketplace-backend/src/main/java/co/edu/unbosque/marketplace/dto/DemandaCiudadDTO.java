package co.edu.unbosque.marketplace.dto;

/**
 * Cantidad de reservas de una ciudad.
 *
 * @see "RF-005, CA-026"
 */
public class DemandaCiudadDTO {
    private String ciudad;
    private Long reservas;

    public DemandaCiudadDTO() {
    }

    public String getCiudad() {
        return ciudad;
    }
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public Long getReservas() {
        return reservas;
    }
    public void setReservas(Long reservas) {
        this.reservas = reservas;
    }
}
