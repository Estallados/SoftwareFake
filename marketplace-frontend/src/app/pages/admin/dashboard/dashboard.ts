import { Component, signal } from '@angular/core';

export interface DashboardMetric {
  label: string;
  value: string;
  icon: 'users' | 'building' | 'chart' | 'shield';
  delta: string;
}

export interface ReservationStat {
  label: 'Confirmadas' | 'Pendientes' | 'Finalizadas' | 'Canceladas';
  value: number;
  percent: number;
}

export interface CityDemand {
  city: string;
  total: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  templateUrl: './dashboard.html',
})
export class Dashboard {
  // TODO: reemplazar por los datos del backend
  metrics = signal<DashboardMetric[]>([]);
  totalReservations = signal(0);
  reservationStats = signal<ReservationStat[]>([]);
  topCities = signal<CityDemand[]>([]);

  /** Colores de barra por estado (clases completas para que Tailwind las detecte) */
  readonly barColor: Record<ReservationStat['label'], string> = {
    Confirmadas: 'bg-emerald-600',
    Pendientes: 'bg-amber-400',
    Finalizadas: 'bg-sky-500',
    Canceladas: 'bg-red-400',
  };
}
