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
 * Entidad que representa pago (simulado, modo prueba) en el marketplace.
 *
 * @see "RF-028"
 */
@Entity
@Table(name = "pago")
public class Pago {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idPago;
	/** Referencia que entrega el proveedor (id del PaymentIntent en Stripe). */
	private String referencia;
	/** STRIPE_TEST o SIMULADO. */
	private String proveedor;
	private BigDecimal valor;
	@Enumerated(EnumType.STRING)
	private EstadoPago estado;
	private String detalle;
	private LocalDateTime fecha;
	/** PaymentMethod de Stripe con el que se cobró (por ejemplo pm_card_visa). */
	private String metodo;
	/** Valor devuelto al cancelar; null si no hubo devolución. */
	private BigDecimal valorReembolsado;
	/** Id de la devolución en Stripe (re_...) o del simulador. */
	private String referenciaReembolso;
	@Column(name = "id_reserva")
	private Long idReserva;
	/**
	 * Clave foránea hacia {@link Reserva}; el valor se asigna con
	 * {@code idReserva}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_reserva", insertable = false, updatable = false)
	private Reserva reserva;

	public Pago() {
	}

	public Long getIdPago() {
		return idPago;
	}

	public void setIdPago(Long idPago) {
		this.idPago = idPago;
	}

	public String getReferencia() {
		return referencia;
	}

	public void setReferencia(String referencia) {
		this.referencia = referencia;
	}

	public String getProveedor() {
		return proveedor;
	}

	public void setProveedor(String proveedor) {
		this.proveedor = proveedor;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public EstadoPago getEstado() {
		return estado;
	}

	public void setEstado(EstadoPago estado) {
		this.estado = estado;
	}

	public String getDetalle() {
		return detalle;
	}

	public void setDetalle(String detalle) {
		this.detalle = detalle;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public String getMetodo() {
		return metodo;
	}

	public void setMetodo(String metodo) {
		this.metodo = metodo;
	}

	public BigDecimal getValorReembolsado() {
		return valorReembolsado;
	}

	public void setValorReembolsado(BigDecimal valorReembolsado) {
		this.valorReembolsado = valorReembolsado;
	}

	public String getReferenciaReembolso() {
		return referenciaReembolso;
	}

	public void setReferenciaReembolso(String referenciaReembolso) {
		this.referenciaReembolso = referenciaReembolso;
	}

	public Long getIdReserva() {
		return idReserva;
	}

	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}
}
