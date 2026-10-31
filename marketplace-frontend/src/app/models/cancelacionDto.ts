import { EstadoReserva } from './estadoReservaDto';

/** Registro de una cancelación. */
export interface Cancelacion {
  idCancelacion: number;
  motivo?: string;
  politica?: string;
  porcentajeReembolso?: number;
  valorReembolso?: number;
  /** Valores de EstadoCancelacion (enum del backend). */
  estado: string;
  estadoFinalReserva?: EstadoReserva;
  fecha?: string; // ISO date-time
  idUsuario?: number;
  idReserva: number;
}
