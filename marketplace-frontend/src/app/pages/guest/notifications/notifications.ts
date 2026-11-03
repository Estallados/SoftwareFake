import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import {Notificacion} from '../../../models/notificacioensDto';
import {NotificacionService} from '../../../services/notificaciones.service';
@Component({
  selector: 'notifications',
  standalone: true,
  templateUrl: './notifications.html',
})
export class Notifications implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly notificacionService = inject(NotificacionService);
  // ==================== DATOS ====================
  items = signal<Notificacion[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  readonly sinLeer = computed(() => this.items().filter((n) => !n.leida).length);
  // ==================== ACCIONES ====================
  marcando = signal(false);
  aviso = signal('');
  errorAccion = signal('');
  private temporizadorAviso?: ReturnType<typeof setTimeout>;
  // ==================== INICIALIZACIÓN ====================
  ngOnInit(): void {
    this.cargarNotificaciones();
  }
  reintentar(): void {
    this.cargarNotificaciones();
  }
  private cargarNotificaciones(): void {
    this.loading.set(true);
    this.errorCarga.set(false);
    this.notificacionService.listar().subscribe({
      next: (respuesta: Notificacion[]) => {
        console.log('Notificaciones recibidas:', respuesta);
        this.items.set(respuesta ?? []);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar las notificaciones:', error);
        this.items.set([]);
        this.errorCarga.set(true);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
    });
  }
  // ==================== MARCAR COMO LEÍDAS ====================
  /** Al hacer clic en una notificación sin leer, se marca como leída. */
  abrir(notificacion: Notificacion): void {
    if (notificacion.leida || notificacion.idNotificacion == null) {
      return;
    }
    this.errorAccion.set('');
    this.notificacionService.marcarLeida(notificacion.idNotificacion).subscribe({
      next: () => {
        this.items.update((lista) =>
          lista.map((n) =>
            n.idNotificacion === notificacion.idNotificacion ? { ...n, leida: true } : n,
          ),
        );
        this.avisarCambio();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al marcar la notificación como leída:', error);
        this.errorAccion.set(error?.error?.mensaje ?? 'No se pudo marcar la notificación como leída.');
        this.cdr.detectChanges();
      },
    });
  }
  marcarTodasLeidas(): void {
    if (this.marcando() || this.sinLeer() === 0) {
      return;
    }
    this.marcando.set(true);
    this.errorAccion.set('');
    this.notificacionService.marcarTodasLeidas().subscribe({
      next: () => {
        this.items.update((lista) => lista.map((n) => ({ ...n, leida: true })));
        this.marcando.set(false);
        this.mostrarAviso('Todas las notificaciones fueron marcadas como leídas.');
        this.avisarCambio();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al marcar todas como leídas:', error);
        this.errorAccion.set(error?.error?.mensaje ?? 'No se pudieron marcar las notificaciones como leídas.');
        this.marcando.set(false);
        this.cdr.detectChanges();
      },
    });
  }
  /** Le avisa a la barra de navegación para que actualice el contador de sin leer. */
  private avisarCambio(): void {
    window.dispatchEvent(new Event('notificaciones-actualizadas'));
  }
  private mostrarAviso(texto: string): void {
    clearTimeout(this.temporizadorAviso);
    this.aviso.set(texto);
    this.temporizadorAviso = setTimeout(() => {
      this.aviso.set('');
      this.cdr.detectChanges();
    }, 4000);
  }
  // ==================== PRESENTACIÓN ====================
  claseItem(notificacion: Notificacion): string {
    return (
      'flex w-full gap-4 border-b border-slate-100 p-6 text-left last:border-0 hover:bg-slate-50 ' +
      (notificacion.leida ? '' : 'bg-emerald-50/40')
    );
  }
  /** Tiempo transcurrido en texto corto: Ahora, Hace 5 min, Hace 3 h, Hace 2 d o la fecha. */
  tiempo(fecha: string | undefined): string {
    if (!fecha) {
      return '';
    }
    const instante = new Date(fecha);
    if (Number.isNaN(instante.getTime())) {
      return '';
    }
    const segundos = Math.round((Date.now() - instante.getTime()) / 1000);
    if (segundos < 60) return 'Ahora';
    const minutos = Math.floor(segundos / 60);
    if (minutos < 60) return `Hace ${minutos} min`;
    const horas = Math.floor(minutos / 60);
    if (horas < 24) return `Hace ${horas} h`;
    const dias = Math.floor(horas / 24);
    if (dias < 7) return `Hace ${dias} d`;
    return instante.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
