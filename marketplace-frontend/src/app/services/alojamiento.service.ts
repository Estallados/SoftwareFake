import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  Observable,
  map,
  shareReplay,
  tap,
} from 'rxjs';

import { AlojamientoEntrada } from '../models/alojamientoEntradaDto';
import { Alojamiento } from '../models/alojamientoDto';
import { FiltrosBusqueda } from '../models/filtrosBusquedaDto';
import { HistorialEstadoAlojamiento } from '../models/historialEstadoAlojamientoDto';
import { PoliticaCancelacion } from '../models/politicaCancelacionDto';
import { RespuestaMensaje } from '../models/respuestaMensajeDto';
import { ServicioAdicional } from '../models/servicioAdicionalDto';
import { API_URL } from './api-url';
import {ResumenResenas} from '../models/resenaDto';

@Injectable({
  providedIn: 'root',
})
export class AlojamientoService {
  private http = inject(HttpClient);
  private base = `${inject(API_URL)}/alojamientos`;

  // Cachés de consultas GET
  private disponiblesCache = new Map<number, Observable<Alojamiento[]>>();
  private busquedaCache = new Map<string, Observable<Alojamiento[]>>();
  private politicasCache$?: Observable<PoliticaCancelacion[]>;
  private miosCache$?: Observable<Alojamiento[]>;
  private detalleCache = new Map<number, Observable<Alojamiento>>();
  private historialCache = new Map<number, Observable<HistorialEstadoAlojamiento[]>>();

  // ================= Consulta =================

  /**
   * Catálogo de reseñas de alojamiento especificado.
   */
  resenasDeAlojamiento(
    idAlojamiento: number
  ): Observable<ResumenResenas> {
    return this.http.get<ResumenResenas>(
      `${this.base}/${idAlojamiento}/resenas`
    );
  }


