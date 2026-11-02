import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Reserva } from '../../../models/reservaDto';
import { ReservaService } from '../../../services/reserva.service';

type PestanaReserva = 'Próximas' | 'Activas' | 'Finalizadas' | 'Canceladas';

const PESTANAS: PestanaReserva[] = ['Próximas', 'Activas', 'Finalizadas', 'Canceladas'];

@Component({
  selector: 'app-guest-reservations',
  standalone: true,
  templateUrl: './reservations.html',
  imports: [CurrencyPipe, DatePipe],
})
export class Reservations implements OnInit {
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly reservaService = inject(ReservaService);

  // ==================== DATOS ====================
  reservas = signal<Reserva[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  pestanaActiva = signal<PestanaReserva>('Próximas');

  // ==================== CLASIFICACIÓN ====================
  /** Fecha de hoy (local) como YYYY-MM-DD, comparable con fechaEntrada/fechaSalida. */
  private hoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private yaInicio(r: Reserva): boolean {
    return r.fechaEntrada <= this.hoy();
  }

  private categoria(r: Reserva): PestanaReserva {
    if (r.estado === 'CANCELADA') return 'Canceladas';
    if (r.completada) return 'Finalizadas';
    if (r.estado === 'CONFIRMADA' && this.yaInicio(r)) return 'Activas';
    return 'Próximas'; // CONFIRMADA futura, PENDIENTE_PAGO o EN_REVISION
  }

  readonly grupos = computed<Record<PestanaReserva, Reserva[]>>(() => {
    const todas = this.reservas();
    return {
      Próximas: todas.filter((r) => this.categoria(r) === 'Próximas'),
      Activas: todas.filter((r) => this.categoria(r) === 'Activas'),
      Finalizadas: todas.filter((r) => this.categoria(r) === 'Finalizadas'),
      Canceladas: todas.filter((r) => this.categoria(r) === 'Canceladas'),
    };
  });

  readonly pestanas = computed(() =>
    PESTANAS.map((value) => ({ value, label: `${value} ${this.grupos()[value].length}` })),
  );

  readonly actuales = computed(() => this.grupos()[this.pestanaActiva()]);

  // ==================== INICIALIZACIÓN ====================
  ngOnInit(): void {
    this.cargarReservas();
  }

  reintentar(): void {
    this.cargarReservas();
  }

  private cargarReservas(): void {
    this.loading.set(true);
    this.errorCarga.set(false);

    this.reservaService.misReservas().subscribe({
      next: (respuesta: Reserva[]) => {
        console.log('Reservas recibidas:', respuesta);
        this.reservas.set(respuesta ?? []);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar las reservas:', error);
        this.reservas.set([]);
        this.errorCarga.set(true);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
    });
  }

  // ==================== PRESENTACIÓN ====================
  claseTab(tab: PestanaReserva): string {
    return (
      'px-5 pb-4 text-sm font-bold ' +
      (this.pestanaActiva() === tab ? 'border-b-2 border-emerald-700 text-emerald-700' : 'text-slate-500')
    );
  }

  etiqueta(r: Reserva): string {
    if (r.estado === 'CANCELADA') return 'Cancelada';
    if (r.completada) return 'Finalizada';
    if (r.estado === 'EN_REVISION') return 'En revisión';
    if (r.estado === 'PENDIENTE_PAGO') return 'Pendiente de pago';
    return this.yaInicio(r) ? 'Activa' : 'Confirmada';
  }

  claseEtiqueta(r: Reserva): string {
    const etiqueta = this.etiqueta(r);
    const tono =
      etiqueta === 'Cancelada'
        ? 'bg-red-50 text-red-700'
        : etiqueta === 'Finalizada'
          ? 'bg-slate-100 text-slate-600'
          : etiqueta === 'Activa'
            ? 'bg-sky-50 text-sky-700'
            : etiqueta === 'Confirmada'
              ? 'bg-emerald-50 text-emerald-700'
              : 'bg-amber-50 text-amber-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tono;
  }

  /** Solo se puede cancelar una reserva confirmada que aún no empieza. */
  puedeCancelar(r: Reserva): boolean {
    return r.estado === 'CONFIRMADA' && !r.completada && !this.yaInicio(r);
  }

  // ==================== NAVEGACIÓN ====================
  /** La cancelación y la reseña se hacen en el detalle; `accion` abre el modal correspondiente. */
  abrir(id: number | undefined, accion?: 'cancelar' | 'resenar'): void {
    if (id == null) {
      console.error('La reserva no tiene un ID válido.');
      return;
    }
    this.router.navigate(['/reservas', id], accion ? { queryParams: { accion } } : {});
  }

  verResenas(): void {
    this.router.navigateByUrl('/resenas');
  }
}
