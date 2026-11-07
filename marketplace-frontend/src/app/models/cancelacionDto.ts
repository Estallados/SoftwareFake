import { EstadoCancelacion } from './estadoCancelacionDto';
import { EstadoReserva } from './estadoReservaDto';


export interface Cancelacion {

  idCancelacion: number;
 
  motivo: string | null;
 
  politica: string | null;
 
  porcentajeReembolso: number | null;
 
  valorReembolso: number | null;
 
  estado: EstadoCancelacion;
 
  estadoFinalReserva: EstadoReserva | null;

  fecha: string;

  idUsuario: number;

  idReserva: number;
}
