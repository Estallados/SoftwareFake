import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay, tap } from 'rxjs';
import {
  CalendarioDisponibilidad,
  ConsultaDisponibilidad,
  PeriodoDisponibilidad,
  PeriodoDisponibilidadEntrada,
} from '../models/disponibilidadDto';
import { RespuestaMensaje } from '../models/respuestaMensajeDto';
import { API_URL } from './api-url';
/**
 * Calendario y períodos de disponibilidad de un alojamiento. Guarda las consultas un tiempo corto
 * en caché y la limpia cuando el anfitrión cambia un período.
 */
@Injectable({
  providedIn: 'root',
})
export class DisponibilidadService {
  
  private http = inject(HttpClient);

  
  private apiUrl = inject(API_URL);
  private readonly TTL_CALENDARIO_MS = 30_000;
  private readonly TTL_CONSULTA_MS = 15_000;

 
  private readonly cache = new Map<string, { expira: number; fuente$: Observable<unknown> }>();
  private cacheado<T>(clave: string, ttl: number, pedir: () => Observable<T>): Observable<T> {
    const guardado = this.cache.get(clave);
    if (guardado && guardado.expira > Date.now()) {
      return guardado.fuente$ as Observable<T>;
    }
    const fuente$ = pedir().pipe(
      tap({ error: () => this.cache.delete(clave) }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.cache.set(clave, { expira: Date.now() + ttl, fuente$ });
    return fuente$;
  }
  /** Descarta lo guardado de un alojamiento (o de todos si no se indica). */
  limpiarCache(idAlojamiento?: number): void {
    if (idAlojamiento === undefined) {
      this.cache.clear();
      return;
    }
    for (const clave of [...this.cache.keys()]) {
      if (clave.startsWith(`${idAlojamiento}|`)) {
        this.cache.delete(clave);
      }
    }
  }

  private base(idAlojamiento: number): string {
    return `${this.apiUrl}/alojamientos/${idAlojamiento}/disponibilidad`;
  }
  // ================= Normalización =================
  // Los nombres de campo del backend no se conocían con certeza: se buscan bajo varias claves posibles.

  private texto(o: Record<string, unknown>, ...claves: string[]): string {
    for (const clave of claves) {
      const v = o?.[clave];
      if (v !== undefined && v !== null && v !== '' && typeof v !== 'object') {
        return String(v);
      }
    }
    return '';
  }


  private aRangos(valor: unknown): PeriodoDisponibilidad[] {
    if (!Array.isArray(valor)) {
      return [];
    }
    return valor
      .map((item) => {
        const o = item as Record<string, unknown>;
        return {
          fechaLlegada: this.texto(o, 'fechaLlegada', 'fechaInicio', 'fechaEntrada', 'llegada', 'desde', 'inicio').slice(0, 10),
          fechaSalida: this.texto(o, 'fechaSalida', 'fechaFin', 'salida', 'hasta', 'fin').slice(0, 10),
        };
      })
      .filter((r) => r.fechaLlegada && r.fechaSalida);
  }


  private normalizarCalendario(r: Record<string, unknown>): CalendarioDisponibilidad {
    const lista = (...claves: string[]) => claves.map((k) => r?.[k]).find((v) => Array.isArray(v));
    return {
      periodos: this.aRangos(lista('periodos', 'periodosDisponibles', 'periodosOfrecidos', 'disponibles')),
      reservas: this.aRangos(lista('reservas', 'reservados', 'reservadas', 'fechasReservadas', 'rangosReservados')),
    };
  }


  private normalizarConsulta(r: Record<string, unknown>): ConsultaDisponibilidad {
    return {
      disponible: this.texto(r, 'disponible', 'estaDisponible') === 'true',
      motivo: this.texto(r, 'motivo', 'mensaje', 'razon') || null,
    };
  }
  // ================= Consulta =================
  /**
   * Calendario: períodos ofrecidos y rangos reservados.
   * Público si el alojamiento está ACTIVA. 404 si no existe.
   */
  calendario(idAlojamiento: number): Observable<CalendarioDisponibilidad> {
    return this.cacheado(`${idAlojamiento}|calendario`, this.TTL_CALENDARIO_MS, () =>
      this.http.get<Record<string, unknown>>(this.base(idAlojamiento)).pipe(map((r) => this.normalizarCalendario(r))),
    );
  }
  /**
   * ¿Está disponible entre dos fechas? (público)
   * Fechas ISO "YYYY-MM-DD". La salida debe ser posterior a la llegada y la
   * llegada no puede estar en el pasado; si no, el backend responde 400.
   */
  consultar(
    idAlojamiento: number,
    llegada: string,
    salida: string,
  ): Observable<ConsultaDisponibilidad> {
    return this.cacheado(`${idAlojamiento}|${llegada}|${salida}`, this.TTL_CONSULTA_MS, () =>
      this.http
        .get<Record<string, unknown>>(`${this.base(idAlojamiento)}/consultar`, { params: { llegada, salida } })
        .pipe(map((r) => this.normalizarConsulta(r))),
    );
  }
  // ================= Gestión (ANFITRION o ADMIN) =================
  /**
   * Registra un período disponible (201).
   * 403 si no es el propietario; 409 si se cruza con otro período.
   */
  agregar(
    idAlojamiento: number,
    datos: PeriodoDisponibilidadEntrada,
  ): Observable<PeriodoDisponibilidad> {
    return this.http
      .post<PeriodoDisponibilidad>(this.base(idAlojamiento), datos)
      .pipe(tap(() => this.limpiarCache(idAlojamiento)));
  }
  /**
   * Modifica un período. 409 si se cruza con otro o afecta una reserva existente.
   */
  actualizar(
    idAlojamiento: number,
    idPeriodo: number,
    datos: PeriodoDisponibilidadEntrada,
  ): Observable<PeriodoDisponibilidad> {
    return this.http
      .put<PeriodoDisponibilidad>(`${this.base(idAlojamiento)}/${idPeriodo}`, datos)
      .pipe(tap(() => this.limpiarCache(idAlojamiento)));
  }
  /**
   * Elimina un período sin reservas. 409 si tiene reservas activas.
   */
  eliminar(idAlojamiento: number, idPeriodo: number): Observable<RespuestaMensaje> {
    return this.http
      .delete<RespuestaMensaje>(`${this.base(idAlojamiento)}/${idPeriodo}`)
      .pipe(tap(() => this.limpiarCache(idAlojamiento)));
  }
}
