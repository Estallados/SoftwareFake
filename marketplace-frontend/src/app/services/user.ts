
import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpHeaders
} from '@angular/common/http';
import { Observable } from 'rxjs';

// Datos del usuario devueltos por Spring Boot

export interface UsuarioDTO {
 
  idUsuario?: number;
  
  nombre?: string;
  
  apellido?: string;
  
  correo?: string;
 
  telefono?: string;
  
  fechaRegistro?: string;
  
  fechaCreacion?: string;
  
  estado?: string;
  
  estadoUsuario?: string;
 
  activo?: boolean;

  rol?: string;
}

// Respuestas de actualización

export interface MensajeRespuesta {
  
  message?: string;
  
  mensaje?: string;
  
  estado?: string;
}


@Injectable({
  providedIn: 'root'
})
export class User {

  
  private readonly http = inject(HttpClient);

  
  private readonly apiUrl =
    'http://localhost:8081/usuarios';

  // Obtener token JWT


  private obtenerHeaders(): HttpHeaders {

    const token =
      sessionStorage.getItem('token') ?? '';

    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }

  // =====================================
  // CONSULTAR MI PERFIL
  // GET /usuarios/perfil
  // =====================================

 
  obtenerMiPerfil(): Observable<UsuarioDTO> {

    return this.http.get<UsuarioDTO>(
      `${this.apiUrl}/perfil`,
      {
        headers: this.obtenerHeaders()
      }
    );
  }

  // =====================================
  // ACTUALIZAR MI PERFIL
  // PUT /usuarios/perfil
  // =====================================


  actualizarMiPerfil(
    nombre: string,
    telefono: string
  ): Observable<MensajeRespuesta> {

    return this.http.put<MensajeRespuesta>(
      `${this.apiUrl}/perfil`,
      {
        nombre,
        telefono
      },
      {
        headers: this.obtenerHeaders()
      }
    );
  }

  // =====================================
  // DESACTIVAR MI CUENTA
  // PATCH /usuarios/perfil/desactivar
  // =====================================

  desactivarMiCuenta(): Observable<MensajeRespuesta> {

    return this.http.patch<MensajeRespuesta>(
      `${this.apiUrl}/perfil/desactivar`,
      {},
      {
        headers: this.obtenerHeaders()
      }
    );
  }
}
