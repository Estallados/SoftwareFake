import { EstadoCancelacion } from './estadoCancelacionDto';
import { EstadoReserva } from './estadoReservaDto';

/** Registro de una cancelación. */
export interface Cancelacion {
  idCancelacion: number;
  motivo: string | null;
  /** Nombre de la política de cancelación aplicada. */
  politica: string | null;
  /** 0 a 100. */
  porcentajeReembolso: number | null;
  valorReembolso: number | null;
  estado: EstadoCancelacion;
  estadoFinalReserva: EstadoReserva | null;
  /** ISO date-time. */
  fecha: string;
  idUsuario: number;
  idReserva: number;
}
