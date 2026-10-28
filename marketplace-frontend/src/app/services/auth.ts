
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

// ROLES PERMITIDOS EN REGISTRO
export type RegistroRol = 'HUESPED' | 'ANFITRION';

// DATOS PARA CREAR USUARIO
export interface RegistroRequest {
  nombre: string;
  correo: string;
  telefono: string;
  rol: RegistroRol;
  contrasenia: string;
}

// RESPUESTA DEL REGISTRO
export interface RegistroResponse {
  message: string;
  correo: string;
  requiereVerificacion: boolean;
}

// RESPUESTA DE VERIFICACIÓN
export interface VerificacionResponse {
  message: string;
  token: string;
  rol: string;
  correo: string;
}

// RESPUESTA DE REENVÍO DE CÓDIGO
export interface ReenvioCodigoResponse {
  message: string;
}

// DATOS DE LOGIN
export interface LoginRequest {
  correo: string;
  contrasenia: string;
  codigo?: string;
}

// RESPUESTA DE LOGIN
export interface LoginResponse {
  token: string;
  rol: string;
  correo: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class Auth {

  private readonly http = inject(HttpClient);

  // URL DEL BACKEND SPRING BOOT
  private readonly apiUrl = 'http://localhost:8081/auth';

  // ======================================
  // 1. REGISTRAR USUARIO
  // ======================================
  registrar(datos: RegistroRequest) {
    return this.http.post<RegistroResponse>(
      `${this.apiUrl}/register`,
      datos
    );
  }

  // ======================================
  // 2. VERIFICAR CORREO ELECTRÓNICO
  // ======================================
  verificarCorreo(correo: string, codigo: string) {
    return this.http.post<VerificacionResponse>(
      `${this.apiUrl}/verificar`,
      { correo, codigo }
    );
  }

  // ======================================
  // 3. REENVIAR CÓDIGO DE VERIFICACIÓN
  // ======================================
  reenviarCodigo(correo: string) {
    return this.http.post<ReenvioCodigoResponse>(
      `${this.apiUrl}/reenviarcodigo`,
      { correo }
    );
  }

  // ======================================
  // 4. INICIAR SESIÓN
  // ======================================
  login(datos: LoginRequest) {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/login`,
      datos
    );
  }

}
