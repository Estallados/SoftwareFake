package co.edu.unbosque.marketplace.dto;


public class PoliticaCancelacionDTO {
    private Long idPolitica;
    private String nombre;
    private String condiciones;
    private Long idAlojamiento;

    public PoliticaCancelacionDTO() {
    }

    public Long getIdPolitica() {
        return idPolitica;
    }
    public void setIdPolitica(Long idPolitica) {
        this.idPolitica = idPolitica;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCondiciones() {
        return condiciones;
    }
    public void setCondiciones(String condiciones) {
        this.condiciones = condiciones;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }
}
