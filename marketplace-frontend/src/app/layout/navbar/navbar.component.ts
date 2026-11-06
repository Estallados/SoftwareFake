import {
  Component,
  computed,
  inject,
  input,
  output,
  signal
} from '@angular/core';

import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';

import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';

import { filter, fromEvent, interval, map, merge, startWith } from 'rxjs';
import {SessionService} from '../../services/session.service';
import {NotificacionService} from '../../services/notificaciones.service';

/** Roles como se muestran en la barra. */
export type Role = 'Huésped' | 'Anfitrión' | 'Administrador';
/** Pantallas de autenticación que puede abrir la barra. */
export type AuthMode = 'login' | 'register' | 'recovery';

/** Opción de la barra: [texto, ruta]. */
type NavItem = [label: string, path: string];

// ======================================
// MENÚS SEGÚN ROL
// ======================================

/** Opciones del huésped. */
const GUEST_ITEMS: NavItem[] = [
  ['Inicio', '/home'],
  ['Explorar', '/explorar'],
  ['Mis favoritos', '/favoritos'],
  ['Mensajes', '/mensajes'],
  ['Mis reservas', '/reservas'],
  ['Mis reseñas', '/resenas']
];

/** Opciones del anfitrión. */
const HOST_ITEMS: NavItem[] = [
  ['Resumen', '/host/dashboard'],
  ['Mis alojamientos', '/host/alojamientos'],
  ['Reservas', '/host/reservas'],
  ['Chat', '/host/chat'],
  ['Reseñas', '/host/resenas']
];

/** Opciones del administrador. */
const ADMIN_ITEMS: NavItem[] = [
  ['Dashboard', '/admin/dashboard'],
  ['Usuarios', '/admin/usuarios'],
  ['Alojamientos', '/admin/alojamientos'],
  ['Reservas', '/admin/reservas'],
  ['Reseñas', '/admin/resenas'],
  ['Reportes', '/admin/reportes'],
  ['Auditoría', '/admin/auditoria']
];

/** Página de inicio de cada rol. */
const ROLE_HOME: Record<Role, string> = {
  Huésped: '/home',
  Anfitrión: '/host/dashboard',
  Administrador: '/admin/dashboard'
};

/** Ruta de las notificaciones de cada rol. */
const NOTIFICATIONS: Record<Role, string | null> = {
  Huésped: '/notificaciones',
  Anfitrión: '/notificaciones',
  Administrador: '/notificaciones'
};

