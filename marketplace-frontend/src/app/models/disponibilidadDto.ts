// OJO: los nombres de campo son una suposición; ajústalos a los DTO de Java
// (PeriodoDisponibilidadDTO y las respuestas de calendario/consultar).

/** Período en el que el anfitrión ofrece el alojamiento. Fechas ISO: "2026-12-01". */
export interface PeriodoDisponibilidad {
  idPeriodo?: number;
  fechaLlegada: string;
  fechaSalida: string;
}

/** Cuerpo para crear o modificar un período. */
export type PeriodoDisponibilidadEntrada = Omit<PeriodoDisponibilidad, 'idPeriodo'>;

/** Rango de fechas ya reservado. */
export interface RangoReservado {
  fechaLlegada: string;
  fechaSalida: string;
}

/** Respuesta de GET /alojamientos/{id}/disponibilidad. */
export interface CalendarioDisponibilidad {
  periodos: PeriodoDisponibilidad[];
  reservas: RangoReservado[];
}

/** Respuesta de GET /alojamientos/{id}/disponibilidad/consultar. */
export interface ConsultaDisponibilidad {
  disponible: boolean;
  motivo?: string | null;
}
