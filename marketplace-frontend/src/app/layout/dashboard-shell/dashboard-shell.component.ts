import { Component, computed, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/** Roles que tienen panel lateral. */
export type DashboardRole = 'Anfitrión' | 'Administrador';

/** Opción del menú lateral. */
interface NavItem {
  /** Ícono de la opción. */
  icon: 'chart' | 'building' | 'calendar' | 'users' | 'bell' | 'star' | 'clipboard' | 'shield';
  /** Texto de la opción. */
  label: string;
  /** Ruta a la que lleva. */
  path: string;
}

// Solo opciones que existen en app.routes.ts
/** Menú lateral del anfitrión. */
const HOST_ITEMS: NavItem[] = [
  { icon: 'chart', label: 'Resumen', path: '/host/dashboard' },
  { icon: 'building', label: 'Mis alojamientos', path: '/host/alojamientos' },
  { icon: 'calendar', label: 'Reservas', path: '/host/reservas' },
  { icon: 'users', label: 'Mensajes', path: '/mensajes' },
  { icon: 'bell', label: 'Notificaciones', path: '/host/notificaciones' },
  { icon: 'star', label: 'Reseñas', path: '/host/resenas' },
  { icon: 'users', label: 'Mi perfil', path: '/perfil' },
];

/** Menú lateral del administrador. */
const ADMIN_ITEMS: NavItem[] = [
  { icon: 'chart', label: 'Dashboard', path: '/admin/dashboard' },
  { icon: 'users', label: 'Usuarios', path: '/admin/usuarios' },
  { icon: 'building', label: 'Aprobación de alojamientos', path: '/admin/alojamientos' },
  { icon: 'clipboard', label: 'Reservas', path: '/admin/reservas' },
  { icon: 'star', label: 'Reseñas', path: '/admin/resenas' },
  { icon: 'clipboard', label: 'Reportes', path: '/admin/reportes' },
  { icon: 'shield', label: 'Auditoría', path: '/admin/auditoria' },
  { icon: 'users', label: 'Mi perfil', path: '/perfil' },
];

/**
 * Marco de los paneles del anfitrión y del administrador: menú lateral según el rol y el contenido
 * de la página.
 */
@Component({
  selector: 'app-dashboard-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './dashboard-shell.component.html',
})
export class DashboardShellComponent {
  /** Rol del panel. */
  role = input.required<DashboardRole>();

  /** Opciones del menú según el rol. */
  items = computed(() => (this.role() === 'Anfitrión' ? HOST_ITEMS : ADMIN_ITEMS));

  /** Título del panel según el rol. */
  heading = computed(() =>
    this.role() === 'Anfitrión' ? 'Panel del anfitrión' : 'Administración',
  );

  /** Clases de la opción activa. */
  readonly activeClass = 'bg-emerald-700 text-white';

  /** Clases de las demás opciones. */
  readonly inactiveClass = 'text-slate-600 hover:bg-slate-100';
}
