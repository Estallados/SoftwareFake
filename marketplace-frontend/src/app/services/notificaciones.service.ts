import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_URL } from './api-url';
import {Notificacion} from '../models/notificacioensDto';

/**
 * Cliente HTTP de /notificaciones. Todo requiere token (lo agrega el interceptor).
 * Cada usuario ve y marca solo las suyas.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private http = inject(HttpClient);
  private base = `${inject(API_URL)}/notificaciones`;

  /** Mis notificaciones, de la más reciente a la más antigua (el backend pagina: por defecto 20). */
  listar(tamanio = 50): Observable<Notificacion[]> {
    return this.http
      .get<{ contenido?: Notificacion[]; content?: Notificacion[] }>(this.base, {
        params: { pagina: 0, tamanio },
      })
      .pipe(map((r) => r?.contenido ?? r?.content ?? []));
  }

  /** Cantidad de notificaciones sin leer (contador de la campana). */
  noLeidas(): Observable<number> {
    return this.http
      .get<{ cantidad: number }>(`${this.base}/noleidas`)
      .pipe(map((r) => r?.cantidad ?? 0));
  }

  /** Marca todas como leídas; devuelve cuántas se actualizaron. */
  marcarTodasLeidas(): Observable<number> {
    return this.http
      .patch<{ actualizadas: number }>(`${this.base}/leidas`, {})
      .pipe(map((r) => r?.actualizadas ?? 0));
  }

  /** Marca una notificación propia como leída (404 si no existe). */
  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.patch<Notificacion>(`${this.base}/${id}/leida`, {});
  }
}
