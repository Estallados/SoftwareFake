import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

export type ReservationStatus = 'Confirmada' | 'Activa' | 'Finalizada' | 'Cancelada';
export type ReservationTab = 'Próximas' | 'Activas' | 'Finalizadas' | 'Canceladas';

export interface Reservation {
  code: string;
  name: string;
  city: string;
  image: string;
  status: ReservationStatus;
  checkin: string;
  checkout: string;
  guests: number;
  total: string;
}

export interface ReviewRef {
  reservation: string; // código de la reserva reseñada
}

@Component({
  selector: 'app-guest-reservations',
  standalone: true,
  templateUrl: './reservations.html',
})
export class Reservations {
  private router = inject(Router);

  // TODO: reemplazar por los datos del backend
  reservations = signal<Reservation[]>([]);
  reviews = signal<ReviewRef[]>([]);
  activeTab = signal<ReservationTab>('Próximas');

  groups = computed<Record<ReservationTab, Reservation[]>>(() => {
    const all = this.reservations();
    return {
      Próximas: all.filter((r) => r.status === 'Confirmada'),
      Activas: all.filter((r) => r.status === 'Activa'),
      Finalizadas: all.filter((r) => r.status === 'Finalizada'),
      Canceladas: all.filter((r) => r.status === 'Cancelada'),
    };
  });

  tabs = computed(() =>
    (Object.keys(this.groups()) as ReservationTab[]).map((value) => ({
      value,
      label: `${value} ${this.groups()[value].length}`,
    })),
  );

  current = computed(() => this.groups()[this.activeTab()]);

  hasReview(code: string) {
    return this.reviews().some((r) => r.reservation === code);
  }

  tabClass(tab: ReservationTab) {
    return (
      'px-5 pb-4 text-sm font-bold ' +
      (this.activeTab() === tab
        ? 'border-b-2 border-emerald-700 text-emerald-700'
        : 'text-slate-500')
    );
  }

  badgeClass(status: ReservationStatus) {
    const tone =
      status === 'Cancelada'
        ? 'bg-red-50 text-red-700'
        : status === 'Finalizada'
          ? 'bg-slate-100 text-slate-600'
          : status === 'Activa'
            ? 'bg-sky-50 text-sky-700'
            : 'bg-emerald-50 text-emerald-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }

  open(code: string) {
    this.router.navigate(['/reservas', code]);
  }

  cancel(reservation: Reservation) {
    // TODO: abrir confirmación y cancelar en el backend
  }

  review(reservation: Reservation) {
    // TODO: abrir el formulario para calificar la estancia
  }

  viewReviews() {
    this.router.navigateByUrl('/resenas');
  }
}