/**
 * Barra de navegación superior: opciones según el rol, contador de notificaciones sin leer, datos
 * del usuario y cierre de sesión.
 */
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

  /** Router de Angular. */
  private readonly router = inject(Router);

  /** Sesión del usuario. */
  protected readonly session = inject(SessionService);

  /** Consulta las notificaciones sin leer. */
  private readonly notificacionService = inject(NotificacionService);

  /** Contador que puede enviar la página padre. */
  unread = input(0);
  /** Notificaciones sin leer del usuario (se consulta al navegar, cada minuto y al marcar como leídas). */
  readonly sinLeer = signal(0);

  /** Número que se muestra en la campana. */
  readonly contador = computed(() => Math.max(this.unread(), this.sinLeer()));

  /** Avisa que se quiere abrir el login, el registro o la recuperación. */
  openAuth = output<AuthMode>();

  /** Indica si el menú del usuario está abierto. */
  roleMenu = false;

  // Se conservan para compatibilidad con el HTML.

  /** Se conserva por compatibilidad con la plantilla. */
  readonly roles: Role[] = [];

  /** Página de inicio de cada rol. */
  readonly roleHome = ROLE_HOME;

  // ======================================
  // DETECTAR CAMBIOS DE RUTA
  // ======================================

  /** URL actual; se actualiza en cada navegación. */
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

  /** Fuerza a recalcular la sesión al cerrarla. */
  private readonly sesionVersion = signal(0);

  // ======================================
  // OBTENER SESIÓN REAL
  // ======================================

  /** Sesión guardada (token, correo y rol), o null. */
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

  /** Indica si hay sesión. */
  readonly estaAutenticado = computed(
    () => this.sesion() !== null
  );

  // ======================================
  // IDENTIFICAR ROL REAL
  // ======================================

  /**
   * Convierte el rol del backend en el de la barra.
   *
   * @param valor rol como llega del backend
   * @returns el rol, o null si no lo reconoce
   */
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

  /** Rol del usuario; Huésped si no hay sesión. */
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

  /**
   * Iniciales, nombre y correo que se muestran; sale del correo porque la sesión no guarda el
   * nombre.
   */
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

  /** Opciones de la barra según el rol. */
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

  /** Página de inicio según el rol. */
  readonly homePath = computed(() => {

    if (!this.estaAutenticado()) {
      return '/home';
    }

    return ROLE_HOME[this.role()];
  });

  /** Ruta de las notificaciones; null sin sesión. */
  readonly notificationsPath = computed(() => {

    if (!this.estaAutenticado()) {
      return null;
    }

    return NOTIFICATIONS[this.role()];
  });

  /** Indica si se está en el login. */
  readonly isLoginPage = computed(() => {

    return this.url().split(/[?#]/)[0] === '/login';
  });

  /**
   * Actualiza el contador de notificaciones al navegar, cada minuto y cuando se marcan como
   * leídas.
   */
  constructor() {
    merge(
      toObservable(this.url),
      interval(60_000),
      fromEvent(window, 'notificaciones-actualizadas'),
    )
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.actualizarSinLeer());
  }

  /** Consulta cuántas notificaciones sin leer tiene el usuario. */
  private actualizarSinLeer(): void {
    if (!this.estaAutenticado()) {
      this.sinLeer.set(0);
      return;
    }
    this.notificacionService.listar().subscribe({
      next: (lista) => this.sinLeer.set((lista ?? []).filter((x) => !x.leida).length),
      error: () => undefined, // un fallo puntual no debe romper la barra
    });
  }

  // ======================================
  // ESTILOS DE NAVEGACIÓN
  // ======================================

  /** Clases de la opción activa. */
  readonly activeClass =
    'text-emerald-700 after:absolute after:bottom-0 ' +
    'after:left-4 after:right-4 after:h-0.5 ' +
    'after:bg-emerald-700';

  /** Clases de las demás opciones. */
  readonly inactiveClass =
    'text-slate-500 hover:text-slate-900';

  // ======================================
  // COMPATIBILIDAD CON HTML EXISTENTE
  // ======================================

  /**
   * Clases de una opción del menú de roles.
   *
   * @param r rol
   * @returns clases CSS
   */
  roleItemClass(r: Role): string {

    return (
      'flex w-full items-center justify-between ' +
      'rounded-xl px-3 py-3 text-sm font-semibold ' +
      (this.role() === r
        ? 'text-emerald-700'
        : 'text-slate-500')
    );
  }

  /**
   * Ícono de un rol.
   *
   * @param r rol
   * @returns nombre del ícono
   */
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

  /**
   * Cierra el menú; no se permite cambiar de rol desde la barra.
   *
   * @param _r rol elegido (no se usa)
   */
  pickRole(_r: Role): void {

    this.roleMenu = false;
  }

  // ======================================
  // AUTENTICACIÓN
  // ======================================

  /**
   * Abre la pantalla de autenticación pedida.
   *
   * @param mode login, registro o recuperación
   */
  auth(mode: AuthMode): void {

    this.roleMenu = false;
    this.openAuth.emit(mode);
  }

  // ======================================
  // CERRAR SESIÓN
  // ======================================

  /** Cierra la sesión y lleva al login. */
  cerrarSesion(): void {
    this.session.cerrarSesion();   // borra el storage y pone la señal en null
    this.roleMenu = false;
    this.router.navigateByUrl('/login');
  }

  // ======================================
  // CERRAR MENÚ
  // ======================================

  /** Cierra el menú del usuario. */
  closeMenu(): void {

    this.roleMenu = false;
  }
}
