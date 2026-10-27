import { Component, computed, inject, signal } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

export type HostReservationStatus = 'Confirmada' | 'Cancelada' | 'Finalizada' | 'Pendiente';

export interface HostReservationInfo {
  code: string;
  start: string;
  end: string;
  guests: number;
  status: HostReservationStatus;
  total: string;
}

export interface ReservedListing {
  name: string;
  image: string;
  city: string;
  location: string;
}

@Component({
  selector: 'app-host-reservation-detail',
  standalone: true,
  templateUrl: './reservation-detail.html',
})
export class ReservationDetail {
  private location = inject(Location);
  private route = inject(ActivatedRoute);

  /** Parámetro :id de la ruta host/reservas/:id (código de la reserva) */
  readonly id = this.route.snapshot.paramMap.get('id');

  // TODO: cargar la reserva y su alojamiento desde el backend usando `id`
  reservation = signal<HostReservationInfo>({
    code: this.id ?? '',
    start: '',
    end: '',
    guests: 0,
    status: 'Confirmada',
    total: '',
  });
  listing = signal<ReservedListing>({ name: '', image: '', city: '', location: '' });

  summary = computed(() => {
    const r = this.reservation();
    return [
      { label: 'Código de reserva', value: r.code },
      { label: 'Alojamiento', value: this.listing().name },
      { label: 'Fecha de entrada', value: r.start },
      { label: 'Fecha de salida', value: r.end },
      { label: 'Número de huéspedes', value: String(r.guests) },
      { label: 'Estado', value: r.status },
    ];
  });

  badgeClass = computed(() => {
    const s = this.reservation().status;
    const tone =
      s === 'Cancelada'
        ? 'bg-red-50 text-red-700'
        : s === 'Finalizada'
          ? 'bg-slate-100 text-slate-600'
          : s === 'Pendiente'
            ? 'bg-amber-50 text-amber-700'
            : 'bg-emerald-50 text-emerald-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  });

  back() {
    this.location.back();
  }
}
