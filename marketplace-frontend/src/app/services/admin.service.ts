import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
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

/**
 * Cliente HTTP de /admin. Todo requiere rol ADMIN (el token lo agrega el interceptor).
 * El backend pagina las listas; aquí se devuelve solo el arreglo (`contenido`) de la primera página.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private base = `${inject(API_URL)}/admin`;

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
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/restringir`, cuerpo);
  }

  /** Libera un usuario restringido (motivo opcional). */
  liberar(id: number, motivo?: string): Observable<Usuario> {
    const cuerpo: Motivo = { motivo };
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/liberar`, cuerpo);
  }

  // ================= Alojamientos =================

  /** Alojamientos de cualquier estado, o solo los del estado indicado (ej. PENDIENTE_REVISION). */
  alojamientos(estado?: EstadoAlojamiento): Observable<Alojamiento[]> {
    return this.lista<Alojamiento>(`${this.base}/alojamientos`, { estado });
  }

  /** PENDIENTE_REVISION → ACTIVA. */
  aprobar(id: number): Observable<Alojamiento> {
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/aprobar`, {});
  }

  /** PENDIENTE_REVISION → BORRADOR, con motivo obligatorio. */
  rechazar(id: number, motivo: string): Observable<Alojamiento> {
    const cuerpo: Motivo = { motivo };
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/rechazar`, cuerpo);
  }

  /** Bloquea con motivo obligatorio (sus reservas futuras pasan a EN_REVISION). */
  bloquear(id: number, motivo: string): Observable<Alojamiento> {
    const cuerpo: Motivo = { motivo };
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/bloquear`, cuerpo);
  }

  /** BLOQUEADA → PAUSADA. */
  desbloquear(id: number): Observable<Alojamiento> {
    return this.http.post<Alojamiento>(`${this.base}/alojamientos/${id}/desbloquear`, {});
  }

  // ================= Reservas =================

  /** Todas las reservas, opcionalmente filtradas por estado. */
  reservas(estado?: EstadoReserva): Observable<Reserva[]> {
    return this.lista<Reserva>(`${this.base}/reservas`, { estado });
  }

  // ================= Reseñas =================

  /** Todas las reseñas para moderar, opcionalmente filtradas por estado. */
  resenas(estado?: EstadoResena): Observable<Resena[]> {
    return this.lista<Resena>(`${this.base}/resenas`, { estado });
  }

  /** Oculta una reseña (motivo obligatorio; no se borra). */
  ocultarResena(id: number, motivo: string): Observable<Resena> {
    const cuerpo: Motivo = { motivo };
    return this.http.patch<Resena>(`${this.base}/resenas/${id}/ocultar`, cuerpo);
  }

  /** Vuelve a mostrar una reseña oculta. */
  mostrarResena(id: number): Observable<Resena> {
    return this.http.patch<Resena>(`${this.base}/resenas/${id}/mostrar`, {});
  }

  // ================= Tarifas =================

  /** Porcentajes globales vigentes de limpieza y servicio. */
  tarifas(): Observable<ConfiguracionTarifa> {
    return this.http.get<ConfiguracionTarifa>(`${this.base}/tarifas`);
  }

  /** Cambia los porcentajes globales (0 a 100; aplican a cotizaciones nuevas). */
  actualizarTarifas(datos: ConfiguracionTarifa): Observable<ConfiguracionTarifa> {
    return this.http.put<ConfiguracionTarifa>(`${this.base}/tarifas`, datos);
  }

  // ================= Auditoría =================

  /** Eventos de auditoría, del más reciente al más antiguo, con filtros opcionales. */
  auditoria(filtros: FiltrosAuditoria = {}): Observable<EventoAuditoria[]> {
    return this.lista<EventoAuditoria>(`${this.base}/auditoria`, { ...filtros });
  }

  /** Detalle de un evento de auditoría. */
  evento(id: number): Observable<EventoAuditoria> {
    return this.http.get<EventoAuditoria>(`${this.base}/auditoria/${id}`);
  }

  // ================= Utilidades =================

  /** GET de una lista paginada del backend; devuelve solo el arreglo `contenido`. */
  private lista<T>(url: string, valores: Record<string, unknown> = {}): Observable<T[]> {
    return this.http
      .get<{ contenido?: T[]; content?: T[] }>(url, { params: this.params(valores) })
      .pipe(map((r) => r?.contenido ?? r?.content ?? []));
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
