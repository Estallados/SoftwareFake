/**
 * OJO: no vi MetodoPagoPruebaDTO; sus campos son una suposición.
 * Ajústalos a la clase Java.
 */
export interface MetodoPagoPrueba {
  /** PaymentMethod de Stripe, ej. "pm_card_visa". */
  metodoPago: string;
  descripcion: string;
  /** Resultado esperado, ej. "APROBADO" o "RECHAZADO". */
  resultado: string;
}
/** Respuesta cruda de GET /pagos/configuracion (los métodos se normalizan en PagoService). */
export interface ConfiguracionPagoRespuesta extends Omit<ConfiguracionPago, 'metodosPrueba'> {
  metodosPrueba: unknown[] | null;
}
/** Respuesta de GET /pagos/configuracion, ya normalizada. */
export interface ConfiguracionPago {
  /** "STRIPE_TEST" o "SIMULADO". */
  modo: 'STRIPE_TEST' | 'SIMULADO' | string;
  /** pk_test_...; null en modo SIMULADO. */
  clavePublica: string | null;
  moneda: string;
  metodosPrueba: MetodoPagoPrueba[];
}
/** Pago asociado a una reserva. */
export interface Pago {
  idPago: number;
  referencia: string;
  proveedor: string;
  valor: number;
  estado: string;
  detalle: string;
  fecha: string;
  metodo: string | null;
  valorReembolsado: number | null;
  referenciaReembolso: string | null;
  idReserva: number;
}
