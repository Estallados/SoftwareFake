import { Component, computed, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

export type Role = 'Huésped' | 'Anfitrión' | 'Administrador';
export type AuthMode = 'login' | 'register' | 'recovery';

/** [texto del menú, ruta] — rutas tomadas de tu app.routes.ts */
type NavItem = [label: string, path: string];

const GUEST_ITEMS: NavItem[] = [
  ['Inicio', '/home'],
  ['Explorar', '/explorar'],
  ['Mis favoritos', '/favoritos'],
  ['Mensajes', '/mensajes'],
  ['Mis reservas', '/reservas'],
  ['Mis reseñas', '/resenas'],
];

const HOST_ITEMS: NavItem[] = [
  ['Resumen', '/host/dashboard'],
  ['Mis alojamientos', '/host/alojamientos'],
  ['Reservas', '/host/reservas'],
  ['Reseñas', '/host/resenas'],
];

const ADMIN_ITEMS: NavItem[] = [
  ['Dashboard', '/admin/dashboard'],
  ['Usuarios', '/admin/usuarios'],
  ['Alojamientos', '/admin/alojamientos'],
  ['Reservas', '/admin/reservas'],
  ['Reseñas', '/admin/resenas'],
  ['Reportes', '/admin/reportes'],
  ['Auditoría', '/admin/auditoria'],
];

/** Pantalla de inicio de cada rol (al cambiar de rol en el selector) */
const ROLE_HOME: Record<Role, string> = {
  Huésped: '/home',
  Anfitrión: '/host/dashboard',
  Administrador: '/admin/dashboard',
};

/** Ruta de notificaciones por rol (el admin no tiene en tus rutas) */
const NOTIFICATIONS: Record<Role, string | null> = {
  Huésped: '/notificaciones',
  Anfitrión: '/host/notificaciones',
  Administrador: null,
};

const USERS: Record<Role, { initials: string; name: string }> = {
  Huésped: { initials: 'MC', name: 'Mariana Cruz' },
  Anfitrión: { initials: 'AV', name: 'Andrés Villa' },
  Administrador: { initials: 'AD', name: 'Admin LR' },
};

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  host: { class: 'sticky top-0 z-40 block' },
})
export class NavbarComponent {
  private router = inject(Router);

  /** Notificaciones sin leer del rol actual */
  unread = input(0);
  /** Abrir el modal de login / registro / recuperación */
  openAuth = output<AuthMode>();

  roleMenu = false;
  readonly roles: Role[] = ['Huésped', 'Anfitrión', 'Administrador'];
  readonly roleHome = ROLE_HOME;

  // URL actual como signal
  private url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  // El rol se deduce de la URL: /admin/** , /host/** o el resto (huésped)
  role = computed<Role>(() => {
    const u = this.url();
    if (u.startsWith('/admin')) return 'Administrador';
    if (u.startsWith('/host')) return 'Anfitrión';
    return 'Huésped';
  });

  items = computed(() => {
    switch (this.role()) {
      case 'Huésped': return GUEST_ITEMS;
      case 'Anfitrión': return HOST_ITEMS;
      default: return ADMIN_ITEMS;
    }
  });

  user = computed(() => USERS[this.role()]);
  homePath = computed(() => ROLE_HOME[this.role()]);
  notificationsPath = computed(() => NOTIFICATIONS[this.role()]);

  // Clases del enlace activo / inactivo (el estado lo da routerLinkActive)
  readonly activeClass =
    'text-emerald-700 after:absolute after:bottom-0 after:left-4 after:right-4 after:h-0.5 after:bg-emerald-700';
  readonly inactiveClass = 'text-slate-500 hover:text-slate-900';

  roleItemClass(r: Role): string {
    return (
      'flex w-full items-center justify-between rounded-xl px-3 py-3 text-sm font-semibold hover:bg-slate-50 ' +
      (this.role() === r ? 'text-emerald-700' : '')
    );
  }

  roleIcon(r: Role): 'shield' | 'building' | 'users' {
    return r === 'Administrador' ? 'shield' : r === 'Anfitrión' ? 'building' : 'users';
  }

  pickRole(r: Role) {
    this.roleMenu = false;
    this.router.navigateByUrl(ROLE_HOME[r]);
  }

  auth(mode: AuthMode) {
    this.roleMenu = false;
    this.openAuth.emit(mode);
  }

  closeMenu() {
    this.roleMenu = false;
  }
}
