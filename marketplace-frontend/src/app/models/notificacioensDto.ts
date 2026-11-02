/** Notificación dentro de la plataforma (RF-003). */
export interface Notificacion {
  idNotificacion: number;
  titulo?: string;
  mensaje?: string;
  tipo?: string;
  fecha?: string; // ISO date-time
  leida: boolean;
}
