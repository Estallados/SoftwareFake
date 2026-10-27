import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

export type HostReservationStatus = 'Confirmada' | 'Próxima' | 'Pendiente' | 'Cancelada' | 'Finalizada';

export interface HostReservation {
  code: string;
  guest: string;
  accommodation: string;
  checkin: string;
  checkout: string;
  total: string;
  status: HostReservationStatus;
}

@Component({
  selector: 'app-host-reservations',
  standalone: true,
  templateUrl: './reservations.html',
})
export class Reservations {
  private router = inject(Router);

  // TODO: reemplazar por las reservas del backend
  reservations = signal<HostReservation[]>([]);

  readonly headers = ['Código', 'Huésped', 'Alojamiento', 'Entrada', 'Salida', 'Total', 'Estado'];

  open(code: string) {
    this.router.navigate(['/host/reservas', code]);
  }

  badgeClass(status: HostReservationStatus) {
    const tone =
      status === 'Cancelada'
        ? 'bg-red-50 text-red-700'
        : status === 'Próxima'
          ? 'bg-sky-50 text-sky-700'
          : status === 'Pendiente'
            ? 'bg-amber-50 text-amber-700'
            : status === 'Finalizada'
              ? 'bg-slate-100 text-slate-600'
              : 'bg-emerald-50 text-emerald-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }
}
