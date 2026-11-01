import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { EstadoReserva } from '../../../models/estadoReservaDto';
import { Reserva } from '../../../models/reservaDto';
import { AdminService } from '../../../services/admin.service';
@Component({
  selector: 'app-admin-reservations',
  standalone: true,
  templateUrl: './reservations.html',
  imports: [CurrencyPipe, DatePipe],
})
export class Reservations implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly TAMANIO = 20;
  readonly headers = ['Código', 'Huésped', 'Alojamiento', 'Anfitrión', 'Fecha', 'Total', 'Estado'];
  /** Filtros del servidor (Finalizada no es un estado: es una confirmada ya cumplida). */
  readonly filtros = [
    { valor: '', texto: 'Todos los estados' },
    { valor: 'CONFIRMADA', texto: 'Confirmadas' },
    { valor: 'EN_REVISION', texto: 'En revisión' },
    { valor: 'CANCELADA', texto: 'Canceladas' },
  ];
  reservations = signal<Reserva[]>([]);
  filtroEstado = signal('');
  pagina = signal(0);
  totalPaginas = signal(1);
  totalElementos = signal(0);
  loading = signal(true);
  errorCarga = signal(false);
  /** Hasta 5 números de página alrededor de la actual. */
  readonly paginasVisibles = computed(() => {
    const total = this.totalPaginas();
    const inicio = Math.max(0, Math.min(this.pagina() - 2, total - 5));
    const fin = Math.min(total, inicio + 5);
    return Array.from({ length: fin - inicio }, (_, i) => inicio + i);
  });
  ngOnInit(): void {
    this.cargar();
  }
  cargar(): void {
    this.loading.set(true);
    this.errorCarga.set(false);
    const estado = this.filtroEstado();
    this.admin.reservasPagina(this.pagina(), this.TAMANIO, estado ? (estado as EstadoReserva) : undefined).subscribe({
      next: (p) => {
        this.reservations.set(p.contenido);
        this.totalPaginas.set(Math.max(1, p.totalPaginas));
        this.totalElementos.set(p.totalElementos);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error al cargar las reservas:', error);
        this.reservations.set([]);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }
  cambiarEstado(valor: string): void {
    this.filtroEstado.set(valor);
    this.pagina.set(0);
    this.cargar();
  }
  irAPagina(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPaginas() || pagina === this.pagina()) {
      return;
    }
    this.pagina.set(pagina);
    this.cargar();
  }
  etiqueta(r: Reserva): string {
    const estado = r.estado as string;
    if (estado === 'CONFIRMADA') return r.completada ? 'Finalizada' : 'Confirmada';
    if (estado === 'CANCELADA') return 'Cancelada';
    if (estado === 'EN_REVISION') return 'En revisión';
    return estado.charAt(0) + estado.slice(1).toLowerCase().replace(/_/g, ' ');
  }
  badgeClass(r: Reserva): string {
    const estado = r.estado as string;
    const tone =
      estado === 'CANCELADA'
        ? 'bg-red-50 text-red-700'
        : estado === 'CONFIRMADA'
          ? r.completada
            ? 'bg-slate-100 text-slate-600'
            : 'bg-emerald-50 text-emerald-700'
          : 'bg-amber-50 text-amber-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }
}
