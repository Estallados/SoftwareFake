import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { Alojamiento } from '../models/alojamientoDto';
import { SessionService } from './session.service';
import {API_URL} from './api-url';
/** El backend puede devolver el alojamiento directo o envuelto en el favorito. */
type ItemFavorito = Alojamiento | { alojamiento: Alojamiento };

interface PaginaFavoritos {
  contenido?: ItemFavorito[];
  content?: ItemFavorito[];
}

@Injectable({ providedIn: 'root' })
export class FavoritoService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);
  // Ajusta la ruta base si tu @RequestMapping del controlador es distinto
  private readonly baseUrl = `${inject(API_URL)}/favoritos`;
  // ==================== ESTADO COMPARTIDO ====================
  /** Ids de alojamientos favoritos, compartidos por todos los componentes. */
  readonly favoritos = signal<number[]>([]);
  /** Mensaje temporal de error (lo muestra <app-favorito-toast />). */
  readonly mensaje = signal<string | null>(null);
  /** Ids con una petición en curso (evita dobles clics). */
  private readonly pendientes = new Set<number>();
  private temporizadorMensaje?: ReturnType<typeof setTimeout>;
  // ==================== HTTP ====================
  /**
   * GET /favoritos?pagina=&tamanio=
   * Favoritos del usuario autenticado, del más reciente al más antiguo.
   * Devuelve una lista simple y sincroniza el estado compartido `favoritos`.
   */
  listar(pagina = 0, tamanio = 20): Observable<Alojamiento[]> {
    return this.http
      .get<PaginaFavoritos>(this.baseUrl, { params: { pagina, tamanio } })
      .pipe(
        map((r) =>
          (r.contenido ?? r.content ?? []).map((item) =>
            'alojamiento' in item ? item.alojamiento : item,
          ),
        ),
        tap((lista) => this.sincronizar(lista)),
      );
  }

  private sincronizar(lista: Alojamiento[]): void {
    const ids = lista
      .map((a) => a.idAlojamiento)
      .filter((id): id is number => id != null);
    this.favoritos.update((actuales) => [...new Set([...actuales, ...ids])]);
  }

  /**
   * POST /favoritos/{idAlojamiento}
   * Idempotente: si ya estaba, responde 200 con el mismo favorito.
   * Errores: 404 si el alojamiento no existe.
   */
  agregar(idAlojamiento: number): Observable<unknown> {
    return this.http.post<unknown>(`${this.baseUrl}/${idAlojamiento}`, null);
  }
  /**
   * DELETE /favoritos/{idAlojamiento}
   * Errores: 404 si el alojamiento no estaba en favoritos.
   */
  eliminar(idAlojamiento: number): Observable<unknown> {
    return this.http.delete<unknown>(`${this.baseUrl}/${idAlojamiento}`);
  }
  // ==================== API PARA LOS COMPONENTES ====================
  esFavorito(id: number | undefined): boolean {
    return id != null && this.favoritos().includes(id);
  }
  /**
   * Alterna el favorito con actualización optimista:
   * cambia al instante y, si el servidor falla, revierte y avisa.
   */
  toggle(id: number | undefined): void {
    if (id == null || this.pendientes.has(id)) {
      return;
    }
    // Sin sesión vigente no tiene sentido llamar al backend (daría 401)
    if (!this.session.autenticado()) {
      this.mostrarMensaje('Inicia sesión para guardar alojamientos en favoritos.');
      return;
    }
    const eraFavorito = this.esFavorito(id);
    this.pendientes.add(id);
    this.setFavorito(id, !eraFavorito);
    const peticion = eraFavorito ? this.eliminar(id) : this.agregar(id);
    peticion.subscribe({
      next: () => this.pendientes.delete(id),
      error: (error: HttpErrorResponse) => {
        this.pendientes.delete(id);
        // Quitar algo que ya no estaba en favoritos (404) equivale a éxito.
        if (eraFavorito && error.status === 404) {
          return;
        }
        this.setFavorito(id, eraFavorito); // rollback
        console.error('Error al actualizar favorito:', error);
        this.mostrarMensaje(this.mensajeDeError(error, eraFavorito));
      },
    });
  }
  private setFavorito(id: number, esFavorito: boolean): void {
    this.favoritos.update((actuales) => {
      const yaEsta = actuales.includes(id);
      if (esFavorito) {
        return yaEsta ? actuales : [...actuales, id];
      }
      return yaEsta ? actuales.filter((f) => f !== id) : actuales;
    });
  }
  private mensajeDeError(error: HttpErrorResponse, eraFavorito: boolean): string {
    if (error.status === 401 || error.status === 403) {
      return 'Inicia sesión para guardar alojamientos en favoritos.';
    }
    if (error.status === 404) {
      return 'Este alojamiento ya no está disponible.';
    }
    return eraFavorito
      ? 'No se pudo quitar de favoritos. Inténtalo de nuevo.'
      : 'No se pudo guardar en favoritos. Inténtalo de nuevo.';
  }
  private mostrarMensaje(mensaje: string): void {
    this.mensaje.set(mensaje);
    clearTimeout(this.temporizadorMensaje);
    this.temporizadorMensaje = setTimeout(() => this.mensaje.set(null), 4000);
  }
}
