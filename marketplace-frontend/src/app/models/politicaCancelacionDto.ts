/** Política de cancelación con sus reglas de devolución (PoliticaCancelacionDTO). */
export interface PoliticaCancelacion {
  /** Identificador de la política. */
  idPolitica: number;
  /** Nombre: FLEXIBLE, MODERADA o ESTRICTA. */
  nombre: string;
  /** Explicación para el huésped de cuánto se devuelve y cuándo. */
  condiciones: string;
  /** Días de anticipación para reembolso total */
  diasReembolsoTotal: number;
  /** Días de anticipación para reembolso parcial */
  diasReembolsoParcial: number;
  /** Porcentaje que se devuelve en el reembolso parcial */
  porcentajeParcial: number;
}
