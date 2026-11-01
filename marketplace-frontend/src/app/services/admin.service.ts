import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { Alojamiento } from '../models/alojamientoDto';
import { ConfiguracionTarifa } from '../models/configuracionTarifaDto';
import { EstadoAlojamiento } from '../models/estadoAlojamientoDto';
import { EstadoResena } from '../models/estadoResenaDto';
import { EstadoReserva } from '../models/estadoReservaDto';
import { EventoAuditoria } from '../models/eventoAuditoriaDto';
import { Motivo } from '../models/motivoDto';
import { Resena } from '../models/resenaDto';
import { Reserva } from '../models/reservaDto';
import { Usuario } from '../models/usuarioDto';
import { API_URL } from './api-url';
/** Filtros opcionales de la auditoría. desde/hasta en ISO date-time (ej. 2026-10-31T00:00:00). */
export interface FiltrosAuditoria {
  accion?: string;
  idActor?: number;
  desde?: string;
  hasta?: string;
}
/** Página del backend: contenido más los totales para paginar. */
export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}
/** Acciones de moderación sobre un alojamiento. */
export type AccionAlojamiento = 'aprobar' | 'rechazar' | 'bloquear' | 'desbloquear';
export const ESTADOS_ALOJAMIENTO = [
  { valor: 'PENDIENTE_REVISION', texto: 'Pendiente de revisión' },
  { valor: 'ACTIVA', texto: 'Activa' },
  { valor: 'PAUSADA', texto: 'Pausada' },
  { valor: 'BLOQUEADA', texto: 'Bloqueada' },
  { valor: 'BORRADOR', texto: 'Borrador' },
];
export interface ConfigAccion {
  titulo: string;
  texto: string;
  etiquetaMotivo: string;
  confirmar: string;
  boton: string;
  requiereMotivo: boolean;
  /** Acción destructiva: se pinta en rojo. */
  peligro: boolean;
  /** Estado en el que queda el alojamiento si la acción se aplica. */
  resultado: string;
}
export const ACCIONES_ALOJAMIENTO: Record<AccionAlojamiento, ConfigAccion> = {
  aprobar: {
    titulo: 'Aprobar alojamiento',
    texto: 'Pasará a ACTIVA y aparecerá en los resultados públicos.',
    etiquetaMotivo: '',
    confirmar: 'Confirmar aprobación',
    boton: 'Aprobar',
    requiereMotivo: false,
    peligro: false,
    resultado: 'ACTIVA',
  },
  rechazar: {
    titulo: 'Rechazar alojamiento',
    texto: 'Volverá a borrador para que el anfitrión lo corrija. El motivo es obligatorio.',
    etiquetaMotivo: 'Motivo del rechazo',
    confirmar: 'Confirmar rechazo',
    boton: 'Rechazar',
    requiereMotivo: true,
    peligro: true,
    resultado: 'BORRADOR',
  },
  bloquear: {
    titulo: 'Bloquear publicación',
    texto: 'Dejará de aparecer en las búsquedas y sus reservas futuras pasarán a EN_REVISION.',
    etiquetaMotivo: 'Motivo del bloqueo',
    confirmar: 'Confirmar bloqueo',
    boton: 'Bloquear',
    requiereMotivo: true,
    peligro: true,
    resultado: 'BLOQUEADA',
  },
  desbloquear: {
    titulo: 'Desbloquear publicación',
    texto: 'Pasará a PAUSADA: el anfitrión debe reanudarla para que vuelva a publicarse.',
    etiquetaMotivo: '',
    confirmar: 'Confirmar desbloqueo',
    boton: 'Desbloquear',
    requiereMotivo: false,
    peligro: false,
    resultado: 'PAUSADA',
  },
};
/** Acciones que el administrador puede aplicar según el estado actual. */
export function accionesDe(estado: string): AccionAlojamiento[] {
  switch (estado) {
    case 'PENDIENTE_REVISION':
      return ['aprobar', 'rechazar'];
    case 'ACTIVA':
      return ['bloquear'];
    case 'BLOQUEADA':
      return ['desbloquear'];
    default:
      return []; // PAUSADA y BORRADOR dependen del anfitrión
  }
}
/** Primer valor no vacío entre varias claves posibles (los DTO pueden nombrarlas distinto). */
export function valorDe(obj: unknown, ...claves: string[]): string {
  if (!obj || typeof obj !== 'object') {
    return '';
  }
  const o = obj as Record<string, unknown>;
  for (const clave of claves) {
    const v = o[clave];
    if (v !== undefined && v !== null && v !== '' && typeof v !== 'object') {
      return String(v);
    }
  }
  return '';
}
export function estadoDe(a: Alojamiento | null | undefined): string {
  return valorDe(a, 'estado');
}
/** Copia del alojamiento con otro estado (para reflejar el resultado de una acción). */
export function conEstado(a: Alojamiento, estado: string): Alojamiento {
  return { ...a, estado } as Alojamiento;
}
export function etiquetaEstado(estado: string): string {
  return ESTADOS_ALOJAMIENTO.find((e) => e.valor === estado)?.texto ?? (estado || 'Sin estado');
}
export function claseBadge(estado: string): string {
  const tono =
    estado === 'ACTIVA'
      ? 'bg-emerald-50 text-emerald-700'
      : estado === 'BLOQUEADA'
        ? 'bg-red-50 text-red-700'
        : estado === 'PENDIENTE_REVISION'
          ? 'bg-amber-50 text-amber-700'
          : 'bg-slate-100 text-slate-600';
  return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tono;
}
/**
 * Cliente HTTP de /admin. Todo requiere rol ADMIN (el token lo agrega el interceptor).
 * El backend pagina las listas; aquí se devuelve solo el arreglo (`contenido`) de la primera página.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private base = `${inject(API_URL)}/admin`;
  // ================= Caché =================
  /** Tiempo que se reutiliza una respuesta antes de volver a pedirla. */
  private readonly TTL_MS = 60_000;
  private readonly cache = new Map<string, { expira: number; fuente$: Observable<unknown> }>();
  /** Reutiliza la misma petición mientras no venza el TTL; los errores no se guardan. */
  private cacheado<T>(clave: string, pedir: () => Observable<T>, ttl = this.TTL_MS): Observable<T> {
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
  /** Descarta lo guardado cuya clave incluya alguna de las rutas (ej. 'alojamientos'). */
  private invalidar(...rutas: string[]): void {
    for (const clave of [...this.cache.keys()]) {
      if (rutas.some((ruta) => clave.includes(`/${ruta}`))) {
        this.cache.delete(clave);
      }
    }
  }
  /** Fuerza a volver a pedir todo al backend. */
  limpiarCache(): void {
    this.cache.clear();
  }
  // ================= Usuarios =================
  /** Todos los usuarios. */
  usuarios(): Observable<Usuario[]> {
    return this.lista<Usuario>(`${this.base}/usuarios?pagina=0&tamanio=100`);
  }
  /** Reservas de un usuario como huésped. */
  reservasDeUsuario(id: number): Observable<Reserva[]> {
    return this.lista<Reserva>(`${this.base}/usuarios/${id}/reservas`);
  }
  /** Alojamientos de un usuario como anfitrión, en cualquier estado. */
  alojamientosDeUsuario(id: number): Observable<Alojamiento[]> {
    return this.lista<Alojamiento>(`${this.base}/usuarios/${id}/alojamientos`);
  }
  /** Restringe un usuario (motivo obligatorio; no aplica a administradores). */
  restringir(id: number, motivo: string): Observable<Usuario> {
    const cuerpo: Motivo = { motivo };
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/restringir`, cuerpo).pipe(tap(() => this.invalidar('usuarios', 'auditoria')));
  }
  /** Libera un usuario restringido (motivo opcional). */
  liberar(id: number, motivo?: string): Observable<Usuario> {
    const cuerpo: Motivo = { motivo };
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/liberar`, cuerpo).pipe(tap(() => this.invalidar('usuarios', 'auditoria')));
  }
  /** Total de usuarios (usa totalElementos de una página de 1 elemento). */
  totalUsuarios(): Observable<number> {
    return this.conteo(`${this.base}/usuarios`);
  }
  // ================= Alojamientos =================
  /** Alojamientos de cualquier estado, o solo los del estado indicado (ej. PENDIENTE_REVISION). */
  alojamientos(estado?: EstadoAlojamiento): Observable<Alojamiento[]> {
    return this.lista<Alojamiento>(`${this.base}/alojamientos`, { estado });
  }
  /** Total de alojamientos, de cualquier estado o solo del indicado. */
  totalAlojamientos(estado?: EstadoAlojamiento): Observable<number> {
    return this.conteo(`${this.base}/alojamientos`, { estado });
  }
  /** Una página de alojamientos con sus totales, opcionalmente filtrada por estado. */
  alojamientosPagina(pagina = 0, tamanio = 20, estado?: EstadoAlojamiento): Observable<Pagina<Alojamiento>> {
    return this.paginado<Alojamiento>(`${this.base}/alojamientos`, { pagina, tamanio, estado });
  }
  /** Todos los alojamientos (páginas de 100, hasta `maxPaginas`). */
  todosLosAlojamientos(
    estado?: EstadoAlojamiento,
    maxPaginas = 20,
  ): Observable<{ alojamientos: Alojamiento[]; total: number }> {
    return this.todasLasPaginas((p) => this.alojamientosPagina(p, 100, estado), maxPaginas).pipe(
      map((r) => ({ alojamientos: r.items, total: r.total })),
    );
  }
  /** Aplica una acción de moderación; devuelve el alojamiento actualizado. */
  ejecutarAccion(accion: AccionAlojamiento, id: number, motivo = ''): Observable<Alojamiento> {
    switch (accion) {
      case 'aprobar':
        return this.aprobar(id);
      case 'rechazar':
        return this.rechazar(id, motivo);
      case 'bloquear':
        return this.bloquear(id, motivo);
      case 'desbloquear':
        return this.desbloquear(id);
    }
  }
  /** PENDIENTE_REVISION → ACTIVA. */
  aprobar(id: number): Observable<Alojamiento> {
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/aprobar`, {}).pipe(tap(() => this.invalidar('alojamientos', 'reservas', 'auditoria')));
  }
  /** PENDIENTE_REVISION → BORRADOR, con motivo obligatorio. */
  rechazar(id: number, motivo: string): Observable<Alojamiento> {
    const cuerpo: Motivo = { motivo };
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/rechazar`, cuerpo).pipe(tap(() => this.invalidar('alojamientos', 'reservas', 'auditoria')));
  }
  /** Bloquea con motivo obligatorio (sus reservas futuras pasan a EN_REVISION). */
  bloquear(id: number, motivo: string): Observable<Alojamiento> {
    const cuerpo: Motivo = { motivo };
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/bloquear`, cuerpo).pipe(tap(() => this.invalidar('alojamientos', 'reservas', 'auditoria')));
  }
  /** BLOQUEADA → PAUSADA. */
  desbloquear(id: number): Observable<Alojamiento> {
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/desbloquear`, {}).pipe(tap(() => this.invalidar('alojamientos', 'reservas', 'auditoria')));
  }
  // ================= Reservas =================
  /** Todas las reservas, opcionalmente filtradas por estado. */
  reservas(estado?: EstadoReserva): Observable<Reserva[]> {
    return this.lista<Reserva>(`${this.base}/reservas`, { estado });
  }
  /** Una página de reservas con sus totales, opcionalmente filtrada por estado. */
  reservasPagina(pagina = 0, tamanio = 20, estado?: EstadoReserva): Observable<Pagina<Reserva>> {
    return this.paginado<Reserva>(`${this.base}/reservas`, { pagina, tamanio, estado });
  }
  /**
   * Recorre todas las páginas de reservas (de 100 en 100, hasta `maxPaginas`) para calcular
   * estadísticas. `total` es el real; si es mayor que reservas.length, la muestra es parcial.
   */
  todasLasReservas(maxPaginas = 20): Observable<{ reservas: Reserva[]; total: number }> {
    return this.todasLasPaginas((p) => this.reservasPagina(p, 100), maxPaginas).pipe(
      map((r) => ({ reservas: r.items, total: r.total })),
    );
  }
  // ================= Reseñas =================
  /** Todas las reseñas para moderar, opcionalmente filtradas por estado. */
  resenas(estado?: EstadoResena): Observable<Resena[]> {
    return this.lista<Resena>(`${this.base}/resenas`, { estado });
  }
  /** Una página de reseñas con sus totales, opcionalmente filtrada por estado. */
  resenasPagina(pagina = 0, tamanio = 20, estado?: EstadoResena): Observable<Pagina<Resena>> {
    return this.paginado<Resena>(`${this.base}/resenas`, { pagina, tamanio, estado });
  }
  /** Todas las reseñas (páginas de 100, hasta `maxPaginas`). */
  todasLasResenas(maxPaginas = 20): Observable<{ resenas: Resena[]; total: number }> {
    return this.todasLasPaginas((p) => this.resenasPagina(p, 100), maxPaginas).pipe(
      map((r) => ({ resenas: r.items, total: r.total })),
    );
  }
  /** Oculta una reseña (motivo obligatorio; no se borra). */
  ocultarResena(id: number, motivo: string): Observable<Resena> {
    const cuerpo: Motivo = { motivo };
    return this.http.patch<Resena>(`${this.base}/resenas/${id}/ocultar`, cuerpo).pipe(tap(() => this.invalidar('resenas', 'auditoria')));
  }
  /** Vuelve a mostrar una reseña oculta. */
  mostrarResena(id: number): Observable<Resena> {
    return this.http.patch<Resena>(`${this.base}/resenas/${id}/mostrar`, {}).pipe(tap(() => this.invalidar('resenas', 'auditoria')));
  }
  // ================= Tarifas =================
  /** Porcentajes globales vigentes de limpieza y servicio. */
  tarifas(): Observable<ConfiguracionTarifa> {
    return this.cacheado(`${this.base}/tarifas`, () => this.http.get<ConfiguracionTarifa>(`${this.base}/tarifas`), 5 * 60_000);
  }
  /** Cambia los porcentajes globales (0 a 100; aplican a cotizaciones nuevas). */
  actualizarTarifas(datos: ConfiguracionTarifa): Observable<ConfiguracionTarifa> {
    return this.http.put<ConfiguracionTarifa>(`${this.base}/tarifas`, datos).pipe(tap(() => this.invalidar('tarifas', 'auditoria')));
  }
  // ================= Auditoría =================
  /** Eventos de auditoría, del más reciente al más antiguo, con filtros opcionales. */
  auditoria(filtros: FiltrosAuditoria = {}): Observable<EventoAuditoria[]> {
    return this.lista<EventoAuditoria>(`${this.base}/auditoria`, { ...filtros });
  }
  /** Detalle de un evento de auditoría. */
  evento(id: number): Observable<EventoAuditoria> {
    return this.cacheado(`${this.base}/auditoria/${id}`, () => this.http.get<EventoAuditoria>(`${this.base}/auditoria/${id}`));
  }
  // ================= Utilidades =================
  /** Mensaje legible a partir de un error HTTP del backend. */
  mensajeDeError(error: unknown, porDefecto: string): string {
    const e = error as HttpErrorResponse;
    if (e?.status === 0) {
      return 'No hay conexión con el servidor.';
    }
    const cuerpo = e?.error;
    if (typeof cuerpo === 'string' && cuerpo.trim()) {
      return cuerpo;
    }
    return cuerpo?.mensaje ?? cuerpo?.message ?? cuerpo?.error ?? porDefecto;
  }
  /** GET de una lista paginada del backend; devuelve solo el arreglo `contenido`. */
  private lista<T>(url: string, valores: Record<string, unknown> = {}): Observable<T[]> {
    const params = this.params(valores);
    return this.cacheado(`${url}|${params.toString()}`, () =>
      this.http
        .get<{ contenido?: T[]; content?: T[] }>(url, { params })
        .pipe(map((r) => r?.contenido ?? r?.content ?? [])),
    );
  }
  /** Pide la primera página y, si hay más, el resto en paralelo (hasta `maxPaginas`). */
  private todasLasPaginas<T>(
    pedir: (pagina: number) => Observable<Pagina<T>>,
    maxPaginas: number,
  ): Observable<{ items: T[]; total: number }> {
    return pedir(0).pipe(
      switchMap((primera) => {
        const paginas = Math.min(primera.totalPaginas, maxPaginas);
        if (paginas <= 1) {
          return of({ items: primera.contenido, total: primera.totalElementos });
        }
        const resto = Array.from({ length: paginas - 1 }, (_, i) => pedir(i + 1));
        return forkJoin(resto).pipe(
          map((otras) => ({
            items: [primera.contenido, ...otras.map((p) => p.contenido)].flat(),
            total: primera.totalElementos,
          })),
        );
      }),
    );
  }
  /** GET de una lista paginada conservando los totales. */
  private paginado<T>(url: string, valores: Record<string, unknown> = {}): Observable<Pagina<T>> {
    const params = this.params(valores);
    return this.cacheado(`${url}|${params.toString()}`, () =>
      this.http
        .get<Partial<Pagina<T>> & { content?: T[]; totalElements?: number; totalPages?: number }>(url, { params })
        .pipe(
          map((r) => ({
            contenido: r?.contenido ?? r?.content ?? [],
            pagina: r?.pagina ?? 0,
            tamanio: r?.tamanio ?? 0,
            totalElementos: r?.totalElementos ?? r?.totalElements ?? 0,
            totalPaginas: r?.totalPaginas ?? r?.totalPages ?? 1,
          })),
        ),
    );
  }
  /** Cantidad total de una lista: pide 1 elemento y lee totalElementos. */
  private conteo(url: string, valores: Record<string, unknown> = {}): Observable<number> {
    return this.paginado<unknown>(url, { ...valores, pagina: 0, tamanio: 1 }).pipe(
      map((p) => p.totalElementos),
    );
  }
  /** Arma HttpParams omitiendo valores vacíos. */
  private params(valores: Record<string, unknown>): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(valores)) {
      if (valor === undefined || valor === null || valor === '') continue;
      params = params.set(clave, String(valor));
    }
    return params;
  }
}
