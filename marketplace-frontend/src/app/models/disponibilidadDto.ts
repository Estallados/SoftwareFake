
export interface PeriodoDisponibilidad {
  
  idPeriodo?: number;
  
  fechaLlegada: string;
 
  fechaSalida: string;
}


export type PeriodoDisponibilidadEntrada = Omit<PeriodoDisponibilidad, 'idPeriodo'>;


export interface RangoReservado {
 
  fechaLlegada: string;

  fechaSalida: string;
}


export interface CalendarioDisponibilidad {

  periodos: PeriodoDisponibilidad[];
 
  reservas: RangoReservado[];
}


export interface ConsultaDisponibilidad {
 
  disponible: boolean;

  motivo?: string | null;
}
