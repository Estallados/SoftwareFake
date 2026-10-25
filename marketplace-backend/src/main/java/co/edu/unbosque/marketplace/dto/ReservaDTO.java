package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Datos de una reserva con su desglose congelado, el último pago y los datos
 * del alojamiento, del huésped y del anfitrión que muestran las pantallas de
 * reservas (así no hace falta una llamada por fila).
 *
 * @see "RF-021, RF-022"
 */
public class ReservaDTO {
    private Long idReserva;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer noches;
    private Integer huespedes;
    private EstadoReserva estado;
    private BigDecimal total;
    private String desglose;
    private Long idHuesped;
    private Long idAlojamiento;
    private String tituloAlojamiento;
    private Long idCotizacion;
    private LocalDateTime fechaCreacion;
    private Boolean completada;
    private PagoDTO pago;
    /** URL de la imagen principal del alojamiento. */
    private String imagenAlojamiento;
    private String ciudad;
    private String ubicacion;
    private String tipoAlojamiento;
    private Integer capacidadAlojamiento;
    /** Política de cancelación del alojamiento. */
    private String nombrePolitica;
    private String nombreHuesped;
    private Long idAnfitrion;
    private String nombreAnfitrion;
    /** Indica si el huésped ya reseñó esta reserva. */
    private Boolean tieneResena;
    /** Datos de la cancelación (fecha, motivo, devolución); null si no fue cancelada por el huésped. */
    private CancelacionDTO cancelacion;

    public ReservaDTO() {
    }

    public Long getIdReserva() {
        return idReserva;
    }
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
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

    public Integer getNoches() {
        return noches;
    }
    public void setNoches(Integer noches) {
        this.noches = noches;
    }

    public Integer getHuespedes() {
        return huespedes;
    }
    public void setHuespedes(Integer huespedes) {
        this.huespedes = huespedes;
    }

    public EstadoReserva getEstado() {
        return estado;
    }
    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getDesglose() {
        return desglose;
    }
    public void setDesglose(String desglose) {
        this.desglose = desglose;
    }

    public Long getIdHuesped() {
        return idHuesped;
    }
    public void setIdHuesped(Long idHuesped) {
        this.idHuesped = idHuesped;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public String getTituloAlojamiento() {
        return tituloAlojamiento;
    }
    public void setTituloAlojamiento(String tituloAlojamiento) {
        this.tituloAlojamiento = tituloAlojamiento;
    }

    public Long getIdCotizacion() {
        return idCotizacion;
    }
    public void setIdCotizacion(Long idCotizacion) {
        this.idCotizacion = idCotizacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Boolean getCompletada() {
        return completada;
    }
    public void setCompletada(Boolean completada) {
        this.completada = completada;
    }

    public PagoDTO getPago() {
        return pago;
    }
    public void setPago(PagoDTO pago) {
        this.pago = pago;
    }

    public String getImagenAlojamiento() {
        return imagenAlojamiento;
    }
    public void setImagenAlojamiento(String imagenAlojamiento) {
        this.imagenAlojamiento = imagenAlojamiento;
    }

    public String getCiudad() {
        return ciudad;
    }
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getUbicacion() {
        return ubicacion;
    }
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getTipoAlojamiento() {
        return tipoAlojamiento;
    }
    public void setTipoAlojamiento(String tipoAlojamiento) {
        this.tipoAlojamiento = tipoAlojamiento;
    }

    public Integer getCapacidadAlojamiento() {
        return capacidadAlojamiento;
    }
    public void setCapacidadAlojamiento(Integer capacidadAlojamiento) {
        this.capacidadAlojamiento = capacidadAlojamiento;
    }

    public String getNombrePolitica() {
        return nombrePolitica;
    }
    public void setNombrePolitica(String nombrePolitica) {
        this.nombrePolitica = nombrePolitica;
    }

    public String getNombreHuesped() {
        return nombreHuesped;
    }
    public void setNombreHuesped(String nombreHuesped) {
        this.nombreHuesped = nombreHuesped;
    }

    public Long getIdAnfitrion() {
        return idAnfitrion;
    }
    public void setIdAnfitrion(Long idAnfitrion) {
        this.idAnfitrion = idAnfitrion;
    }

    public String getNombreAnfitrion() {
        return nombreAnfitrion;
    }
    public void setNombreAnfitrion(String nombreAnfitrion) {
        this.nombreAnfitrion = nombreAnfitrion;
    }

    public Boolean getTieneResena() {
        return tieneResena;
    }
    public void setTieneResena(Boolean tieneResena) {
        this.tieneResena = tieneResena;
    }

    public CancelacionDTO getCancelacion() {
        return cancelacion;
    }
    public void setCancelacion(CancelacionDTO cancelacion) {
        this.cancelacion = cancelacion;
    }
}
