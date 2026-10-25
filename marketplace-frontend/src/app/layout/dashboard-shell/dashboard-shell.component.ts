import { Component, computed, input, output } from '@angular/core';

export type View =
  | 'dashboard' | 'alojamientos' | 'reservas' | 'mensajes' | 'disponibilidad'
  | 'notificaciones' | 'resenas' | 'perfil' | 'usuarios' | 'reportes' | 'auditoria'
  | (string & {}); // ajusta a las vistas reales de tu app

export type DashboardRole = 'Anfitrión' | 'Administrador';

interface NavItem {
  icon: 'chart' | 'building' | 'calendar' | 'users' | 'bell' | 'star' | 'clipboard' | 'shield';
  label: string;
  target: View;
}

const HOST_ITEMS: NavItem[] = [
  { icon: 'chart', label: 'Resumen', target: 'dashboard' },
  { icon: 'building', label: 'Mis alojamientos', target: 'alojamientos' },
  { icon: 'calendar', label: 'Reservas', target: 'reservas' },
  { icon: 'users', label: 'Mensajes', target: 'mensajes' },
  { icon: 'calendar', label: 'Disponibilidad', target: 'disponibilidad' },
  { icon: 'bell', label: 'Notificaciones', target: 'notificaciones' },
  { icon: 'star', label: 'Reseñas', target: 'resenas' },
  { icon: 'users', label: 'Mi perfil', target: 'perfil' },
];

const ADMIN_ITEMS: NavItem[] = [
  { icon: 'chart', label: 'Dashboard', target: 'dashboard' },
  { icon: 'users', label: 'Usuarios', target: 'usuarios' },
  { icon: 'building', label: 'Aprobación de alojamientos', target: 'alojamientos' },
  { icon: 'clipboard', label: 'Reservas', target: 'reservas' },
  { icon: 'bell', label: 'Notificaciones', target: 'notificaciones' },
  { icon: 'star', label: 'Reseñas', target: 'resenas' },
  { icon: 'clipboard', label: 'Reportes', target: 'reportes' },
  { icon: 'shield', label: 'Auditoría', target: 'auditoria' },
  { icon: 'users', label: 'Mi perfil', target: 'perfil' },
];

@Component({
  selector: 'app-dashboard-shell',
  standalone: true,
  templateUrl: './dashboard-shell.component.html',
})
export class DashboardShellComponent {
  // Props de React
  role = input.required<DashboardRole>();
  active = input.required<View>();
  navigate = output<View>();

  items = computed(() => (this.role() === 'Anfitrión' ? HOST_ITEMS : ADMIN_ITEMS));
  heading = computed(() =>
    this.role() === 'Anfitrión' ? 'Panel del anfitrión' : 'Administración',
  );

  itemClass(target: View): string {
    return (
      'flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition ' +
      (this.active() === target
        ? 'bg-emerald-700 text-white'
        : 'text-slate-600 hover:bg-slate-100')
    );
  }
}
