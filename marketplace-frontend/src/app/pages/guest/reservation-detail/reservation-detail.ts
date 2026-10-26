import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

export type ReservationStatus = 'Confirmada' | 'Activa' | 'Finalizada' | 'Cancelada';

export interface Reservation {
  code: string;
  name: string;
  city: string;
  image: string;
  status: ReservationStatus;
  created: string;
  checkin: string;
  checkout: string;
  nights: number;
  guests: number;
  type: string;
  capacity: number;
  location: string;
  host: string;
  price: string;
  base: string;
  extras: string;
  discount: string;
  total: string;
}

@Component({
  selector: 'app-guest-reservation-detail',
  standalone: true,
  templateUrl: './reservation-detail.html',
})
export class ReservationDetail {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /** Parámetro :id de la ruta reservas/:id (código de la reserva) */
  readonly id = this.route.snapshot.paramMap.get('id');

  // TODO: cargar la reserva desde el backend usando `id`
  reservation = signal<Reservation>({
    code: '',
    name: '',
    city: '',
    image: '',
    status: 'Confirmada',
    created: '',
    checkin: '',
    checkout: '',
    nights: 0,
    guests: 0,
    type: '',
    capacity: 0,
    location: '',
    host: '',
    price: '',
    base: '',
    extras: '',
    discount: '',
    total: '',
  });
  cancelled = signal(false);
  hasReview = signal(false);

  status = computed<ReservationStatus>(() =>
    this.cancelled() ? 'Cancelada' : this.reservation().status,
  );

  info = computed(() => {
    const r = this.reservation();
    return [
      { label: 'Código', value: r.code },
      { label: 'Estado actual', value: this.status() },
      { label: 'Fecha de creación', value: r.created },
      { label: 'Entrada', value: r.checkin },
      { label: 'Salida', value: r.checkout },
      { label: 'Número de noches', value: String(r.nights) },
      { label: 'Huéspedes', value: String(r.guests) },
    ];
  });

  badgeClass = computed(() => {
    const s = this.status();
    const tone =
      s === 'Cancelada'
        ? 'bg-red-50 text-red-700'
        : s === 'Finalizada'
          ? 'bg-slate-100 text-slate-600'
          : s === 'Activa'
            ? 'bg-sky-50 text-sky-700'
            : 'bg-emerald-50 text-emerald-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  });

  back() {
    this.router.navigateByUrl('/reservas');
  }

  message() {
    this.router.navigateByUrl('/mensajes');
  }

  cancel() {
    // TODO: confirmar y cancelar en el backend; luego this.cancelled.set(true)
  }

  review() {
    // TODO: abrir el formulario para calificar la estancia
  }

  viewReview() {
    this.router.navigateByUrl('/resenas');
  }
}
