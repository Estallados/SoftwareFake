import { EstadoCancelacion } from './estadoCancelacionDto';
import { EstadoReserva } from './estadoReservaDto';

/** Registro de una cancelación. */
export interface Cancelacion {
  /** Identificador de la cancelación. */
  idCancelacion: number;
  /** Motivo que escribió quien canceló. */
  motivo: string | null;
  /** Nombre de la política de cancelación aplicada. */
  politica: string | null;
  /** 0 a 100. */
  porcentajeReembolso: number | null;
  /** Valor devuelto. */
  valorReembolso: number | null;
  /** Estado de la cancelación. */
  estado: EstadoCancelacion;
  /** Estado en que quedó la reserva. */
  estadoFinalReserva: EstadoReserva | null;
  /** ISO date-time. */
  fecha: string;
  /** Usuario que canceló. */
  idUsuario: number;
  /** Reserva cancelada. */
  idReserva: number;
}
