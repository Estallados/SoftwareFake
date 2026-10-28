package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad que guarda una petición hecha con la cabecera Idempotency-Key y la
 * respuesta que se le dio, para devolver la misma respuesta si se repite.
 * <p>
 * La clave es única por usuario y operación (por ejemplo, la misma clave puede
 * usarse una vez para crear una reserva y otra para pagarla).
 * </p>
 *
 * @see "RNF-013, RNF-014"
 */
@Entity
@Table(name = "registroIdempotencia")
public class RegistroIdempotencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRegistro;
    private String clave;
    @Column(name = "id_usuario")
    private Long idUsuario;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idUsuario}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;
    /** Nombre de la operación protegida, por ejemplo CREAR_RESERVA. */
    private String operacion;
    /** SHA-256 del cuerpo de la petición: detecta si la clave se reusa con otros datos. */
    private String huellaPeticion;
    @Enumerated(EnumType.STRING)
    private EstadoIdempotencia estado;
    private Integer codigoRespuesta;
    private String cuerpoRespuesta;
    private LocalDateTime fechaCreacion;

    public RegistroIdempotencia() {
    }

    public Long getIdRegistro() {
        return idRegistro;
    }
    public void setIdRegistro(Long idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getClave() {
        return clave;
    }
    public void setClave(String clave) {
        this.clave = clave;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getOperacion() {
        return operacion;
    }
    public void setOperacion(String operacion) {
        this.operacion = operacion;
    }

    public String getHuellaPeticion() {
        return huellaPeticion;
    }
    public void setHuellaPeticion(String huellaPeticion) {
        this.huellaPeticion = huellaPeticion;
    }

    public EstadoIdempotencia getEstado() {
        return estado;
    }
    public void setEstado(EstadoIdempotencia estado) {
        this.estado = estado;
    }

    public Integer getCodigoRespuesta() {
        return codigoRespuesta;
    }
    public void setCodigoRespuesta(Integer codigoRespuesta) {
        this.codigoRespuesta = codigoRespuesta;
    }

    public String getCuerpoRespuesta() {
        return cuerpoRespuesta;
    }
    public void setCuerpoRespuesta(String cuerpoRespuesta) {
        this.cuerpoRespuesta = cuerpoRespuesta;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
