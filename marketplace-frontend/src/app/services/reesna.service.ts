import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Resena, SolicitudResena } from '../models/resenaDto';
import { API_URL } from './api-url';

/**
 * Cliente HTTP de reseñas del huésped. Requiere token y rol HUESPED.
 * (Las reseñas públicas de un alojamiento están en AlojamientoService.resenasDeAlojamiento.)
 */
@Injectable({ providedIn: 'root' })
export class ResenaService {
  private http = inject(HttpClient);
  private base = `${inject(API_URL)}/resenas`;

  /** Mis reseñas como huésped, incluidas las ocultas (las más recientes primero). */
  mias(tamanio = 50): Observable<Resena[]> {
    return this.http
      .get<{ contenido?: Resena[]; content?: Resena[] }>(`${this.base}/mias`, {
        params: { pagina: 0, tamanio },
      })
      .pipe(map((r) => r?.contenido ?? r?.content ?? []));
  }

  /**
   * Reseña una estancia completada (201). Una reseña por reserva, calificación de 1 a 5, comentario hasta 2000.
   * 400 datos inválidos · 404 reserva inexistente · 409 reserva no completada o ya reseñada.
   */
  crear(datos: SolicitudResena): Observable<Resena> {
    return this.http.post<Resena>(this.base, datos);
  }
}
