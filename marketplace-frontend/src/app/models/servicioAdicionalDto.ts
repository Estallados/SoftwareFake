/** Servicio adicional con costo (ServicioAdicionalDTO). */
export interface ServicioAdicional {
  /** Identificador del servicio. */
  idServicio?: number;
  /** Nombre del servicio, por ejemplo "Desayuno". */
  nombre: string;
  /** Costo por reserva, en pesos colombianos. */
  costo: number;
  /** Alojamiento que lo ofrece. */
  idAlojamiento?: number;
}
