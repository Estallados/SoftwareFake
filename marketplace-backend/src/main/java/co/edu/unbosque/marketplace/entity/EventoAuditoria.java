package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento de la auditoría: quién hizo qué, sobre qué recurso, cuándo y con qué
 * resultado.
 * <p>
 * Es de solo inserción: {@code @Immutable} hace que Hibernate ignore cualquier
 * actualización, y el repositorio no expone update ni delete.
 * </p>
 * <p>
 * A diferencia de las demás tablas, no tiene claves foráneas: la bitácora debe
 * poder registrar intentos con usuarios o recursos que no existen (un login con
 * un correo desconocido, una consulta a un id inválido), e {@code idEntidad}
 * apunta a tablas distintas según {@code entidad}.
 * </p>
 */
@Entity
@Immutable
@Table(name = "eventoAuditoria")
public class EventoAuditoria {

	/** Identificador del evento. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idEvento;

	/** Acción realizada, por ejemplo LOGIN o RESERVA_CREADA. */
	@Column(updatable = false)
	private String accion;

	/** Momento del evento. */
	@Column(updatable = false)
	private LocalDateTime fecha;

	/** Información adicional, por ejemplo el motivo de un rechazo. */
	@Column(updatable = false)
	private String detalle;

	/**
	 * Usuario que hizo la acción; null si no se conoce (por ejemplo, un login
	 * fallido).
	 */
	@Column(updatable = false)
	private Long idUsuario;
	/** Tipo de recurso afectado, por ejemplo USUARIO o ALOJAMIENTO. */
	@Column(updatable = false)
	private String entidad;

	/** Id del recurso afectado, en la tabla que indica {@code entidad}. */
	@Column(updatable = false)
	private Long idEntidad;

	/** Resultado de la operación. */
	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private ResultadoAuditoria resultado;

	/** Constructor vacío, requerido por JPA. */
	public EventoAuditoria() {
	}

	/**
	 * Devuelve el identificador del evento.
	 *
	 * @return identificador del evento
	 */
	public Long getIdEvento() {
		return idEvento;
	}

	/**
	 * Asigna el identificador del evento.
	 *
	 * @param idEvento identificador del evento
	 */
	public void setIdEvento(Long idEvento) {
		this.idEvento = idEvento;
	}

	/**
	 * Devuelve la acción realizada, por ejemplo LOGIN o RESERVA_CREADA.
	 *
	 * @return acción realizada, por ejemplo LOGIN o RESERVA_CREADA
	 */
	public String getAccion() {
		return accion;
	}

	/**
	 * Asigna la acción realizada, por ejemplo LOGIN o RESERVA_CREADA.
	 *
	 * @param accion acción realizada, por ejemplo LOGIN o RESERVA_CREADA
	 */
	public void setAccion(String accion) {
		this.accion = accion;
	}

	/**
	 * Devuelve el momento del evento.
	 *
	 * @return momento del evento
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento del evento.
	 *
	 * @param fecha momento del evento
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	/**
	 * Devuelve la información adicional, por ejemplo el motivo de un rechazo.
	 *
	 * @return información adicional, por ejemplo el motivo de un rechazo
	 */
	public String getDetalle() {
		return detalle;
	}

	/**
	 * Asigna la información adicional, por ejemplo el motivo de un rechazo.
	 *
	 * @param detalle información adicional, por ejemplo el motivo de un rechazo
	 */
	public void setDetalle(String detalle) {
		this.detalle = detalle;
	}

	/**
	 * Devuelve el usuario que hizo la acción; null si no se conoce (por ejemplo, un login fallido).
	 *
	 * @return usuario que hizo la acción; null si no se conoce (por ejemplo, un login fallido)
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que hizo la acción; null si no se conoce (por ejemplo, un login fallido).
	 *
	 * @param idUsuario usuario que hizo la acción; null si no se conoce (por ejemplo, un login fallido)
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el tipo de recurso afectado, por ejemplo USUARIO o ALOJAMIENTO.
	 *
	 * @return tipo de recurso afectado, por ejemplo USUARIO o ALOJAMIENTO
	 */
	public String getEntidad() {
		return entidad;
	}

	/**
	 * Asigna el tipo de recurso afectado, por ejemplo USUARIO o ALOJAMIENTO.
	 *
	 * @param entidad tipo de recurso afectado, por ejemplo USUARIO o ALOJAMIENTO
	 */
	public void setEntidad(String entidad) {
		this.entidad = entidad;
	}

	/**
	 * Devuelve el id del recurso afectado, en la tabla que indica {@code entidad}.
	 *
	 * @return id del recurso afectado, en la tabla que indica {@code entidad}
	 */
	public Long getIdEntidad() {
		return idEntidad;
	}

	/**
	 * Asigna el id del recurso afectado, en la tabla que indica {@code entidad}.
	 *
	 * @param idEntidad id del recurso afectado, en la tabla que indica {@code entidad}
	 */
	public void setIdEntidad(Long idEntidad) {
		this.idEntidad = idEntidad;
	}

	/**
	 * Devuelve el resultado de la operación.
	 *
	 * @return resultado de la operación
	 */
	public ResultadoAuditoria getResultado() {
		return resultado;
	}

	/**
	 * Asigna el resultado de la operación.
	 *
	 * @param resultado resultado de la operación
	 */
	public void setResultado(ResultadoAuditoria resultado) {
		this.resultado = resultado;
	}
}
