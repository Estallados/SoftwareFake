import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { API_URL } from './api-url';
import { SessionService } from './session.service';
/** Mensaje ya normalizado para la pantalla. */
export interface Mensaje {

  id: number;
  
  texto: string;
  /** ISO date-time. */
  fecha: string;
  /** true si lo escribió el usuario de la sesión. */
  mio: boolean;
}
/** Conversación ya normalizada para la pantalla. */
export interface Conversacion {

  id: number;
  /** El otro participante. */
  contacto: string;
  /** Nombre del anfitrión del alojamiento de la conversación. */
  anfitrion: string;
  
  iniciales: string;
 
  alojamiento: string;
  /** Id del alojamiento relacionado (null si el backend no lo envía). */
  idAlojamiento: number | null;
  
  reserva: string;
  
  noLeidos: number;
 
  ultimo: string;
}

type Crudo = Record<string, unknown>;

/**
 * Primer valor no vacío entre varias claves posibles del DTO, como texto.
 * Conserva los booleanos (false → 'false') para que esMio=false no se confunda con "sin dato".
 */
function valorDe(origen: Crudo, ...claves: string[]): string {
  for (const clave of claves) {
    const valor = origen?.[clave];
    if (valor !== undefined && valor !== null && String(valor).trim() !== '') {
      return String(valor);
    }
  }
  return '';
}

type PaginaCruda = { contenido?: Crudo[]; content?: Crudo[]; totalPaginas?: number; totalPages?: number };

@Injectable({
  providedIn: 'root',
})
export class MensajeriaService {

  private http = inject(HttpClient);

  private session = inject(SessionService);

