/** Servicio adicional con costo (ServicioAdicionalDTO). */
export interface ServicioAdicional {

  idServicio?: number;
  
  nombre: string;
 
  costo: number;

  idAlojamiento?: number;
}
