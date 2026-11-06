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
 */
@Entity
@Table(name = "registroIdempotencia")
public class RegistroIdempotencia {

	/** Identificador del registro. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idRegistro;

	/** Valor de la cabecera {@code Idempotency-Key} que mandó el cliente. */
	private String clave;

	/** Usuario que hizo la petición. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;
	/** Nombre de la operación protegida, por ejemplo CREAR_RESERVA. */
	private String operacion;
	/**
	 * SHA-256 del cuerpo de la petición: detecta si la clave se reusa con otros
	 * datos.
	 */
	private String huellaPeticion;

	/** Si la petición todavía se está atendiendo o ya terminó. */
	@Enumerated(EnumType.STRING)
	private EstadoIdempotencia estado;

	/** Código HTTP que se respondió. */
	private Integer codigoRespuesta;

	/** Cuerpo JSON que se respondió, para repetirlo si llega la misma petición. */
	@Column(columnDefinition = "TEXT")
	private String cuerpoRespuesta;

	/** Momento en que llegó la petición. */
	private LocalDateTime fechaCreacion;

	/** Constructor vacío, requerido por JPA. */
	public RegistroIdempotencia() {
	}

	/**
	 * Devuelve el identificador del registro.
	 *
	 * @return identificador del registro
	 */
	public Long getIdRegistro() {
		return idRegistro;
	}

	/**
	 * Asigna el identificador del registro.
	 *
	 * @param idRegistro identificador del registro
	 */
	public void setIdRegistro(Long idRegistro) {
		this.idRegistro = idRegistro;
	}

	/**
	 * Devuelve el valor de la cabecera {@code Idempotency-Key} que mandó el cliente.
	 *
	 * @return valor de la cabecera {@code Idempotency-Key} que mandó el cliente
	 */
	public String getClave() {
		return clave;
	}

	/**
	 * Asigna el valor de la cabecera {@code Idempotency-Key} que mandó el cliente.
	 *
	 * @param clave valor de la cabecera {@code Idempotency-Key} que mandó el cliente
	 */
	public void setClave(String clave) {
		this.clave = clave;
	}

	/**
	 * Devuelve el usuario que hizo la petición.
	 *
	 * @return usuario que hizo la petición
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que hizo la petición.
	 *
	 * @param idUsuario usuario que hizo la petición
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el nombre de la operación protegida, por ejemplo CREAR_RESERVA.
	 *
	 * @return nombre de la operación protegida, por ejemplo CREAR_RESERVA
	 */
	public String getOperacion() {
		return operacion;
	}

	/**
	 * Asigna el nombre de la operación protegida, por ejemplo CREAR_RESERVA.
	 *
	 * @param operacion nombre de la operación protegida, por ejemplo CREAR_RESERVA
	 */
	public void setOperacion(String operacion) {
		this.operacion = operacion;
	}

	/**
	 * Devuelve SHA-256 del cuerpo de la petición: detecta si la clave se reusa con otros datos.
	 *
	 * @return SHA-256 del cuerpo de la petición: detecta si la clave se reusa con otros datos
	 */
	public String getHuellaPeticion() {
		return huellaPeticion;
	}

	/**
	 * Asigna SHA-256 del cuerpo de la petición: detecta si la clave se reusa con otros datos.
	 *
	 * @param huellaPeticion SHA-256 del cuerpo de la petición: detecta si la clave se reusa con otros datos
	 */
	public void setHuellaPeticion(String huellaPeticion) {
		this.huellaPeticion = huellaPeticion;
	}

	/**
	 * Devuelve si la petición todavía se está atendiendo o ya terminó.
	 *
	 * @return si la petición todavía se está atendiendo o ya terminó
	 */
	public EstadoIdempotencia getEstado() {
		return estado;
	}

	/**
	 * Asigna si la petición todavía se está atendiendo o ya terminó.
	 *
	 * @param estado si la petición todavía se está atendiendo o ya terminó
	 */
	public void setEstado(EstadoIdempotencia estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve el código HTTP que se respondió.
	 *
	 * @return código HTTP que se respondió
	 */
	public Integer getCodigoRespuesta() {
		return codigoRespuesta;
	}

	/**
	 * Asigna el código HTTP que se respondió.
	 *
	 * @param codigoRespuesta código HTTP que se respondió
	 */
	public void setCodigoRespuesta(Integer codigoRespuesta) {
		this.codigoRespuesta = codigoRespuesta;
	}

	/**
	 * Devuelve el cuerpo JSON que se respondió, para repetirlo si llega la misma petición.
	 *
	 * @return cuerpo JSON que se respondió, para repetirlo si llega la misma petición
	 */
	public String getCuerpoRespuesta() {
		return cuerpoRespuesta;
	}

	/**
	 * Asigna el cuerpo JSON que se respondió, para repetirlo si llega la misma petición.
	 *
	 * @param cuerpoRespuesta cuerpo JSON que se respondió, para repetirlo si llega la misma petición
	 */
	public void setCuerpoRespuesta(String cuerpoRespuesta) {
		this.cuerpoRespuesta = cuerpoRespuesta;
	}

	/**
	 * Devuelve el momento en que llegó la petición.
	 *
	 * @return momento en que llegó la petición
	 */
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	/**
	 * Asigna el momento en que llegó la petición.
	 *
	 * @param fechaCreacion momento en que llegó la petición
	 */
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}
}
