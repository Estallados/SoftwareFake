package co.edu.unbosque.marketplace.dto;

/**
 * Datos para activar o confirmar el segundo factor (TOTP).
 *
 * @see "RNF-033"
 */
public class SegundoFactorDTO {
    private String secreto;
    private String uri;
    private String codigo;

    public SegundoFactorDTO() {
    }

    public String getSecreto() {
        return secreto;
    }
    public void setSecreto(String secreto) {
        this.secreto = secreto;
    }

    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
