package co.edu.unbosque.marketplace.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

/**
 * Entidad que representa imagen en el marketplace. Puede ser un archivo
 * subido por el anfitrión (se guarda en disco y {@code rutaArchivo} indica
 * dónde) o una URL externa ({@code rutaArchivo} en null). Una imagen por
 * alojamiento es la principal (portada).
 *
 * @see "RF-017"
 */
@Entity
@Table(name = "imagen")
public class Imagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idImagen;
    private String url;
    private Long idAlojamiento;
    /** Portada del alojamiento; null en imágenes anteriores a este campo. */
    private Boolean principal;
    /** Ruta relativa del archivo en el almacenamiento; null si es una URL externa. */
    private String rutaArchivo;

    public Imagen() {
    }

    public Long getIdImagen() {
        return idImagen;
    }
    public void setIdImagen(Long idImagen) {
        this.idImagen = idImagen;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public Boolean getPrincipal() {
        return principal;
    }
    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }
    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }
}
