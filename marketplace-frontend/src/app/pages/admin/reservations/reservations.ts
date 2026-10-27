import { Component, signal } from '@angular/core';

export type AdminReservationStatus = 'Confirmada' | 'Pendiente' | 'Finalizada' | 'Cancelada';

export interface AdminReservation {
  code: string;
  guest: string;
  accommodation: string;
  host: string;
  date: string;
  total: string;
  status: AdminReservationStatus;
}

@Component({
  selector: 'app-admin-reservations',
  standalone: true,
  templateUrl: './reservations.html',
})
export class Reservations {
  // TODO: reemplazar por las reservas del backend
  reservations = signal<AdminReservation[]>([]);

  readonly headers = ['Código', 'Huésped', 'Alojamiento', 'Anfitrión', 'Fecha', 'Total', 'Estado'];

  badgeClass(status: AdminReservationStatus) {
    const tone =
      status === 'Cancelada'
        ? 'bg-red-50 text-red-700'
        : status === 'Pendiente'
          ? 'bg-amber-50 text-amber-700'
          : status === 'Finalizada'
            ? 'bg-slate-100 text-slate-600'
            : 'bg-emerald-50 text-emerald-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }
}
