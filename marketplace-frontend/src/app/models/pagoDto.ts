/** Pago simulado de una reserva. */
export interface Pago {
  idPago: number;
  referencia?: string;
  proveedor?: string;
  valor: number;
  /** Valores de EstadoPago (enum del backend). */
  estado: string;
  detalle?: string;
  fecha?: string; // ISO date-time
  metodo?: string;
  valorReembolsado?: number;
  referenciaReembolso?: string;
  idReserva: number;
}
