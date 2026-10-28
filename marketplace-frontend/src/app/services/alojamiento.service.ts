import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {Observable, map, tap} from 'rxjs';
import { AlojamientoEntrada } from '../models/alojamientoEntradaDto';
import { Alojamiento } from '../models/alojamientoDto';
import { FiltrosBusqueda } from '../models/filtrosBusquedaDto';
import { HistorialEstadoAlojamiento } from '../models/historialEstadoAlojamientoDto';
import { OrdenAlojamientos } from '../models/ordenAlojamientosDto';
import { PoliticaCancelacion } from '../models/politicaCancelacionDto';
import { RespuestaMensaje } from '../models/respuestaMensajeDto';
import { ServicioAdicional } from '../models/servicioAdicionalDto';
import { API_URL } from './api-url';

/**
 * Cliente HTTP de /alojamientos.
 * Públicos: disponibles, buscar, politicas y el detalle de una publicación ACTIVA.
 * El resto requiere token (lo agrega el interceptor de autenticación).
 *
 * Los listados devuelven un arreglo simple: el backend responde con una página
 * (campo `content`) y aquí se extrae solo ese contenido, sin enviar pagina/tamanio.
 */
@Injectable({ providedIn: 'root' })
export class AlojamientoService {
  private http = inject(HttpClient);
  private base = `${inject(API_URL)}/alojamientos`;

  // ================= Consulta =================

  /** Catálogo de alojamientos disponibles para reservar (público). */
  disponibles(tamanio: number): Observable<Alojamiento[]> {
    const url = `${this.base}/disponibles?pagina=0&tamanio=${tamanio}`;
    console.log('[AlojamientoService] disponibles() -> GET', url);

    return this.http
      .get<{ contenido: Alojamiento[] }>(url)
      .pipe(
        tap({
          next: (r) =>
            console.log('[AlojamientoService] respuesta OK. contenido:', r?.contenido?.length, 'items', r),
          error: (e) =>
            console.error('[AlojamientoService] ERROR status:', e.status, 'url:', e.url, e),
          complete: () => console.log('[AlojamientoService] petición completada'),
        }),
        map((r) => r.contenido),
      );
  }

  /** Búsqueda con filtros opcionales y combinables (público). */
  buscar(filtros: FiltrosBusqueda = {}): Observable<Alojamiento[]> {
    return this.http
      .get<{ content: Alojamiento[] }>(this.base, { params: this.params({ ...filtros }) })
      .pipe(map((r) => r.content));
  }

  /** Políticas de cancelación disponibles (público). */
  politicas(): Observable<PoliticaCancelacion[]> {
    return this.http.get<PoliticaCancelacion[]>(`${this.base}/politicas`);
  }

  /** Mis alojamientos con su estado (ANFITRION). */
  mios(): Observable<Alojamiento[]> {
    return this.http
      .get<{ content: Alojamiento[] }>(`${this.base}/mios`)
      .pipe(map((r) => r.content));
  }

  /** Detalle: público si está ACTIVA; en otro estado solo su anfitrión o un admin. */
  obtener(id: number): Observable<Alojamiento> {
    return this.http.get<Alojamiento>(`${this.base}/${id}`);
  }

  /** Historial de estados (propietario o ADMIN). */
  historial(id: number): Observable<HistorialEstadoAlojamiento[]> {
    return this.http.get<HistorialEstadoAlojamiento[]>(`${this.base}/${id}/historial`);
  }

  // ================= Creación y edición =================

  /**
   * Crea un alojamiento en BORRADOR con sus imágenes (ANFITRION).
   * multipart/form-data: 'datos' (JSON como texto) y 'archivos' (1 a 15 imágenes JPG/PNG/WEBP, máx. 5 MB).
   * La primera imagen queda como principal.
   */
  crear(datos: AlojamientoEntrada, archivos: File[] = []): Observable<Alojamiento> {
    const form = new FormData();
    form.append('datos', JSON.stringify(datos));
    archivos.forEach((f) => form.append('archivos', f));
    // No fijes Content-Type: el navegador agrega el boundary del multipart.
    return this.http.post<Alojamiento>(this.base, form);
  }

  /** Edita (propietario o ADMIN). Los campos undefined/null no cambian; las amenidades reemplazan las anteriores. */
  actualizar(id: number, datos: Partial<AlojamientoEntrada>): Observable<Alojamiento> {
    return this.http.put<Alojamiento>(`${this.base}/${id}`, datos);
  }

  // ================= Imágenes =================

  /** Sube imágenes (máx. 15 por alojamiento). Si no había portada, la primera queda como principal. */
  subirImagenes(id: number, archivos: File[]): Observable<Alojamiento> {
    const form = new FormData();
    archivos.forEach((f) => form.append('archivos', f));
    return this.http.post<Alojamiento>(`${this.base}/${id}/imagenes`, form);
  }

  /** Elimina una imagen (409 si es la última del alojamiento). */
  eliminarImagen(id: number, idImagen: number): Observable<Alojamiento> {
    return this.http.delete<Alojamiento>(`${this.base}/${id}/imagenes/${idImagen}`);
  }

  /** Marca una imagen como principal. */
  marcarPrincipal(id: number, idImagen: number): Observable<Alojamiento> {
    return this.http.patch<Alojamiento>(`${this.base}/${id}/imagenes/${idImagen}/principal`, {});
  }

  // ================= Ciclo de estados =================

  /** BORRADOR → PENDIENTE_REVISION (ANFITRION). */
  enviarRevision(id: number): Observable<Alojamiento> {
    return this.http.post<Alojamiento>(`${this.base}/${id}/enviarrevision`, {});
  }

  /** ACTIVA → PAUSADA (ANFITRION). */
  pausar(id: number): Observable<Alojamiento> {
    return this.http.post<Alojamiento>(`${this.base}/${id}/pausar`, {});
  }

  /** PAUSADA → ACTIVA (ANFITRION). */
  reanudar(id: number): Observable<Alojamiento> {
    return this.http.post<Alojamiento>(`${this.base}/${id}/reanudar`, {});
  }

  // ================= Servicios adicionales =================

  agregarServicio(id: number, servicio: ServicioAdicional): Observable<ServicioAdicional> {
    return this.http.post<ServicioAdicional>(`${this.base}/${id}/servicios`, servicio);
  }

  actualizarServicio(id: number, idServicio: number, servicio: ServicioAdicional): Observable<ServicioAdicional> {
    return this.http.put<ServicioAdicional>(`${this.base}/${id}/servicios/${idServicio}`, servicio);
  }

  eliminarServicio(id: number, idServicio: number): Observable<RespuestaMensaje> {
    return this.http.delete<RespuestaMensaje>(`${this.base}/${id}/servicios/${idServicio}`);
  }

  // ================= Utilidades =================

  /** Arma HttpParams omitiendo valores vacíos; los arreglos se repiten como ?amenidades=a&amenidades=b. */
  private params(valores: Record<string, unknown>): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(valores)) {
      if (valor === undefined || valor === null || valor === '') continue;
      if (Array.isArray(valor)) {
        valor.forEach((v) => (params = params.append(clave, String(v))));
      } else {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
