package co.edu.unbosque.marketplace.dto;

/**
 * Reseñas visibles de un alojamiento (o de todos los alojamientos de un
 * anfitrión, con idAlojamiento en null) con su promedio y cantidad.
 *
 * @see "RF-004, CA-019, CA-020"
 */
public class ResumenResenasDTO {
    private Long idAlojamiento;
    private Double promedio;
    private Long cantidad;
    private PaginaDTO<ResenaDTO> resenas;

    public ResumenResenasDTO() {
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public Double getPromedio() {
        return promedio;
    }
    public void setPromedio(Double promedio) {
        this.promedio = promedio;
    }

    public Long getCantidad() {
        return cantidad;
    }
    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public PaginaDTO<ResenaDTO> getResenas() {
        return resenas;
    }
    public void setResenas(PaginaDTO<ResenaDTO> resenas) {
        this.resenas = resenas;
    }
}
