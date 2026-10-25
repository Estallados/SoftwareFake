import { Component, computed, input, output } from '@angular/core';

export type Role = 'Huésped' | 'Anfitrión' | 'Administrador';
export type AuthMode = 'login' | 'register' | 'recovery';
export type View =
  | 'inicio' | 'explorar' | 'favoritos' | 'mensajes' | 'reservas' | 'resenas'
  | 'dashboard' | 'alojamientos' | 'usuarios' | 'reportes' | 'auditoria'
  | 'notificaciones' | 'perfil' | 'detalle' | 'detalle-reserva'
  | 'detalle-usuario' | 'detalle-admin-alojamiento'
  | (string & {}); // amplía con las vistas reales de tu app

type NavItem = [label: string, target: View];

const GUEST_ITEMS: NavItem[] = [
  ['Inicio', 'inicio'],
  ['Explorar', 'explorar'],
  ['Mis favoritos', 'favoritos'],
  ['Mensajes', 'mensajes'],
  ['Mis reservas', 'reservas'],
  ['Mis reseñas', 'resenas'],
];

const HOST_ITEMS: NavItem[] = [
  ['Resumen', 'dashboard'],
  ['Mis alojamientos', 'alojamientos'],
  ['Reservas', 'reservas'],
];

const ADMIN_ITEMS: NavItem[] = [
  ['Dashboard', 'dashboard'],
  ['Usuarios', 'usuarios'],
  ['Alojamientos', 'alojamientos'],
  ['Reseñas', 'resenas'],
  ['Reportes', 'reportes'],
  ['Auditoría', 'auditoria'],
];

const USERS: Record<Role, { initials: string; name: string }> = {
  Huésped: { initials: 'MC', name: 'Mariana Cruz' },
  Anfitrión: { initials: 'AV', name: 'Andrés Villa' },
  Administrador: { initials: 'AD', name: 'Admin LR' },
};

@Component({
  selector: 'app-navbar',
  standalone: true,
  templateUrl: './navbar.component.html',
})
export class NavbarComponent {
  // ---- Entradas (estado que en React vivía en App) ----
  role = input<Role>('Huésped');
  view = input<View>('inicio');
  /** Notificaciones sin leer del rol actual */
  unread = input(0);

  // ---- Salidas (callbacks) ----
  navigate = output<View>();
  switchRole = output<Role>();
  openAuth = output<AuthMode>();

  // ---- Estado interno ----
  roleMenu = false;
  readonly roles: Role[] = ['Huésped', 'Anfitrión', 'Administrador'];

  items = computed(() => {
    switch (this.role()) {
      case 'Huésped': return GUEST_ITEMS;
      case 'Anfitrión': return HOST_ITEMS;
      default: return ADMIN_ITEMS;
    }
  });

  user = computed(() => USERS[this.role()]);
  homeTarget = computed<View>(() => (this.role() === 'Huésped' ? 'inicio' : 'dashboard'));

  isActive(target: View): boolean {
    const v = this.view();
    return (
      v === target ||
      (v === 'detalle-reserva' && target === 'reservas') ||
      (v === 'detalle-usuario' && target === 'usuarios') ||
      (v === 'detalle-admin-alojamiento' && target === 'alojamientos')
    );
  }

  itemClass(target: View): string {
    return (
      'relative h-full px-4 text-sm font-semibold transition ' +
      (this.isActive(target)
        ? 'text-emerald-700 after:absolute after:bottom-0 after:left-4 after:right-4 after:h-0.5 after:bg-emerald-700'
        : 'text-slate-500 hover:text-slate-900')
    );
  }

  roleItemClass(r: Role): string {
    return (
      'flex w-full items-center justify-between rounded-xl px-3 py-3 text-sm font-semibold hover:bg-slate-50 ' +
      (this.role() === r ? 'text-emerald-700' : '')
    );
  }

  roleIcon(r: Role): 'shield' | 'building' | 'users' {
    return r === 'Administrador' ? 'shield' : r === 'Anfitrión' ? 'building' : 'users';
  }

  // ---- Acciones ----
  pickRole(r: Role) {
    this.roleMenu = false;
    this.switchRole.emit(r);
  }

  auth(mode: AuthMode) {
    this.roleMenu = false;
    this.openAuth.emit(mode);
  }

  goProfile() {
    this.roleMenu = false;
    this.navigate.emit('perfil');
  }
}
