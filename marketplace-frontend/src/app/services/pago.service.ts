import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import { ConfiguracionPago, ConfiguracionPagoRespuesta, MetodoPagoPrueba } from '../models/pagoDto';
import { API_URL } from './api-url';
@Injectable({
  providedIn: 'root',
})
export class PagoService {
  private http = inject(HttpClient);
  private base = `${inject(API_URL)}/pagos`;
  private configuracion$?: Observable<ConfiguracionPago>;
  /**
   * GET /pagos/configuracion (público): modo, clave publicable, moneda y métodos de prueba.
   * Se pide una sola vez; si falla, la siguiente llamada vuelve a intentarlo.
   */
  configuracion(): Observable<ConfiguracionPago> {
    if (!this.configuracion$) {
      this.configuracion$ = this.http
        .get<ConfiguracionPagoRespuesta>(`${this.base}/configuracion`)
        .pipe(
          map((c) => ({
            ...c,
            metodosPrueba: (c.metodosPrueba ?? [])
              .map((m) => this.normalizarMetodo(m))
              .filter((m): m is MetodoPagoPrueba => m !== null),
          })),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }
    return this.configuracion$;
  }
  /**
   * No depende de los nombres de campo de MetodoPagoPruebaDTO: el método de pago es el
   * texto que empieza por "pm_"; el resto de textos sirve de descripción.
   * Cuando se conozca el DTO, se puede reemplazar por un mapeo directo.
   */
  private normalizarMetodo(item: unknown): MetodoPagoPrueba | null {
    if (typeof item === 'string') {
      return item.startsWith('pm_') ? { metodoPago: item, descripcion: item, resultado: '' } : null;
    }
    if (!item || typeof item !== 'object') {
      return null;
    }
    const textos = Object.values(item as Record<string, unknown>).filter(
      (v): v is string => typeof v === 'string',
    );
    const metodoPago = textos.find((v) => v.startsWith('pm_'));
    if (!metodoPago) {
      return null;
    }
    const resto = textos.filter((v) => v !== metodoPago);
    return { metodoPago, descripcion: resto[0] ?? metodoPago, resultado: resto[1] ?? '' };
  }
}