  /**
   * Catálogo de alojamientos disponibles.
   * Reutiliza la petición para un mismo tamaño.
   */
  disponibles(tamanio: number): Observable<Alojamiento[]> {
    const cached = this.disponiblesCache.get(tamanio);

    if (cached) {
      return cached;
    }

    const url = `${this.base}/disponibles?pagina=0&tamanio=${tamanio}`;

    const request$ = this.http
      .get<{ contenido: Alojamiento[] }>(url)
      .pipe(
        map((r) => r.contenido),
        tap({
          error: (e) =>
            console.error('[AlojamientoService] Error en disponibles:', e),
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    this.disponiblesCache.set(tamanio, request$);

    return request$;
  }

  /**
   * Búsqueda con filtros opcionales.
   * Comparte los resultados para filtros equivalentes.
   */
  buscar(filtros: FiltrosBusqueda = {}): Observable<Alojamiento[]> {
    const params = this.params({ ...filtros });

    const cacheKey = params
      .keys()
      .sort()
      .map((key) =>
        params
          .getAll(key)!
          .slice()
          .sort()
          .map((value) => `${key}=${encodeURIComponent(value)}`)
          .join('&'),
      )
      .join('&');

    const cached = this.busquedaCache.get(cacheKey);

    if (cached) {
      return cached;
    }

    const request$ = this.http
      .get<{ content: Alojamiento[] }>(this.base, { params })
      .pipe(
        map((r) => r.content),
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    this.busquedaCache.set(cacheKey, request$);

    return request$;
  }

  /**
   * Políticas de cancelación.
   */
  politicas(): Observable<PoliticaCancelacion[]> {
    if (!this.politicasCache$) {
      this.politicasCache$ = this.http
        .get<PoliticaCancelacion[]>(`${this.base}/politicas`)
        .pipe(
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }

    return this.politicasCache$;
  }

  /**
   * Alojamientos del anfitrión autenticado.
   */
  mios(): Observable<Alojamiento[]> {
    if (!this.miosCache$) {
      this.miosCache$ = this.http
        .get<{ content: Alojamiento[] }>(`${this.base}/mios`)
        .pipe(
          map((r) => r.content),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }

    return this.miosCache$;
  }

  /**
   * Detalle de un alojamiento.
   */
  obtener(id: number): Observable<Alojamiento> {
    const cached = this.detalleCache.get(id);

    if (cached) {
      return cached;
    }

    const request$ = this.http
      .get<Alojamiento>(`${this.base}/${id}`)
      .pipe(
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    this.detalleCache.set(id, request$);

    return request$;
  }

  /**
   * Historial de estados de un alojamiento.
   */
  historial(id: number): Observable<HistorialEstadoAlojamiento[]> {
    const cached = this.historialCache.get(id);

    if (cached) {
      return cached;
    }

    const request$ = this.http
      .get<HistorialEstadoAlojamiento[]>(`${this.base}/${id}/historial`)
      .pipe(
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    this.historialCache.set(id, request$);

    return request$;
  }

  // ================= Creación y edición =================

  crear(
    datos: AlojamientoEntrada,
    archivos: File[] = [],
  ): Observable<Alojamiento> {
    const form = new FormData();

    form.append('datos', JSON.stringify(datos));
    archivos.forEach((f) => form.append('archivos', f));

    return this.http.post<Alojamiento>(this.base, form).pipe(
      tap(() => this.invalidarListados()),
    );
  }

  actualizar(
    id: number,
    datos: Partial<AlojamientoEntrada>,
  ): Observable<Alojamiento> {
    return this.http.put<Alojamiento>(`${this.base}/${id}`, datos).pipe(
      tap(() => this.invalidarAlojamiento(id)),
    );
  }

  // ================= Imágenes =================

  subirImagenes(id: number, archivos: File[]): Observable<Alojamiento> {
    const form = new FormData();

    archivos.forEach((f) => form.append('archivos', f));

    return this.http
      .post<Alojamiento>(`${this.base}/${id}/imagenes`, form)
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  eliminarImagen(
    id: number,
    idImagen: number,
  ): Observable<Alojamiento> {
    return this.http
      .delete<Alojamiento>(`${this.base}/${id}/imagenes/${idImagen}`)
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  marcarPrincipal(
    id: number,
    idImagen: number,
  ): Observable<Alojamiento> {
    return this.http
      .patch<Alojamiento>(
        `${this.base}/${id}/imagenes/${idImagen}/principal`,
        {},
      )
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  // ================= Ciclo de estados =================

  enviarRevision(id: number): Observable<Alojamiento> {
    return this.http
      .post<Alojamiento>(`${this.base}/${id}/enviarrevision`, {})
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  pausar(id: number): Observable<Alojamiento> {
    return this.http
      .post<Alojamiento>(`${this.base}/${id}/pausar`, {})
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  reanudar(id: number): Observable<Alojamiento> {
    return this.http
      .post<Alojamiento>(`${this.base}/${id}/reanudar`, {})
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  // ================= Servicios adicionales =================

  agregarServicio(
    id: number,
    servicio: ServicioAdicional,
  ): Observable<ServicioAdicional> {
    return this.http
      .post<ServicioAdicional>(`${this.base}/${id}/servicios`, servicio)
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  actualizarServicio(
    id: number,
    idServicio: number,
    servicio: ServicioAdicional,
  ): Observable<ServicioAdicional> {
    return this.http
      .put<ServicioAdicional>(
        `${this.base}/${id}/servicios/${idServicio}`,
        servicio,
      )
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  eliminarServicio(
    id: number,
    idServicio: number,
  ): Observable<RespuestaMensaje> {
    return this.http
      .delete<RespuestaMensaje>(
        `${this.base}/${id}/servicios/${idServicio}`,
      )
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  // ================= Caché =================

  /**
   * Invalida listados que pueden haber cambiado.
   */
  private invalidarListados(): void {
    this.disponiblesCache.clear();
    this.busquedaCache.clear();
    this.miosCache$ = undefined;
  }

  /**
   * Invalida el detalle y el historial de un alojamiento.
   */
  private invalidarAlojamiento(id: number): void {
    this.detalleCache.delete(id);
    this.historialCache.delete(id);
    this.invalidarListados();
  }

  /**
   * Permite forzar la recarga de todos los datos almacenados.
   */
  limpiarCache(): void {
    this.disponiblesCache.clear();
    this.busquedaCache.clear();
    this.politicasCache$ = undefined;
    this.miosCache$ = undefined;
    this.detalleCache.clear();
    this.historialCache.clear();
  }

  // ================= Utilidades =================

  private params(valores: Record<string, unknown>): HttpParams {
    let params = new HttpParams();

    for (const [clave, valor] of Object.entries(valores)) {
      if (valor === undefined || valor === null || valor === '') {
        continue;
      }

      if (Array.isArray(valor)) {
        valor.forEach((v) => {
          params = params.append(clave, String(v));
        });
      } else {
        params = params.set(clave, String(valor));
      }
    }

    return params;
  }
}
