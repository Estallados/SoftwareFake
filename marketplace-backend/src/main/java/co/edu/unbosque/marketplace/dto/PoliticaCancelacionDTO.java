package co.edu.unbosque.marketplace.dto;

/**
 * Política de cancelación con sus reglas de devolución por días de anticipación.
 *
 * @see "RF-002"
 */
public class PoliticaCancelacionDTO {
    private Long idPolitica;
    private String nombre;
    private String condiciones;
    private Integer diasReembolsoTotal;
    private Integer diasReembolsoParcial;
    private Integer porcentajeParcial;

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

    public Integer getDiasReembolsoTotal() {
        return diasReembolsoTotal;
    }
    public void setDiasReembolsoTotal(Integer diasReembolsoTotal) {
        this.diasReembolsoTotal = diasReembolsoTotal;
    }

    public Integer getDiasReembolsoParcial() {
        return diasReembolsoParcial;
    }
    public void setDiasReembolsoParcial(Integer diasReembolsoParcial) {
        this.diasReembolsoParcial = diasReembolsoParcial;
    }

    public Integer getPorcentajeParcial() {
        return porcentajeParcial;
    }
    public void setPorcentajeParcial(Integer porcentajeParcial) {
        this.porcentajeParcial = porcentajeParcial;
    }
}
