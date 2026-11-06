package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoPago;

/**
 * Pago de una reserva. Los pagos son de prueba (Stripe en modo test o el
 * simulador interno) y no mueven dinero real.
 */
public class PagoDTO {
	/** Identificador del pago. */
	private Long idPago;

	/** Referencia del proveedor (id del PaymentIntent en Stripe). */
	private String referencia;

	/** Proveedor que procesó el pago: STRIPE_TEST o SIMULADO. */
	private String proveedor;

	/** Valor cobrado. */
	private BigDecimal valor;

	/** Estado del pago. */
	private EstadoPago estado;

	/** Mensaje de la pasarela, por ejemplo la causa de un rechazo. */
	private String detalle;

	/** Momento del pago. */
	private LocalDateTime fecha;

	/** Método de pago usado, por ejemplo {@code pm_card_visa}. */
	private String metodo;

	/** Valor devuelto al cancelar; {@code null} si no hubo devolución. */
	private BigDecimal valorReembolsado;

	/** Id de la devolución en el proveedor. */
	private String referenciaReembolso;

	/** Reserva pagada. */
	private Long idReserva;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public PagoDTO() {
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
	 * Devuelve la referencia del proveedor (id del PaymentIntent en Stripe).
	 *
	 * @return referencia del proveedor (id del PaymentIntent en Stripe)
	 */
	public String getReferencia() {
		return referencia;
	}

	/**
	 * Asigna la referencia del proveedor (id del PaymentIntent en Stripe).
	 *
	 * @param referencia referencia del proveedor (id del PaymentIntent en Stripe)
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
	 * Devuelve el valor cobrado.
	 *
	 * @return valor cobrado
	 */
	public BigDecimal getValor() {
		return valor;
	}

	/**
	 * Asigna el valor cobrado.
	 *
	 * @param valor valor cobrado
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
	 * Devuelve la reserva pagada.
	 *
	 * @return reserva pagada
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva pagada.
	 *
	 * @param idReserva reserva pagada
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}

	/**
	 * Devuelve el método de pago usado, por ejemplo {@code pm_card_visa}.
	 *
	 * @return método de pago usado, por ejemplo {@code pm_card_visa}
	 */
	public String getMetodo() {
		return metodo;
	}

	/**
	 * Asigna el método de pago usado, por ejemplo {@code pm_card_visa}.
	 *
	 * @param metodo método de pago usado, por ejemplo {@code pm_card_visa}
	 */
	public void setMetodo(String metodo) {
		this.metodo = metodo;
	}

	/**
	 * Devuelve el valor devuelto al cancelar; {@code null} si no hubo devolución.
	 *
	 * @return valor devuelto al cancelar; {@code null} si no hubo devolución
	 */
	public BigDecimal getValorReembolsado() {
		return valorReembolsado;
	}

	/**
	 * Asigna el valor devuelto al cancelar; {@code null} si no hubo devolución.
	 *
	 * @param valorReembolsado valor devuelto al cancelar; {@code null} si no hubo devolución
	 */
	public void setValorReembolsado(BigDecimal valorReembolsado) {
		this.valorReembolsado = valorReembolsado;
	}

	/**
	 * Devuelve el id de la devolución en el proveedor.
	 *
	 * @return id de la devolución en el proveedor
	 */
	public String getReferenciaReembolso() {
		return referenciaReembolso;
	}

	/**
	 * Asigna el id de la devolución en el proveedor.
	 *
	 * @param referenciaReembolso id de la devolución en el proveedor
	 */
	public void setReferenciaReembolso(String referenciaReembolso) {
		this.referenciaReembolso = referenciaReembolso;
	}
}
