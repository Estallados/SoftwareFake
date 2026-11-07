/** Notificación dentro de la plataforma, la que se ve en la campana. */
export interface Notificacion {

  idNotificacion: number;

  titulo?: string;

  mensaje?: string;
 
  tipo?: string;

  fecha?: string; // ISO date-time
 
  leida: boolean;
}
