
import {
  Injectable,
  computed,
  signal
} from '@angular/core';

// ======================================
// ROLES DEL SISTEMA
// ======================================


export type RolUsuario =
  | 'Huésped'
  | 'Anfitrión'
  | 'Administrador';

// ======================================
// DATOS DE LA SESIÓN
// ======================================


export interface DatosSesion {
  
  token: string;

  rol: string;
 
  correo: string;
}


@Injectable({
  providedIn: 'root'
})
export class SessionService {

  // ======================================
  // ESTADO DE LA SESIÓN
  // ======================================

  
  private readonly sesion = signal<DatosSesion | null>(
    this.recuperarSesion()
  );

 
  readonly usuario = this.sesion.asReadonly();

  // ======================================
  // VERIFICAR AUTENTICACIÓN
  // ======================================

  
  readonly autenticado = computed(() => {

    const datos = this.sesion();

    return (
      datos !== null &&
      this.tokenVigente(datos.token)
    );
  });

  // ======================================
  // CORREO DEL USUARIO
  // ======================================

  readonly correo = computed(() => {

    return this.sesion()?.correo ?? '';
  });

  // ======================================
  // ROL DEL USUARIO
  // ======================================

  readonly rol = computed<RolUsuario | null>(() => {

    const datos = this.sesion();

    if (!datos || !this.tokenVigente(datos.token)) {
      return null;
    }

    return this.normalizarRol(datos.rol);
  });

  // ======================================
  // 1. GUARDAR SESIÓN
  // ======================================

  
  iniciarSesion(datos: DatosSesion): void {

    if (
      !datos.token ||
      !datos.rol ||
      !datos.correo
    ) {
      throw new Error(
        'La respuesta de autenticación está incompleta.'
      );
    }

    if (!this.tokenVigente(datos.token)) {
      throw new Error(
        'El token JWT es inválido o ha expirado.'
      );
    }

    sessionStorage.setItem(
      'token',
      datos.token
    );

    sessionStorage.setItem(
      'rol',
      datos.rol
    );

    sessionStorage.setItem(
      'correo',
      datos.correo
    );

    this.sesion.set({ ...datos });
  }

  // ======================================
  // 2. OBTENER TOKEN JWT
  // ======================================


  obtenerToken(): string | null {

    const datos = this.sesion();

    if (!datos) {
      return null;
    }

    if (!this.tokenVigente(datos.token)) {
      return null;
    }

    return datos.token;
  }

  // ======================================
  // 3. VERIFICAR PERMISOS
  // ======================================

  tieneRol(...roles: RolUsuario[]): boolean {

    const rolActual = this.rol();

    return (
      rolActual !== null &&
      roles.includes(rolActual)
    );
  }

  // ======================================
  // 4. CERRAR SESIÓN
  // ======================================

  
  cerrarSesion(): void {

    sessionStorage.removeItem('token');
    sessionStorage.removeItem('rol');
    sessionStorage.removeItem('correo');

    this.sesion.set(null);
  }

  // ======================================
  // 5. RECUPERAR SESIÓN GUARDADA
  // ======================================

 
  private recuperarSesion(): DatosSesion | null {

    const token =
      sessionStorage.getItem('token');

    const rol =
      sessionStorage.getItem('rol');

    const correo =
      sessionStorage.getItem('correo');

    if (!token || !rol || !correo) {
      return null;
    }

    if (!this.tokenVigente(token)) {

      sessionStorage.removeItem('token');
      sessionStorage.removeItem('rol');
      sessionStorage.removeItem('correo');

      return null;
    }

    return {
      token,
      rol,
      correo
    };
  }

  // ======================================
  // 6. NORMALIZAR ROL
  // ======================================


  private normalizarRol(
    valor: string
  ): RolUsuario | null {

    const rol = valor
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/^ROLE_/, '');

    switch (rol) {

      case 'HUESPED':
        return 'Huésped';

      case 'ANFITRION':
        return 'Anfitrión';

      case 'ADMIN':
      case 'ADMINISTRADOR':
        return 'Administrador';

      default:
        return null;
    }
  }

  // ======================================
  // 7. COMPROBAR EXPIRACIÓN DEL JWT
  // ======================================


  private tokenVigente(token: string): boolean {

    try {

      const partes = token.split('.');

      if (partes.length !== 3) {
        return false;
      }

      const base64 = partes[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const payload = JSON.parse(
        atob(base64)
      ) as {
        exp?: number;
      };

      if (typeof payload.exp !== 'number') {
        return false;
      }

      return payload.exp * 1000 > Date.now();

    } catch {

      return false;
    }
  }
}
