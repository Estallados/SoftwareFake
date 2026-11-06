import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Cotizacion, CotizacionSolicitud } from '../models/cotizacionDto';
import { Reserva, SolicitudReserva } from '../models/reservaDto';
import { API_URL } from './api-url';
/**
 * Cotizaciones y reservas del huésped: cotizar, reservar con pago de prueba, consultar y cancelar.
 * Reservar y cancelar llevan una clave de idempotencia para no repetir la operación si se
 * reintenta.
 */
@Injectable({
  providedIn: 'root',
})
export class ReservaService {
  /** Cliente HTTP. */
  private http = inject(HttpClient);

  /** Dirección base del backend. */
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
  /**
   * GET /reservas/mias (huésped). Mis reservas, las más recientes primero; el backend pagina y se
   * piden hasta `tamanio`.
   *
   * @param tamanio cantidad máxima de reservas
   * @returns las reservas del huésped
   */
  misReservas(tamanio = 100): Observable<Reserva[]> {
    return this.http
      .get<{ contenido?: Reserva[]; content?: Reserva[] }>(`${this.apiUrl}/reservas/mias`, {
        params: { pagina: 0, tamanio },
      })
      .pipe(map((r) => r?.contenido ?? r?.content ?? []));
  }
  /** GET /reservas/{id}. 404 si no existe; 403 si no es tuya. (Endpoint supuesto.) */
  obtener(id: number): Observable<Reserva> {
    return this.http.get<Reserva>(`${this.apiUrl}/reservas/${id}`);
  }
  /**
   * POST /reservas/{id}/cancelar con { motivo } (motivo obligatorio). Exige la cabecera Idempotency-Key:
   * si no se indica una, se genera una nueva; pásala explícita solo para reintentar la MISMA cancelación.
   * 404 no existe · 409 no se puede cancelar en ese estado o clave repetida con otra petición.
   */
  cancelar(id: number, motivo: string, claveIdempotencia: string = this.nuevaClaveIdempotencia()): Observable<unknown> {
    return this.http.post<unknown>(
      `${this.apiUrl}/reservas/${id}/cancelar`,
      { motivo },
      { headers: { 'Idempotency-Key': claveIdempotencia } },
    );
  }
  /** Clave única por intento de pago (1 a 100 caracteres). */
  nuevaClaveIdempotencia(): string {
    const c = globalThis.crypto;
    if (c.randomUUID) {
      return c.randomUUID(); // requiere contexto seguro (https o localhost)
    }
    // Alternativa criptográficamente segura que también funciona sin https: 32 caracteres hex.
    return Array.from(c.getRandomValues(new Uint8Array(16)), (b) => b.toString(16).padStart(2, '0')).join('');
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
