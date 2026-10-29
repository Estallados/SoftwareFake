
import {
  Component,
  computed,
  inject,
  input,
  output,
  signal
} from '@angular/core';

import { toSignal } from '@angular/core/rxjs-interop';

import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';

import { filter, map, startWith } from 'rxjs';

export type Role = 'Huésped' | 'Anfitrión' | 'Administrador';
export type AuthMode = 'login' | 'register' | 'recovery';

type NavItem = [label: string, path: string];

// ======================================
// MENÚS SEGÚN ROL
// ======================================

const GUEST_ITEMS: NavItem[] = [
  ['Inicio', '/home'],
  ['Explorar', '/explorar'],
  ['Mis favoritos', '/favoritos'],
  ['Mensajes', '/mensajes'],
  ['Mis reservas', '/reservas'],
  ['Mis reseñas', '/resenas']
];

const HOST_ITEMS: NavItem[] = [
  ['Resumen', '/host/dashboard'],
  ['Mis alojamientos', '/host/alojamientos'],
  ['Reservas', '/host/reservas'],
  ['Reseñas', '/host/resenas']
];

const ADMIN_ITEMS: NavItem[] = [
  ['Dashboard', '/admin/dashboard'],
  ['Usuarios', '/admin/usuarios'],
  ['Alojamientos', '/admin/alojamientos'],
  ['Reservas', '/admin/reservas'],
  ['Reseñas', '/admin/resenas'],
  ['Reportes', '/admin/reportes'],
  ['Auditoría', '/admin/auditoria']
];

const ROLE_HOME: Record<Role, string> = {
  Huésped: '/home',
  Anfitrión: '/host/dashboard',
  Administrador: '/admin/dashboard'
};

const NOTIFICATIONS: Record<Role, string | null> = {
  Huésped: '/notificaciones',
  Anfitrión: '/notificaciones',
  Administrador: '/notificaciones'
};

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  host: {
    class: 'sticky top-0 z-40 block'
  }
})
export class NavbarComponent {

  private readonly router = inject(Router);

  unread = input(0);
  openAuth = output<AuthMode>();

  roleMenu = false;

  // Se conservan para compatibilidad con el HTML.
  readonly roles: Role[] = [];
  readonly roleHome = ROLE_HOME;

  // ======================================
  // DETECTAR CAMBIOS DE RUTA
  // ======================================

  private readonly url = toSignal(
    this.router.events.pipe(
      filter(
        (event): event is NavigationEnd =>
          event instanceof NavigationEnd
      ),
      map(event => event.urlAfterRedirects),
      startWith(this.router.url)
    ),
    {
      initialValue: this.router.url
    }
  );

  // Permite actualizar la sesión al cerrar sesión.
  private readonly sesionVersion = signal(0);

  // ======================================
  // OBTENER SESIÓN REAL
  // ======================================

  readonly sesion = computed(() => {

    // Se consulta la URL para actualizar la sesión
    // después de navegar desde el login.
    this.url();
    this.sesionVersion();

    const token = sessionStorage.getItem('token');
    const correo = sessionStorage.getItem('correo');
    const rol = sessionStorage.getItem('rol');

    if (!token || !correo || !rol) {
      return null;
    }

    return {
      token,
      correo,
      rol
    };
  });

  readonly estaAutenticado = computed(
    () => this.sesion() !== null
  );

  // ======================================
  // IDENTIFICAR ROL REAL
  // ======================================

  private normalizarRol(valor: string): Role | null {

    const rol = valor
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    if (
      rol === 'HUESPED' ||
      rol === 'ROLE_HUESPED'
    ) {
      return 'Huésped';
    }

    if (
      rol === 'ANFITRION' ||
      rol === 'ROLE_ANFITRION'
    ) {
      return 'Anfitrión';
    }

    if (
      rol === 'ADMIN' ||
      rol === 'ADMINISTRADOR' ||
      rol === 'ROLE_ADMIN' ||
      rol === 'ROLE_ADMINISTRADOR'
    ) {
      return 'Administrador';
    }

    return null;
  }

  readonly role = computed<Role>(() => {

    const sesion = this.sesion();

    if (!sesion) {
      return 'Huésped';
    }

    return this.normalizarRol(sesion.rol) ?? 'Huésped';
  });

  // ======================================
  // DATOS DEL USUARIO
  // ======================================

  readonly user = computed(() => {

    const sesion = this.sesion();

    if (!sesion) {
      return {
        initials: '?',
        name: 'Invitado',
        email: ''
      };
    }

    const correo = sesion.correo;

    // Como el backend solo devuelve correo,
    // mostramos ese dato sin inventar nombres.
    const nombreVisible = correo.split('@')[0];

    const initials = nombreVisible
      .split(/[._\-\s]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(parte => parte.charAt(0).toUpperCase())
      .join('') || 'U';

    return {
      initials,
      name: nombreVisible,
      email: correo
    };
  });

  // ======================================
  // NAVBAR SEGÚN ROL
  // ======================================

  readonly items = computed<NavItem[]>(() => {

    if (!this.estaAutenticado()) {
      return [
        ['Inicio', '/home'],
        ['Explorar', '/explorar']
      ];
    }

    switch (this.role()) {

      case 'Anfitrión':
        return HOST_ITEMS;

      case 'Administrador':
        return ADMIN_ITEMS;

      default:
        return GUEST_ITEMS;
    }
  });

  readonly homePath = computed(() => {

    if (!this.estaAutenticado()) {
      return '/home';
    }

    return ROLE_HOME[this.role()];
  });

  readonly notificationsPath = computed(() => {

    if (!this.estaAutenticado()) {
      return null;
    }

    return NOTIFICATIONS[this.role()];
  });

  readonly isLoginPage = computed(() => {

    return this.url().split(/[?#]/)[0] === '/login';
  });

  // ======================================
  // ESTILOS DE NAVEGACIÓN
  // ======================================

  readonly activeClass =
    'text-emerald-700 after:absolute after:bottom-0 ' +
    'after:left-4 after:right-4 after:h-0.5 ' +
    'after:bg-emerald-700';

  readonly inactiveClass =
    'text-slate-500 hover:text-slate-900';

  // ======================================
  // COMPATIBILIDAD CON HTML EXISTENTE
  // ======================================

  roleItemClass(r: Role): string {

    return (
      'flex w-full items-center justify-between ' +
      'rounded-xl px-3 py-3 text-sm font-semibold ' +
      (this.role() === r
        ? 'text-emerald-700'
        : 'text-slate-500')
    );
  }

  roleIcon(r: Role): 'shield' | 'building' | 'users' {

    if (r === 'Administrador') {
      return 'shield';
    }

    if (r === 'Anfitrión') {
      return 'building';
    }

    return 'users';
  }

  // No se permite cambiar de rol desde la navbar.
  pickRole(_r: Role): void {

    this.roleMenu = false;
  }

  // ======================================
  // AUTENTICACIÓN
  // ======================================

  auth(mode: AuthMode): void {

    this.roleMenu = false;
    this.openAuth.emit(mode);
  }

  // ======================================
  // CERRAR SESIÓN
  // ======================================

  cerrarSesion(): void {

    sessionStorage.removeItem('token');
    sessionStorage.removeItem('rol');
    sessionStorage.removeItem('correo');

    this.roleMenu = false;

    // Actualiza los datos visibles.
    this.sesionVersion.update(valor => valor + 1);

    this.router.navigateByUrl('/login');
  }

  // ======================================
  // CERRAR MENÚ
  // ======================================

  closeMenu(): void {

    this.roleMenu = false;
  }
}
