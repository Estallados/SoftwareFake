/** Política de cancelación con sus reglas de devolución (PoliticaCancelacionDTO). */
export interface PoliticaCancelacion {
  
  idPolitica: number;
 
  nombre: string;
 
  condiciones: string;
  /** Días de anticipación para reembolso total */
  diasReembolsoTotal: number;
  /** Días de anticipación para reembolso parcial */
  diasReembolsoParcial: number;
  /** Porcentaje que se devuelve en el reembolso parcial */
  porcentajeParcial: number;
}
