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
/**
 * Publicaciones de alojamiento: catálogo, búsqueda, detalle y, para el anfitrión, crear, editar,
 * imágenes, cambios de estado y servicios adicionales. Guarda en caché las consultas y la limpia
 * cuando algo cambia.
 */
@Injectable({
  providedIn: 'root',
})
export class AlojamientoService {
  /** Cliente HTTP. */
  private http = inject(HttpClient);

  /** Ruta base de los alojamientos. */
  private base = `${inject(API_URL)}/alojamientos`;
  // Cachés de consultas GET

  /** Catálogo guardado por tamaño de página. */
  private disponiblesCache = new Map<number, Observable<Alojamiento[]>>();

  /** Resultados guardados por búsqueda. */
  private busquedaCache = new Map<string, Observable<Alojamiento[]>>();

  /** Políticas de cancelación guardadas. */
  private politicasCache$?: Observable<PoliticaCancelacion[]>;

  /** Alojamientos del anfitrión guardados. */
  private miosCache$?: Observable<Alojamiento[]>;

  /** Detalle de cada alojamiento guardado. */
  private detalleCache = new Map<number, Observable<Alojamiento>>();

  /** Historial de estados de cada alojamiento guardado. */
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
   * Cantidad de alojamientos agrupados por tipo.
   * Incluye alojamientos en cualquier estado.
   */
  cantidadPorTipo(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `${this.base}/tipos`
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
      .get<{ contenido?: Alojamiento[]; content?: Alojamiento[] }>(this.base, { params })
      .pipe(
        map((r) => r?.contenido ?? r?.content ?? []),
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
        .get<{ contenido?: Alojamiento[]; content?: Alojamiento[] }>(`${this.base}/mios`)
        .pipe(
          map((r) => r?.contenido ?? r?.content ?? []),
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

  /**
   * POST /alojamientos. Crea el alojamiento en BORRADOR con sus imágenes (multipart: el JSON en
   * 'datos' y las imágenes en 'archivos').
   *
   * @param datos datos del alojamiento
   * @param archivos imágenes (la primera queda como portada)
   * @returns el alojamiento creado
   */
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

  /**
   * PUT /alojamientos/{id}. Edita los datos del alojamiento.
   *
   * @param id alojamiento
   * @param datos campos a cambiar
   * @returns el alojamiento actualizado
   */
  actualizar(
    id: number,
    datos: Partial<AlojamientoEntrada>,
  ): Observable<Alojamiento> {
    return this.http.put<Alojamiento>(`${this.base}/${id}`, datos).pipe(
      tap(() => this.invalidarAlojamiento(id)),
    );
  }
  // ================= Imágenes =================

  /**
   * POST /alojamientos/{id}/imagenes. Sube una o varias imágenes.
   *
   * @param id alojamiento
   * @param archivos imágenes a subir
   * @returns el alojamiento con sus imágenes
   */
  subirImagenes(id: number, archivos: File[]): Observable<Alojamiento> {
    const form = new FormData();
    archivos.forEach((f) => form.append('archivos', f));
    return this.http
      .post<Alojamiento>(`${this.base}/${id}/imagenes`, form)
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  /**
   * DELETE /alojamientos/{id}/imagenes/{idImagen}. Elimina una imagen.
   *
   * @param id alojamiento
   * @param idImagen imagen
   * @returns el alojamiento con las imágenes restantes
   */
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

  /**
   * PATCH /alojamientos/{id}/imagenes/{idImagen}/principal. Marca una imagen como portada.
   *
   * @param id alojamiento
   * @param idImagen imagen
   * @returns el alojamiento con sus imágenes
   */
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

  /**
   * POST /alojamientos/{id}/enviarrevision. Envía el borrador a revisión del administrador.
   *
   * @param id alojamiento
   * @returns el alojamiento en PENDIENTE_REVISION
   */
  enviarRevision(id: number): Observable<Alojamiento> {
    return this.http
      .post<Alojamiento>(`${this.base}/${id}/enviarrevision`, {})
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  /**
   * POST /alojamientos/{id}/pausar. Pausa la publicación.
   *
   * @param id alojamiento
   * @returns el alojamiento en PAUSADA
   */
  pausar(id: number): Observable<Alojamiento> {
    return this.http
      .post<Alojamiento>(`${this.base}/${id}/pausar`, {})
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }

  /**
   * POST /alojamientos/{id}/reanudar. Vuelve a publicar un alojamiento pausado.
   *
   * @param id alojamiento
   * @returns el alojamiento en ACTIVA
   */
  reanudar(id: number): Observable<Alojamiento> {
    return this.http
      .post<Alojamiento>(`${this.base}/${id}/reanudar`, {})
      .pipe(
        tap(() => this.invalidarAlojamiento(id)),
      );
  }
  // ================= Servicios adicionales =================

  /**
   * POST /alojamientos/{id}/servicios. Agrega un servicio adicional.
   *
   * @param id alojamiento
   * @param servicio nombre y costo
   * @returns el servicio creado
   */
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

  /**
   * PUT /alojamientos/{id}/servicios/{idServicio}. Modifica un servicio adicional.
   *
   * @param id alojamiento
   * @param idServicio servicio
   * @param servicio nombre y costo nuevos
   * @returns el servicio actualizado
   */
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

  /**
   * DELETE /alojamientos/{id}/servicios/{idServicio}. Elimina un servicio adicional.
   *
   * @param id alojamiento
   * @param idServicio servicio
   * @returns la respuesta con el mensaje
   */
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
  // ================= Presentación =================
  /**
   * Resumen corto de amenidades para tarjetas (ej. "WiFi · Cocina").
   */
  amenidadesResumen(stay: Alojamiento, max = 2): string {
    return (
      (stay.amenidades ?? []).slice(0, max).join(' · ') ||
      'Sin servicios indicados'
    );
  }
  // ================= Barra de búsqueda (Inicio y Explorar) =================
  /** Fecha de hoy (local) como YYYY-MM-DD. */
  fechaHoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  /** Valida la barra de búsqueda; devuelve el mensaje de error o null si está bien. */
  validarBusqueda(f: { entrada: string; salida: string; huespedes: string }): string | null {
    if (!!f.entrada !== !!f.salida) {
      return 'Indica la fecha de entrada y la de salida.';
    }
    if (f.entrada && f.entrada < this.fechaHoy()) {
      return 'La fecha de entrada no puede estar en el pasado.';
    }
    if (f.entrada && f.salida <= f.entrada) {
      return 'La salida debe ser posterior a la entrada.';
    }
    const n = Number(f.huespedes);
    if (f.huespedes.trim() && (!Number.isInteger(n) || n < 1)) {
      return 'Los huéspedes deben ser un número mayor que cero.';
    }
    return null;
  }
  /** Parámetros de la URL de Explorar (ciudad, llegada, salida, huespedes); omite los vacíos. */
  paramsBusqueda(f: { ciudad: string; entrada: string; salida: string; huespedes: string }): Record<string, string | number> {
    const p: Record<string, string | number> = {};
    if (f.ciudad.trim()) {
      p['ciudad'] = f.ciudad.trim();
    }
    if (f.entrada && f.salida) {
      p['llegada'] = f.entrada;
      p['salida'] = f.salida;
    }
    if (f.huespedes.trim()) {
      p['huespedes'] = Number(f.huespedes);
    }
    return p;
  }
  // ================= Utilidades =================

  /**
   * Arma los parámetros de la URL; omite los valores vacíos y repite la clave para las listas.
   *
   * @param valores valores a enviar
   * @returns parámetros HTTP
   */
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
