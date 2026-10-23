package co.edu.unbosque.marketplace.dto;


public class ImagenDTO {
    private Long idImagen;
    private String url;
    private Long idAlojamiento;

    public ImagenDTO() {
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
}
