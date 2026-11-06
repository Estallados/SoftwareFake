/**
 * Método de pago de prueba de Stripe y el resultado que produce, para mostrarlo en la pantalla de
 * pago.
 */
export interface MetodoPagoPrueba {
  /** PaymentMethod de Stripe, ej. "pm_card_visa". */
  metodoPago: string;
  /** Descripción para el usuario, por ejemplo "Visa aprobada". */
  descripcion: string;
  /** Resultado esperado, ej. "APROBADO" o "RECHAZADO". */
  resultado: string;
}
/** Respuesta cruda de GET /pagos/configuracion (los métodos se normalizan en PagoService). */
export interface ConfiguracionPagoRespuesta extends Omit<ConfiguracionPago, 'metodosPrueba'> {
  /** Métodos de pago de prueba, tal como llegan del backend. */
  metodosPrueba: unknown[] | null;
}
/** Respuesta de GET /pagos/configuracion, ya normalizada. */
export interface ConfiguracionPago {
  /** "STRIPE_TEST" o "SIMULADO". */
  modo: 'STRIPE_TEST' | 'SIMULADO' | string;
  /** pk_test_...; null en modo SIMULADO. */
  clavePublica: string | null;
  /** Moneda en que se cobra (COP). */
  moneda: string;
  /** Métodos de pago de prueba ya normalizados. */
  metodosPrueba: MetodoPagoPrueba[];
}
/** Pago asociado a una reserva. */
export interface Pago {
  /** Identificador del pago. */
  idPago: number;
  /** Referencia del proveedor (id del PaymentIntent en Stripe). */
  referencia: string;
  /** Proveedor que procesó el pago: STRIPE_TEST o SIMULADO. */
  proveedor: string;
  /** Valor cobrado. */
  valor: number;
  /** Estado del pago. */
  estado: string;
  /** Mensaje de la pasarela, por ejemplo la causa de un rechazo. */
  detalle: string;
  /** Momento del pago (ISO). */
  fecha: string;
  /** Método de pago usado. */
  metodo: string | null;
  /** Valor devuelto al cancelar; null si no hubo devolución. */
  valorReembolsado: number | null;
  /** Id de la devolución en el proveedor. */
  referenciaReembolso: string | null;
  /** Reserva pagada. */
  idReserva: number;
}
