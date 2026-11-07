import { Component, computed, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';


export type DashboardRole = 'Anfitrión' | 'Administrador';


interface NavItem {
  /** Ícono de la opción. */
  icon: 'chart' | 'building' | 'calendar' | 'users' | 'bell' | 'star' | 'clipboard' | 'shield';
  /** Texto de la opción. */
  label: string;
  /** Ruta a la que lleva. */
  path: string;
}

// Solo opciones que existen en app.routes.ts

const HOST_ITEMS: NavItem[] = [
  { icon: 'chart', label: 'Resumen', path: '/host/dashboard' },
  { icon: 'building', label: 'Mis alojamientos', path: '/host/alojamientos' },
  { icon: 'calendar', label: 'Reservas', path: '/host/reservas' },
  { icon: 'users', label: 'Mensajes', path: '/mensajes' },
  { icon: 'bell', label: 'Notificaciones', path: '/host/notificaciones' },
  { icon: 'star', label: 'Reseñas', path: '/host/resenas' },
  { icon: 'users', label: 'Mi perfil', path: '/perfil' },
];


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


@Component({
  selector: 'app-dashboard-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './dashboard-shell.component.html',
})
export class DashboardShellComponent {

  role = input.required<DashboardRole>();

 
  items = computed(() => (this.role() === 'Anfitrión' ? HOST_ITEMS : ADMIN_ITEMS));


  heading = computed(() =>
    this.role() === 'Anfitrión' ? 'Panel del anfitrión' : 'Administración',
  );


  readonly activeClass = 'bg-emerald-700 text-white';


  readonly inactiveClass = 'text-slate-600 hover:bg-slate-100';
}
