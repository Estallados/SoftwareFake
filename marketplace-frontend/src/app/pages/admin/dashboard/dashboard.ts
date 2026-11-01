import { Component, OnInit, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { EstadoAlojamiento } from '../../../models/estadoAlojamientoDto';
import { AdminService } from '../../../services/admin.service';
export interface DashboardMetric {
  label: string;
  value: string;
  icon: 'users' | 'building' | 'chart' | 'shield';
  /** Dato secundario opcional que se muestra como etiqueta. */
  delta?: string;
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
export class Dashboard implements OnInit {
  private readonly admin = inject(AdminService);
  loading = signal(true);
  errorCarga = signal(false);
  metrics = signal<DashboardMetric[]>([]);
  totalReservations = signal(0);
  reservationStats = signal<ReservationStat[]>([]);
  topCities = signal<CityDemand[]>([]);
  /** Se llena cuando las estadísticas se calcularon con menos reservas que el total real. */
  muestraParcial = signal<{ usadas: number; total: number } | null>(null);
  /** Colores de barra por estado (clases completas para que Tailwind las detecte) */
  readonly barColor: Record<ReservationStat['label'], string> = {
    Confirmadas: 'bg-emerald-600',
    Pendientes: 'bg-amber-400',
    Finalizadas: 'bg-sky-500',
    Canceladas: 'bg-red-400',
  };
  ngOnInit(): void {
    this.cargar();
  }
  cargar(): void {
    this.loading.set(true);
    this.errorCarga.set(false);
    forkJoin({
      usuarios: this.admin.totalUsuarios(),
      alojamientos: this.admin.totalAlojamientos(),
      activas: this.admin.totalAlojamientos('ACTIVA' as EstadoAlojamiento),
      porRevisar: this.admin.totalAlojamientos('PENDIENTE_REVISION' as EstadoAlojamiento),
      datos: this.admin.todasLasReservas(),
    }).subscribe({
      next: ({ usuarios, alojamientos, activas, porRevisar, datos }) => {
        const { reservas, total } = datos;
        const conteo: Record<ReservationStat['label'], number> = {
          Confirmadas: 0, Pendientes: 0, Finalizadas: 0, Canceladas: 0,
        };
        const ciudades = new Map<string, number>();
        for (const r of reservas) {
          const estado = r.estado as string;
          if (estado === 'CANCELADA') {
            conteo.Canceladas++;
          } else if (estado === 'CONFIRMADA') {
            if (r.completada) conteo.Finalizadas++;
            else conteo.Confirmadas++;
          } else {
            conteo.Pendientes++; // EN_REVISION y cualquier otro estado
          }
          if (r.ciudad) {
            ciudades.set(r.ciudad, (ciudades.get(r.ciudad) ?? 0) + 1);
          }
        }
        const base = reservas.length || 1;
        const n = (valor: number) => valor.toLocaleString('es-CO');
        this.metrics.set([
          { label: 'Usuarios registrados', value: n(usuarios), icon: 'users' },
          { label: 'Alojamientos', value: n(alojamientos), icon: 'building', delta: `${n(activas)} activos` },
          { label: 'Reservas', value: n(total), icon: 'chart', delta: `${n(conteo.Confirmadas)} confirmadas` },
          {
            label: 'Alojamientos por revisar',
            value: n(porRevisar),
            icon: 'shield',
            delta: porRevisar > 0 ? 'Requieren acción' : 'Al día',
          },
        ]);
        this.totalReservations.set(total);
        this.reservationStats.set(
          (Object.keys(conteo) as ReservationStat['label'][]).map((label) => ({
            label,
            value: conteo[label],
            percent: Math.round((conteo[label] / base) * 100),
          })),
        );
        this.topCities.set(
          [...ciudades.entries()]
            .map(([city, cantidad]) => ({ city, total: cantidad }))
            .sort((a, b) => b.total - a.total || a.city.localeCompare(b.city, 'es'))
            .slice(0, 5),
        );
        this.muestraParcial.set(reservas.length < total ? { usadas: reservas.length, total } : null);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error al cargar el dashboard:', error);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }
}
