import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

export interface HostMetric {
  label: string;
  value: string;
  icon: 'building' | 'calendar' | 'chart' | 'star';
  delta: string;
}

export interface MonthPerformance {
  month: string;
  percent: number; // altura de la barra (0-100)
}

export interface UpcomingStay {
  date: string;
  accommodation: string;
  guest: string;
}

@Component({
  selector: 'app-host-dashboard',
  standalone: true,
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private router = inject(Router);

  // TODO: reemplazar por los datos del backend
  hostName = signal('');
  metrics = signal<HostMetric[]>([]);
  performance = signal<MonthPerformance[]>([]);
  upcomingStays = signal<UpcomingStay[]>([]);

  createListing() {
    this.router.navigateByUrl('/host/alojamientos/crear');
  }
}