  private base = `${inject(API_URL)}/conversaciones`;
  /** Mis conversaciones (primera página, las más recientes). */
  conversaciones(tamanio = 50): Observable<Conversacion[]> {
    return this.http
      .get<PaginaCruda>(this.base, { params: { pagina: 0, tamanio } })
      .pipe(map((p) => (p?.contenido ?? p?.content ?? []).map((c) => this.aConversacion(c))));
  }
  /**
   * Mensajes de una conversación, del más antiguo al más reciente.
   * Si hay varias páginas del servidor se piden todas (hasta `maxPaginas`).
   */
  mensajes(idConversacion: number, maxPaginas = 10): Observable<Mensaje[]> {
    const pedir = (pagina: number) =>
      this.http.get<PaginaCruda>(`${this.base}/${idConversacion}/mensajes`, { params: { pagina, tamanio: 100 } });
    const items = (p: PaginaCruda) => (p?.contenido ?? p?.content ?? []).map((m) => this.aMensaje(m));
    return pedir(0).pipe(
      switchMap((primera) => {
        const paginas = Math.min(primera?.totalPaginas ?? primera?.totalPages ?? 1, maxPaginas);
        if (paginas <= 1) {
          return of(items(primera));
        }
        const resto = Array.from({ length: paginas - 1 }, (_, i) => pedir(i + 1));
        return forkJoin(resto).pipe(map((otras) => [...items(primera), ...otras.flatMap(items)]));
      }),
      // Del más antiguo al más reciente, sin importar en qué orden pagine el backend.
      map((lista) => [...lista].sort((a, b) => a.fecha.localeCompare(b.fecha))),
    );
  }
  /** Envía un mensaje (1 a 2000 caracteres). 403 si no participas; 404 si no existe. */
  enviar(idConversacion: number, contenido: string): Observable<Mensaje> {
    return this.http
      .post<Crudo>(`${this.base}/${idConversacion}/mensajes`, { contenido })
      .pipe(map((m) => this.aMensaje(m)));
  }
  /**
   * Inicia o retoma la conversación con el anfitrión de un alojamiento (200 si ya existía, 201 si es nueva).
   * El mensaje inicial se envía aparte con enviar(), con el campo que sí conozco (contenido).
   * 403 si el alojamiento es tuyo; 404 si no existe.
   */
  iniciar(idAlojamiento: number, mensaje?: string): Observable<Conversacion> {
    return this.http.post<Crudo>(this.base, { idAlojamiento }).pipe(
      map((c) => this.aConversacion(c)),
      switchMap((conversacion) =>
        mensaje?.trim() ? this.enviar(conversacion.id, mensaje.trim()).pipe(map(() => conversacion)) : of(conversacion),
      ),
    );
  }
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
  // ================= Normalización de DTO =================
  private aConversacion(c: Crudo): Conversacion {
    // El contacto es el otro participante: si soy huésped es el anfitrión, y al revés.
    const soyHuesped = this.session.rol() === 'Huésped';
    const contacto =
      valorDe(c, 'nombreContacto', 'interlocutor', 'nombreOtroParticipante') ||
      (soyHuesped
        ? valorDe(c, 'nombreAnfitrion', 'anfitrion')
        : valorDe(c, 'nombreHuesped', 'huesped'));
    // Si soy huésped, el contacto ya es el anfitrión; si soy anfitrión, el nombre viene en su propio campo.
    const anfitrion = valorDe(c, 'nombreAnfitrion', 'anfitrion') || (soyHuesped ? contacto : '');
    const idReserva = valorDe(c, 'idReserva');
    return {
      id: Number(valorDe(c, 'idConversacion', 'id')),
      contacto: contacto || 'Conversación',
      anfitrion,
      iniciales: this.iniciales(anfitrion || contacto),
      alojamiento: valorDe(c, 'tituloAlojamiento', 'alojamiento', 'titulo'),
      idAlojamiento: Number(valorDe(c, 'idAlojamiento', 'alojamientoId')) || null,
      reserva: idReserva ? `Reserva #${idReserva}` : '',
      noLeidos: Number(valorDe(c, 'noLeidos', 'mensajesNoLeidos', 'sinLeer')) || 0,
      ultimo: valorDe(c, 'ultimoMensaje', 'ultimoContenido', 'contenidoUltimoMensaje'),
    };
  }
  private aMensaje(m: Crudo): Mensaje {
    return {
      id: Number(valorDe(m, 'idMensaje', 'id')),
      texto: valorDe(m, 'contenido', 'texto', 'mensaje'),
      fecha: valorDe(m, 'fechaEnvio', 'fecha', 'fechaCreacion'),
      mio: this.esMio(m),
    };
  }
  /** ¿Lo escribí yo? Prueba, en orden: un booleano del DTO, el id, el correo y el rol del remitente. */
  private esMio(m: Crudo): boolean {
    const directo = valorDe(m, 'esMio', 'propio', 'mio', 'enviadoPorMi');
    if (directo) {
      return directo === 'true';
    }
    const claims = this.claims();
    const miId = ['idUsuario', 'id', 'userId', 'uid'].map((k) => claims[k]).find((v) => v != null);
    const idRemitente = valorDe(m, 'idRemitente', 'idEmisor', 'idAutor', 'remitenteId');
    if (miId != null && idRemitente) {
      return String(miId) === idRemitente;
    }
    const correo = valorDe(m, 'correoRemitente', 'emailRemitente', 'correoEmisor');
    if (correo) {
      const miCorreo = this.session.correo() || String(claims['sub'] ?? '');
      return correo.toLowerCase() === miCorreo.toLowerCase();
    }
    const rolRemitente = this.sinTildes(valorDe(m, 'rolRemitente', 'tipoRemitente', 'rolEmisor'));
    const miRol = this.session.rol();
    if (rolRemitente && miRol) {
      return rolRemitente.includes(this.sinTildes(miRol));
    }
    return false;
  }
  /** Datos del JWT (el payload no está cifrado). */
  private claims(): Record<string, unknown> {
    const token = this.session.obtenerToken();
    if (!token) {
      return {};
    }
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const binario = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
      const bytes = Uint8Array.from(binario, (c) => c.charCodeAt(0));
      return JSON.parse(new TextDecoder().decode(bytes));
    } catch {
      return {};
    }
  }


  private sinTildes(texto: string): string {
    return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase();
  }


  private iniciales(nombre: string): string {
    return (
      nombre
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((p) => p[0])
        .join('')
        .toUpperCase() || '?'
    );
  }
}
