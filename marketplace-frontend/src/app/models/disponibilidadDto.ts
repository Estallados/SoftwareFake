/** Período en el que el anfitrión ofrece el alojamiento. Fechas ISO: "2026-12-01". */
export interface PeriodoDisponibilidad {
  /** Identificador del período. */
  idPeriodo?: number;
  /** Primer día en que se puede llegar. */
  fechaLlegada: string;
  /** Último día en que se puede salir. */
  fechaSalida: string;
}

/** Cuerpo para crear o modificar un período. */
export type PeriodoDisponibilidadEntrada = Omit<PeriodoDisponibilidad, 'idPeriodo'>;

/** Rango de fechas ya reservado. */
export interface RangoReservado {
  /** Día de llegada de la reserva. */
  fechaLlegada: string;
  /** Día de salida de la reserva (no ocupa noche). */
  fechaSalida: string;
}

/** Respuesta de GET /alojamientos/{id}/disponibilidad. */
export interface CalendarioDisponibilidad {
  /** Períodos ofrecidos por el anfitrión. */
  periodos: PeriodoDisponibilidad[];
  /** Rangos ya ocupados por reservas activas. */
  reservas: RangoReservado[];
}

/** Respuesta de GET /alojamientos/{id}/disponibilidad/consultar. */
export interface ConsultaDisponibilidad {
  /** Indica si se puede reservar en esas fechas. */
  disponible: boolean;
  /** Por qué no está disponible; null si sí lo está. */
  motivo?: string | null;
}
