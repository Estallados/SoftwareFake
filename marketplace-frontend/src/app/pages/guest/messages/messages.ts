import { DatePipe } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { interval } from 'rxjs';
import { Conversacion, MensajeriaService, Mensaje } from '../../../services/mensajeria.service';
interface GrupoDia {
  etiqueta: string;
  mensajes: Mensaje[];
}
@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './messages.html',
})
export class Messages implements OnInit {
  private readonly service = inject(MensajeriaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  @ViewChild('scroller') private scroller?: ElementRef<HTMLElement>;
  /** Cada cuánto se buscan mensajes nuevos (no hay tiempo real en el backend). */
  private readonly REFRESCO_MS = 10_000;
  readonly MAX_CARACTERES = 2000;
  /** Sube cada vez que el usuario envía un mensaje: descarta refrescos que ya quedaron viejos. */
  private versionMensajes = 0;
  conversations = signal<Conversacion[]>([]);
  selectedId = signal<number | null>(null);
  messages = signal<Mensaje[]>([]);
  draft = signal('');
  search = signal('');
  loading = signal(true);
  errorCarga = signal(false);
  loadingMessages = signal(false);
  errorMensajes = signal(false);
  sending = signal(false);
  errorEnvio = signal<string | null>(null);
  filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    return this.conversations().filter((c) => `${c.anfitrion} ${c.contacto} ${c.alojamiento}`.toLowerCase().includes(term));
  });
  active = computed(() => this.conversations().find((c) => c.id === this.selectedId()) ?? null);
  /** Mensajes agrupados por día (Hoy, Ayer o la fecha). */
  grupos = computed(() => {
    const grupos: GrupoDia[] = [];
    for (const m of this.messages()) {
      const etiqueta = this.etiquetaDia(m.fecha);
      const ultimo = grupos.at(-1);
      if (ultimo?.etiqueta === etiqueta) {
        ultimo.mensajes.push(m);
      } else {
        grupos.push({ etiqueta, mensajes: [m] });
      }
    }
    return grupos;
  });
  ngOnInit(): void {
    this.cargarConversaciones();
    interval(this.REFRESCO_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refrescar());
  }
  cargarConversaciones(): void {
    this.loading.set(true);
    this.errorCarga.set(false);
    this.service.conversaciones().subscribe({
      next: (lista) => {
        this.conversations.set(lista);
        this.loading.set(false);
        // ?conversacion=ID abre esa conversación (viene de "Contactar al anfitrión")
        const pedida = Number(this.route.snapshot.queryParamMap.get('conversacion'));
        const destino = (pedida > 0 ? lista.find((c) => c.id === pedida) : undefined) ?? lista[0];
        if (destino) {
          this.select(destino.id);
        }
      },
      error: (error) => {
        console.error('Error al cargar las conversaciones:', error);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }
  select(id: number): void {
    this.selectedId.set(id);
    this.errorEnvio.set(null);
    this.conversations.update((lista) => lista.map((c) => (c.id === id ? { ...c, noLeidos: 0 } : c)));
    this.cargarMensajes(id, false);
  }
  private cargarMensajes(id: number, silencioso: boolean): void {
    if (!silencioso) {
      this.loadingMessages.set(true);
      this.errorMensajes.set(false);
      this.messages.set([]);
    }
    const version = this.versionMensajes;
    this.service.mensajes(id).subscribe({
      next: (lista) => {
        if (this.selectedId() !== id) {
          return; // el usuario ya cambió de conversación
        }
        if (silencioso && version !== this.versionMensajes) {
          return; // se envió un mensaje mientras llegaba esta respuesta: traería la lista sin él
        }
        const hayNuevos = lista.length !== this.messages().length;
        this.messages.set(lista);
        this.loadingMessages.set(false);
        if (hayNuevos) {
          this.irAlFinal();
        }
      },
      error: (error) => {
        if (!silencioso) {
          console.error('Error al cargar los mensajes:', error);
          this.errorMensajes.set(true);
          this.loadingMessages.set(false);
        }
      },
    });
  }
  /** Refresco periódico: solo si la pestaña está a la vista. */
  private refrescar(): void {
    if (document.hidden) {
      return;
    }
    this.service.conversaciones().subscribe({
      next: (lista) => {
        this.conversations.set(lista.map((c) => (c.id === this.selectedId() ? { ...c, noLeidos: 0 } : c)));
        this.errorCarga.set(false);
        this.loading.set(false);
      },
      error: () => undefined, // un fallo puntual no interrumpe: se reintenta en el siguiente ciclo
    });
    const id = this.selectedId();
    if (id !== null) {
      this.cargarMensajes(id, true);
    }
  }
  send(): void {
    const texto = this.draft().trim();
    const id = this.selectedId();
    if (!texto || id === null || this.sending()) {
      return;
    }
    if (texto.length > this.MAX_CARACTERES) {
      this.errorEnvio.set(`El mensaje no puede superar ${this.MAX_CARACTERES} caracteres.`);
      return;
    }
    this.sending.set(true);
    this.errorEnvio.set(null);
    this.service.enviar(id, texto).subscribe({
      next: (mensaje) => {
        this.versionMensajes++;
        this.conversations.update((lista) => lista.map((c) => (c.id === id ? { ...c, ultimo: texto } : c)));
        this.draft.set('');
        this.sending.set(false);
        // Si el usuario cambió de conversación mientras se enviaba, no se mezcla en la lista actual.
        if (this.selectedId() === id) {
          this.messages.update((lista) => [...lista, mensaje]);
          this.irAlFinal();
        }
      },
      error: (error) => {
        this.errorEnvio.set(this.service.mensajeDeError(error, 'No se pudo enviar el mensaje.'));
        this.sending.set(false); // el borrador se conserva para reintentar
      },
    });
  }
  /** Abre el detalle del alojamiento relacionado con la conversación. */
  irAlAlojamiento(c: Conversacion): void {
    if (c.idAlojamiento == null) {
      console.error('La conversación no tiene un alojamiento asociado.');
      return;
    }
    this.router.navigate(['/alojamiento', c.idAlojamiento]);
  }

  private irAlFinal(): void {
    setTimeout(() => {
      const el = this.scroller?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }
  private etiquetaDia(iso: string): string {
    const fecha = new Date(iso);
    if (Number.isNaN(fecha.getTime())) {
      return '';
    }
    const dia = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
    const diferencia = Math.round((dia(new Date()) - dia(fecha)) / 86400000);
    if (diferencia === 0) return 'Hoy';
    if (diferencia === 1) return 'Ayer';
    return fecha.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }
  itemClass(c: Conversacion): string {
    return (
      'flex w-full gap-3 border-b border-slate-100 p-5 text-left ' +
      (this.selectedId() === c.id ? 'bg-emerald-50' : 'hover:bg-slate-50')
    );
  }
  bubbleClass(m: Mensaje): string {
    return (
      'max-w-[70%] rounded-2xl px-5 py-3 text-sm leading-6 break-words ' +
      (m.mio ? 'bg-emerald-700 text-white' : 'border border-slate-200 bg-white')
    );
  }
  timeClass(m: Mensaje): string {
    return 'mt-1 text-right text-[10px] ' + (m.mio ? 'text-emerald-100' : 'text-slate-400');
  }
}
