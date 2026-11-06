
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from './api-url';
import { Observable } from 'rxjs';

// ======================================
// ROLES PERMITIDOS EN REGISTRO
// ======================================

export type RegistroRol = 'HUESPED' | 'ANFITRION';

// ======================================
// 1. REGISTRO DE USUARIOS
// ======================================

export interface RegistroRequest {
  nombre: string;
  correo: string;
  telefono: string;
  rol: RegistroRol;
  contrasenia: string;
}

export interface RegistroResponse {
  message: string;
  correo: string;
  requiereVerificacion: boolean;
}

// ======================================
// 2. VERIFICACIÓN DE CORREO
// ======================================

export interface VerificacionResponse {
  message: string;
  token: string;
  rol: string;
  correo: string;
}

// ======================================
// 3. REENVÍO DE CÓDIGO
// ======================================

export interface ReenvioCodigoResponse {
  message: string;
}

// ======================================
// 4. INICIO DE SESIÓN
// ======================================

export interface LoginRequest {
  correo: string;
  contrasenia: string;
  codigo?: string;
}

export interface LoginResponse {
  token: string;
  rol: string;
  correo: string;
  message?: string;
  cuentaNueva?: boolean;
}

// ======================================
// 5. RECUPERACIÓN DE CONTRASEÑA
// ======================================

export interface RecuperarContrasenaRequest {
  correo: string;
}

export interface RecuperarContrasenaResponse {
  message: string;
}

// ======================================
// 6. RESTABLECER CONTRASEÑA
// ======================================

export interface RestablecerContrasenaRequest {
  token: string;
  contrasenia: string;
}

export interface RestablecerContrasenaResponse {
  message: string;
}

// ======================================
// 7. CONFIGURACIÓN DE GOOGLE
// ======================================

export interface GoogleConfiguracionResponse {
  habilitado: boolean;
  clientId: string | null;
}

// ======================================
// 8. LOGIN CON GOOGLE
// ======================================

export interface GoogleLoginRequest {
  idToken: string;
  rol: RegistroRol;
  codigo?: string;
}

export interface GoogleLoginResponse
  extends LoginResponse {
  cuentaNueva?: boolean;
}

// ======================================
// SERVICIO DE AUTENTICACIÓN
// ======================================

@Injectable({
  providedIn: 'root'
})
export class Auth {

  // ======================================
  // INYECCIÓN DE DEPENDENCIAS
  // ======================================

  private readonly http = inject(HttpClient);

  private readonly baseUrl = inject(API_URL);

  // URL BASE:
  // http://localhost:8081/auth

  private readonly apiUrl =
    `${this.baseUrl.replace(/\/+$/, '')}/auth`;

  // ======================================
  // 1. REGISTRAR USUARIO
  // POST /auth/register
  // ======================================

  registrar(
    datos: RegistroRequest
  ): Observable<RegistroResponse> {

    return this.http.post<RegistroResponse>(
      `${this.apiUrl}/register`,
      {
        nombre: datos.nombre.trim(),
        correo: datos.correo.trim().toLowerCase(),
        telefono: datos.telefono?.trim() ?? '',
        rol: datos.rol,
        contrasenia: datos.contrasenia
      }
    );

  }

  // ======================================
  // 2. VERIFICAR CORREO
  // POST /auth/verificar
  // ======================================

  verificarCorreo(
    correo: string,
    codigo: string
  ): Observable<VerificacionResponse> {

    return this.http.post<VerificacionResponse>(
      `${this.apiUrl}/verificar`,
      {
        correo: correo.trim().toLowerCase(),
        codigo: codigo.trim()
      }
    );

  }

  // ======================================
  // 3. REENVIAR CÓDIGO DE VERIFICACIÓN
  // POST /auth/reenviarcodigo
  // ======================================

  reenviarCodigo(
    correo: string
  ): Observable<ReenvioCodigoResponse> {

    return this.http.post<ReenvioCodigoResponse>(
      `${this.apiUrl}/reenviarcodigo`,
      {
        correo: correo.trim().toLowerCase()
      }
    );

  }

  // ======================================
  // 4. INICIAR SESIÓN NORMAL
  // POST /auth/login
  // ======================================

  login(
    datos: LoginRequest
  ): Observable<LoginResponse> {

    const solicitud: LoginRequest = {

      correo: datos.correo.trim().toLowerCase(),

      contrasenia: datos.contrasenia,

      ...(datos.codigo?.trim()
        ? {
          codigo: datos.codigo.trim()
        }
        : {})

    };

    return this.http.post<LoginResponse>(
      `${this.apiUrl}/login`,
      solicitud
    );

  }

  // ======================================
  // 5. SOLICITAR RECUPERACIÓN
  // POST /auth/recuperar
  // ======================================

  recuperarContrasena(
    correo: string
  ): Observable<RecuperarContrasenaResponse> {

    const datos: RecuperarContrasenaRequest = {

      correo: correo.trim().toLowerCase()

    };

    return this.http.post<RecuperarContrasenaResponse>(
      `${this.apiUrl}/recuperar`,
      datos
    );

  }

  // ======================================
  // 6. RESTABLECER CONTRASEÑA
  // POST /auth/restablecer
  // ======================================

  restablecerContrasena(
    token: string,
    contrasenia: string
  ): Observable<RestablecerContrasenaResponse> {

    const datos: RestablecerContrasenaRequest = {

      token: token.trim(),

      contrasenia: contrasenia

    };

    return this.http.post<RestablecerContrasenaResponse>(
      `${this.apiUrl}/restablecer`,
      datos
    );

  }

  // ======================================
  // 7. CONSULTAR CONFIGURACIÓN GOOGLE
  // GET /auth/google/configuracion
  // ======================================

  configuracionGoogle():
    Observable<GoogleConfiguracionResponse> {

    return this.http.get<GoogleConfiguracionResponse>(
      `${this.apiUrl}/google/configuracion`
    );

  }

  // ======================================
  // 8. INICIAR SESIÓN CON GOOGLE
  // POST /auth/google
  // ======================================

  iniciarConGoogle(
    idToken: string,
    rol: RegistroRol = 'HUESPED',
    codigo?: string
  ): Observable<GoogleLoginResponse> {

    const datos: GoogleLoginRequest = {

      idToken: idToken,

      rol: rol,

      ...(codigo?.trim()
        ? {
          codigo: codigo.trim()
        }
        : {})

    };

    return this.http.post<GoogleLoginResponse>(
      `${this.apiUrl}/google`,
      datos
    );

  }

}
