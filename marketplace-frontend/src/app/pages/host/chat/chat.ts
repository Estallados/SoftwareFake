
import { DatePipe } from '@angular/common';

import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal
} from '@angular/core';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import { interval } from 'rxjs';

import {
  Conversacion,
  Mensaje,
  MensajeriaService
} from '../../../services/mensajeria.service';

// ======================================
// COMPONENTE CHAT DEL ANFITRIÓN
// ======================================

@Component({
  selector: 'app-host-chat',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './chat.html'
})
export class Chat implements OnInit {

  // ======================================
  // INYECCIÓN DE SERVICIOS
  // ======================================

  private readonly mensajeria = inject(MensajeriaService);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('scroller')
  scroller?: ElementRef<HTMLDivElement>;

  // ======================================
  // CONFIGURACIÓN
  // ======================================

  readonly limite = 2000;

  // ======================================
  // ESTADOS DEL CHAT
  // ======================================

  readonly conversaciones = signal<Conversacion[]>([]);

  readonly conversacionId = signal<number | null>(null);

  readonly mensajes = signal<Mensaje[]>([]);

  readonly busqueda = signal('');

  readonly borrador = signal('');

  readonly cargando = signal(true);

  readonly cargandoMensajes = signal(false);

  readonly enviando = signal(false);

  readonly error = signal('');

  readonly errorMensajes = signal('');

  readonly errorEnvio = signal('');

  // Control de solicitudes para evitar
  // que respuestas antiguas reemplacen datos nuevos

  private revisionMensajes = 0;

  private revisionConversaciones = 0;

  // ======================================
  // FILTRAR CONVERSACIONES
  // ======================================

  readonly filtradas = computed(() => {

    const consulta = this.busqueda()
      .trim()
      .toLocaleLowerCase();

    return this.conversaciones().filter(c =>

      `${c.contacto} ${c.alojamiento} ${c.reserva}`
        .toLocaleLowerCase()
        .includes(consulta)

    );
  });

  // ======================================
  // CONVERSACIÓN ACTIVA
  // ======================================

  readonly activa = computed(() => {

    return this.conversaciones().find(
      c => c.id === this.conversacionId()
    ) ?? null;

  });

  // ======================================
  // INICIALIZAR CHAT
  // ======================================

