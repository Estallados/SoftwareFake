package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
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
 * Pago de una reserva. Todos los pagos son de prueba: se hacen con Stripe en
 * modo test o con el simulador interno, y no mueven dinero real.
 */
@Entity
@Table(name = "pago")
public class Pago {

	/** Identificador del pago. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idPago;

	/** Referencia que entrega el proveedor (id del PaymentIntent en Stripe). */
	private String referencia;

	/** Proveedor que procesó el pago: STRIPE_TEST o SIMULADO. */
	private String proveedor;

	/** Valor cobrado, en pesos colombianos. */
	private BigDecimal valor;

	/** Estado del pago. */
	@Enumerated(EnumType.STRING)
	private EstadoPago estado;

	/** Mensaje de la pasarela, por ejemplo la causa de un rechazo. */
	private String detalle;

	/** Momento del pago. */
	private LocalDateTime fecha;

	/** PaymentMethod de Stripe con el que se cobró (por ejemplo pm_card_visa). */
	private String metodo;
	/** Valor devuelto al cancelar; null si no hubo devolución. */
	private BigDecimal valorReembolsado;
	/** Id de la devolución en Stripe (re_...) o del simulador. */
	private String referenciaReembolso;

	/** Reserva que se pagó. */
	@Column(name = "id_reserva")
	private Long idReserva;

	/**
	 * Clave foránea hacia {@link Reserva}; el valor se asigna con
	 * {@code idReserva}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_reserva", insertable = false, updatable = false)
	private Reserva reserva;

	/** Constructor vacío, requerido por JPA. */
	public Pago() {
	}

	/**
	 * Devuelve el identificador del pago.
	 *
	 * @return identificador del pago
	 */
	public Long getIdPago() {
		return idPago;
	}

	/**
	 * Asigna el identificador del pago.
	 *
	 * @param idPago identificador del pago
	 */
	public void setIdPago(Long idPago) {
		this.idPago = idPago;
	}

	/**
	 * Devuelve la referencia que entrega el proveedor (id del PaymentIntent en Stripe).
	 *
	 * @return referencia que entrega el proveedor (id del PaymentIntent en Stripe)
	 */
	public String getReferencia() {
		return referencia;
	}

	/**
	 * Asigna la referencia que entrega el proveedor (id del PaymentIntent en Stripe).
	 *
	 * @param referencia referencia que entrega el proveedor (id del PaymentIntent en Stripe)
	 */
	public void setReferencia(String referencia) {
		this.referencia = referencia;
	}

	/**
	 * Devuelve el proveedor que procesó el pago: STRIPE_TEST o SIMULADO.
	 *
	 * @return proveedor que procesó el pago: STRIPE_TEST o SIMULADO
	 */
	public String getProveedor() {
		return proveedor;
	}

	/**
	 * Asigna el proveedor que procesó el pago: STRIPE_TEST o SIMULADO.
	 *
	 * @param proveedor proveedor que procesó el pago: STRIPE_TEST o SIMULADO
	 */
	public void setProveedor(String proveedor) {
		this.proveedor = proveedor;
	}

	/**
	 * Devuelve el valor cobrado, en pesos colombianos.
	 *
	 * @return valor cobrado, en pesos colombianos
	 */
	public BigDecimal getValor() {
		return valor;
	}

	/**
	 * Asigna el valor cobrado, en pesos colombianos.
	 *
	 * @param valor valor cobrado, en pesos colombianos
	 */
	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	/**
	 * Devuelve el estado del pago.
	 *
	 * @return estado del pago
	 */
	public EstadoPago getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado del pago.
	 *
	 * @param estado estado del pago
	 */
	public void setEstado(EstadoPago estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve el mensaje de la pasarela, por ejemplo la causa de un rechazo.
	 *
	 * @return mensaje de la pasarela, por ejemplo la causa de un rechazo
	 */
	public String getDetalle() {
		return detalle;
	}

	/**
	 * Asigna el mensaje de la pasarela, por ejemplo la causa de un rechazo.
	 *
	 * @param detalle mensaje de la pasarela, por ejemplo la causa de un rechazo
	 */
	public void setDetalle(String detalle) {
		this.detalle = detalle;
	}

	/**
	 * Devuelve el momento del pago.
	 *
	 * @return momento del pago
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento del pago.
	 *
	 * @param fecha momento del pago
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	/**
	 * Devuelve PaymentMethod de Stripe con el que se cobró (por ejemplo pm_card_visa).
	 *
	 * @return PaymentMethod de Stripe con el que se cobró (por ejemplo pm_card_visa)
	 */
	public String getMetodo() {
		return metodo;
	}

	/**
	 * Asigna PaymentMethod de Stripe con el que se cobró (por ejemplo pm_card_visa).
	 *
	 * @param metodo PaymentMethod de Stripe con el que se cobró (por ejemplo pm_card_visa)
	 */
	public void setMetodo(String metodo) {
		this.metodo = metodo;
	}

	/**
	 * Devuelve el valor devuelto al cancelar; null si no hubo devolución.
	 *
	 * @return valor devuelto al cancelar; null si no hubo devolución
	 */
	public BigDecimal getValorReembolsado() {
		return valorReembolsado;
	}

	/**
	 * Asigna el valor devuelto al cancelar; null si no hubo devolución.
	 *
	 * @param valorReembolsado valor devuelto al cancelar; null si no hubo devolución
	 */
	public void setValorReembolsado(BigDecimal valorReembolsado) {
		this.valorReembolsado = valorReembolsado;
	}

	/**
	 * Devuelve el id de la devolución en Stripe (re_...) o del simulador.
	 *
	 * @return id de la devolución en Stripe (re_...) o del simulador
	 */
	public String getReferenciaReembolso() {
		return referenciaReembolso;
	}

	/**
	 * Asigna el id de la devolución en Stripe (re_...) o del simulador.
	 *
	 * @param referenciaReembolso id de la devolución en Stripe (re_...) o del simulador
	 */
	public void setReferenciaReembolso(String referenciaReembolso) {
		this.referenciaReembolso = referenciaReembolso;
	}

	/**
	 * Devuelve la reserva que se pagó.
	 *
	 * @return reserva que se pagó
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva que se pagó.
	 *
	 * @param idReserva reserva que se pagó
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}
}
