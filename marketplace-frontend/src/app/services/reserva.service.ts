import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Cotizacion, CotizacionSolicitud } from '../models/cotizacionDto';
import { Reserva, SolicitudReserva } from '../models/reservaDto';
import { API_URL } from './api-url';
@Injectable({
  providedIn: 'root',
})
export class ReservaService {
  private http = inject(HttpClient);
  private apiUrl = inject(API_URL);
  /**
   * POST /cotizaciones (201). Devuelve el desglose y sirve 30 minutos.
   * 400 datos inválidos · 404 alojamiento inexistente · 409 no disponible en esas fechas.
   */
  cotizar(solicitud: CotizacionSolicitud): Observable<Cotizacion> {
    return this.http.post<Cotizacion>(`${this.apiUrl}/cotizaciones`, solicitud);
  }
  /**
   * POST /reservas (201, solo HUESPED). Cobra y deja la reserva CONFIRMADA.
   * Idempotency-Key: reutilízala SOLO para reintentar exactamente la misma petición.
   * 402 pago rechazado · 403 alojamiento propio · 404 cotización inexistente
   * 409 cotización vencida/usada, fechas tomadas o clave repetida · 502 falla de la pasarela.
   */
  crear(solicitud: SolicitudReserva, claveIdempotencia: string): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.apiUrl}/reservas`, solicitud, {
      headers: { 'Idempotency-Key': claveIdempotencia },
    });
  }
  /** Clave única por intento de pago (1 a 100 caracteres). */
  nuevaClaveIdempotencia(): string {
    return (
      globalThis.crypto?.randomUUID?.() ??
      `${Date.now()}-${Math.random().toString(36).slice(2)}`
    );
  }
  /** Mensaje legible a partir de un error HTTP del backend. */
  mensajeDeError(error: HttpErrorResponse, porDefecto: string): string {
    if (error.status === 0) {
      return 'No hay conexión con el servidor.';
    }
    const cuerpo = error.error;
    if (typeof cuerpo === 'string' && cuerpo.trim()) {
      return cuerpo;
    }
    return cuerpo?.mensaje ?? cuerpo?.message ?? cuerpo?.error ?? porDefecto;
  }
}