  ngOnInit(): void {

    this.cargarConversaciones();

    // Actualizar automáticamente cada 10 segundos

    interval(10000)
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {

        if (
          typeof document === 'undefined' ||
          !document.hidden
        ) {

          this.actualizarSilenciosamente();

        }

      });
  }

  // ======================================
  // CONSULTAR CONVERSACIONES DEL BACKEND
  // GET /conversaciones
  // ======================================

  cargarConversaciones(): void {

    const revision = ++this.revisionConversaciones;

    this.cargando.set(true);

    this.error.set('');

    this.mensajeria.conversaciones()
      .subscribe({

        next: lista => {

          if (
            revision !== this.revisionConversaciones
          ) {
            return;
          }

          this.conversaciones.set(lista);

          this.cargando.set(false);

          // Buscar conversación solicitada por URL

          const solicitada = Number(
            this.route.snapshot.queryParamMap.get(
              'conversacion'
            )
          );

          const actual = this.conversacionId();

          // Conservar conversación seleccionada
          // o seleccionar la primera disponible

          const seleccion =

            lista.find(c => c.id === actual)

            ?? lista.find(c => c.id === solicitada)

            ?? lista[0];

          if (
            seleccion &&
            actual !== seleccion.id
          ) {

            this.seleccionar(seleccion.id);

          } else if (!seleccion) {

            this.conversacionId.set(null);

            this.mensajes.set([]);

          }

        },

        error: err => {

          if (
            revision !== this.revisionConversaciones
          ) {
            return;
          }

          this.cargando.set(false);

          this.error.set(
            this.mensajeria.mensajeDeError(
              err,
              'No se pudieron cargar los chats.'
            )
          );

        }

      });
  }

  // ======================================
  // SELECCIONAR CONVERSACIÓN
  // ======================================

  seleccionar(id: number): void {

    this.conversacionId.set(id);

    this.errorMensajes.set('');

    this.errorEnvio.set('');

    this.cargarMensajes(id, false);
  }

  // ======================================
  // CARGAR MENSAJES
  // GET /conversaciones/{id}/mensajes
  // ======================================

  private cargarMensajes(
    id: number,
    silencioso: boolean
  ): void {

    const revision = ++this.revisionMensajes;

    if (!silencioso) {

      this.cargandoMensajes.set(true);

      this.errorMensajes.set('');

      this.mensajes.set([]);

    }

    this.mensajeria.mensajes(id)
      .subscribe({

        next: lista => {

          if (
            id !== this.conversacionId() ||
            revision !== this.revisionMensajes
          ) {
            return;
          }

          // Comprobar si llegaron nuevos mensajes

          const nuevos =

            lista.length !== this.mensajes().length

            || lista.at(-1)?.id !==
            this.mensajes().at(-1)?.id;

          this.mensajes.set(lista);

          this.cargandoMensajes.set(false);

          if (nuevos) {
            this.irAlFinal();
          }

        },

        error: err => {

          if (
            id !== this.conversacionId() ||
            revision !== this.revisionMensajes
          ) {
            return;
          }

          this.cargandoMensajes.set(false);

          if (!silencioso) {

            this.errorMensajes.set(
              this.mensajeria.mensajeDeError(
                err,
                'No se pudieron cargar los mensajes.'
              )
            );

          }

        }

      });
  }

  // ======================================
  // ENVIAR MENSAJE AL HUÉSPED
  // POST /conversaciones/{id}/mensajes
  // ======================================

  enviar(): void {

    const texto = this.borrador().trim();

    const id = this.conversacionId();

    // Validaciones antes del envío

    if (
      id === null ||
      this.enviando() ||
      !texto
    ) {
      return;
    }

    if (texto.length > this.limite) {

      this.errorEnvio.set(
        `Máximo ${this.limite} caracteres por mensaje.`
      );

      return;
    }

    this.enviando.set(true);

    this.errorEnvio.set('');

    this.mensajeria.enviar(id, texto)
      .subscribe({

        next: mensaje => {

          this.enviando.set(false);

          // Evitar actualizar una conversación
          // diferente si el usuario cambió de chat

          if (
            this.conversacionId() === id
          ) {

            ++this.revisionMensajes;

            this.mensajes.update(lista => [
              ...lista,
              mensaje
            ]);

            this.borrador.set('');

            this.irAlFinal();

          }

          // Actualizar último mensaje mostrado

          this.conversaciones.update(lista =>

            lista.map(c =>

              c.id === id

                ? {
                  ...c,
                  ultimo: texto
                }

                : c

            )

          );

        },

        error: err => {

          this.enviando.set(false);

          this.errorEnvio.set(
            this.mensajeria.mensajeDeError(
              err,
              'No se pudo enviar el mensaje.'
            )
          );

        }

      });
  }

  // ======================================
  // ACTUALIZACIÓN AUTOMÁTICA
  // ======================================

  private actualizarSilenciosamente(): void {

    // Refrescar lista de conversaciones

    this.mensajeria.conversaciones()
      .subscribe({

        next: lista => {

          if (!this.cargando()) {

            this.conversaciones.set(lista);

          }

        },

        error: () => undefined

      });

    // Refrescar mensajes del chat activo

    const id = this.conversacionId();

    if (
      id !== null &&
      !this.enviando()
    ) {

      this.cargarMensajes(id, true);

    }
  }

  // ======================================
  // NAVEGAR AL ALOJAMIENTO
  // ======================================

  irAlAlojamiento(): void {

    const id = this.activa()?.idAlojamiento;

    if (id != null) {

      void this.router.navigate([
        '/host/alojamientos',
        id
      ]);

    }
  }

  // ======================================
  // INICIALES DEL HUÉSPED
  // ======================================

  iniciales(contacto: string): string {

    return contacto
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(
        p => p.charAt(0).toUpperCase()
      )
      .join('') || '?';
  }

  // ======================================
  // ESTILO DE CONVERSACIÓN SELECCIONADA
  // ======================================

  claseConversacion(id: number): string {

    return (
      'flex w-full items-start gap-3 ' +
      'border-b border-slate-100 p-4 ' +
      'text-left transition ' +

      (
        id === this.conversacionId()

          ? 'bg-emerald-50'

          : 'hover:bg-slate-50'
      )
    );
  }

  // ======================================
  // ESTILO DE MENSAJES ENVIADOS Y RECIBIDOS
  // ======================================

  claseMensaje(m: Mensaje): string {

    return (
      'max-w-[85%] rounded-2xl ' +
      'px-4 py-3 text-sm leading-6 ' +
      'shadow-sm sm:max-w-[75%] ' +

      (
        m.mio

          ? 'bg-emerald-700 text-white'

          : 'border border-slate-200 ' +
          'bg-white text-slate-800'
      )
    );
  }

  // ======================================
  // DESPLAZAR CHAT AL ÚLTIMO MENSAJE
  // ======================================

  private irAlFinal(): void {

    setTimeout(() => {

      const nodo =
        this.scroller?.nativeElement;

      if (nodo) {

        nodo.scrollTop = nodo.scrollHeight;

      }

    });
  }
}
