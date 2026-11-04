
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from './api-url';

// ======================================
// ROLES PERMITIDOS EN REGISTRO
// ======================================

export type RegistroRol = 'HUESPED' | 'ANFITRION';

// ======================================
// DATOS PARA CREAR USUARIO
// ======================================

export interface RegistroRequest {
  nombre: string;
  correo: string;
  telefono: string;
  rol: RegistroRol;
  contrasenia: string;
}

// ======================================
// RESPUESTA DEL REGISTRO
// ======================================

export interface RegistroResponse {
  message: string;
  correo: string;
  requiereVerificacion: boolean;
}

// ======================================
// RESPUESTA DE VERIFICACIÓN
// ======================================

export interface VerificacionResponse {
  message: string;
  token: string;
  rol: string;
  correo: string;
}

// ======================================
// RESPUESTA DE REENVÍO DE CÓDIGO
// ======================================

export interface ReenvioCodigoResponse {
  message: string;
}

// ======================================
// DATOS PARA INICIAR SESIÓN
// ======================================

export interface LoginRequest {
  correo: string;
  contrasenia: string;
  codigo?: string;
}

// ======================================
// RESPUESTA DEL LOGIN
// ======================================

export interface LoginResponse {
  token: string;
  rol: string;
  correo: string;
  message?: string;
  cuentaNueva?: boolean;
}

// ======================================
// RECUPERAR CONTRASEÑA
// ======================================

export interface RecuperarContrasenaRequest {
  correo: string;
}

export interface RecuperarContrasenaResponse {
  message: string;
}

// ======================================
// RESTABLECER CONTRASEÑA
// ======================================

export interface RestablecerContrasenaRequest {
  token: string;
  contrasenia: string;
}

export interface RestablecerContrasenaResponse {
  message: string;
}

// ======================================
// CONFIGURACIÓN DE GOOGLE
// ======================================

export interface GoogleConfiguracionResponse {
  habilitado: boolean;
  clientId: string | null;
}

// ======================================
// SOLICITUD DE LOGIN CON GOOGLE
// ======================================

export interface GoogleLoginRequest {
  idToken: string;
  rol: RegistroRol;
  codigo?: string;
}

// ======================================
// RESPUESTA DEL LOGIN CON GOOGLE
// ======================================

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

  // URL BASE DEL BACKEND
  // Por defecto: http://localhost:8081

  private readonly apiUrl =
    `${inject(API_URL)}/auth`;

  // ======================================
  // 1. REGISTRAR USUARIO
  // POST /auth/register
  // ======================================

  registrar(datos: RegistroRequest) {

    return this.http.post<RegistroResponse>(
      `${this.apiUrl}/register`,
      datos
    );

  }

  // ======================================
  // 2. VERIFICAR CORREO ELECTRÓNICO
  // POST /auth/verificar
  // ======================================

  verificarCorreo(
    correo: string,
    codigo: string
  ) {

    return this.http.post<VerificacionResponse>(
      `${this.apiUrl}/verificar`,
      {
        correo: correo.trim(),
        codigo: codigo.trim()
      }
    );

  }

  // ======================================
  // 3. REENVIAR CÓDIGO DE VERIFICACIÓN
  // POST /auth/reenviarcodigo
  // ======================================

  reenviarCodigo(correo: string) {

    return this.http.post<ReenvioCodigoResponse>(
      `${this.apiUrl}/reenviarcodigo`,
      {
        correo: correo.trim()
      }
    );

  }

  // ======================================
  // 4. INICIAR SESIÓN NORMAL
  // POST /auth/login
  // ======================================

  login(datos: LoginRequest) {

    return this.http.post<LoginResponse>(
      `${this.apiUrl}/login`,
      {
        correo: datos.correo.trim(),
        contrasenia: datos.contrasenia,

        ...(datos.codigo
          ? { codigo: datos.codigo.trim() }
          : {})
      }
    );

  }

  // ======================================
  // 5. RECUPERAR CONTRASEÑA
  // POST /auth/recuperar
  // ======================================

  recuperarContrasena(correo: string) {

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
  ) {

    const datos: RestablecerContrasenaRequest = {
      token: token.trim(),
      contrasenia
    };

    return this.http.post<RestablecerContrasenaResponse>(
      `${this.apiUrl}/restablecer`,
      datos
    );

  }

  // ======================================
  // 7. OBTENER CONFIGURACIÓN DE GOOGLE
  // GET /auth/google/configuracion
  // ======================================

  configuracionGoogle() {

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
  ) {

    const datos: GoogleLoginRequest = {

      idToken,

      rol,

      ...(codigo
        ? { codigo: codigo.trim() }
        : {})

    };

    return this.http.post<GoogleLoginResponse>(
      `${this.apiUrl}/google`,
      datos
    );

  }

}
