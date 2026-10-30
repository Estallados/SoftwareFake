
import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpHeaders
} from '@angular/common/http';
import { Observable } from 'rxjs';

import { SessionService } from './session.service';

// ======================================
// DATOS DEL ALOJAMIENTO
// ======================================

export interface DatosAlojamiento {
  titulo: string;
  descripcion?: string;
  ubicacion?: string;
  ciudad?: string;
  tipo?: string;
  capacidad?: number;
  habitaciones?: number;
  camas?: number;
  banos?: number;
  precioNoche?: number;
  horaEntrada?: string;
  horaSalida?: string;
  reglas?: string;
  idPolitica?: number;
  amenidades: string[];
}

// ======================================
// RESPUESTA DEL BACKEND
// ======================================

export interface AlojamientoRespuesta extends DatosAlojamiento {
  idAlojamiento: number;
  estado: string;
  imagenes?: string[];
  detalleImagenes?: ImagenAlojamiento[];
}

// ======================================
// IMÁGENES
// ======================================

export interface ImagenAlojamiento {
  idImagen: number;
  url: string;
  principal: boolean;
}

// ======================================
// POLÍTICAS DE CANCELACIÓN
// ======================================

export interface PoliticaCancelacion {
  idPolitica: number;
  nombre: string;
  condiciones: string;
}

// ======================================
// SERVICIO DE PUBLICACIÓN
// ======================================

@Injectable({
  providedIn: 'root'
})
export class PublicacionHostService {

  private readonly http = inject(HttpClient);

  private readonly sesion = inject(SessionService);

  private readonly apiUrl =
    'http://localhost:8081/alojamientos';

  // ======================================
  // 1. OBTENER JWT
  // ======================================

  private opciones() {

    const token = this.sesion.obtenerToken();

    return {
      headers: token
        ? new HttpHeaders({
          Authorization: `Bearer ${token}`
        })
        : new HttpHeaders()
    };
  }

  // ======================================
  // 2. CONSULTAR POLÍTICAS
  // GET /alojamientos/politicas
  // ======================================

  obtenerPoliticas(): Observable<PoliticaCancelacion[]> {

    return this.http.get<PoliticaCancelacion[]>(
      `${this.apiUrl}/politicas`
    );
  }

  // ======================================
  // 3. CREAR ALOJAMIENTO EN BORRADOR
  // POST /alojamientos
  // ======================================

  crearBorrador(
    datos: DatosAlojamiento,
    imagenes: File[]
  ): Observable<AlojamientoRespuesta> {

    const formData = new FormData();

    // JSON del alojamiento
    formData.append(
      'datos',
      JSON.stringify(datos)
    );

    // Fotografías del alojamiento
    for (const imagen of imagenes) {

      formData.append(
        'archivos',
        imagen,
        imagen.name
      );
    }

    // No establecer Content-Type manualmente.
    // Angular genera el boundary del multipart.

    return this.http.post<AlojamientoRespuesta>(
      this.apiUrl,
      formData,
      this.opciones()
    );
  }

  // ======================================
  // 4. ACTUALIZAR BORRADOR
  // PUT /alojamientos/{id}
  // ======================================

  actualizarBorrador(
    id: number,
    datos: DatosAlojamiento
  ): Observable<AlojamientoRespuesta> {

    return this.http.put<AlojamientoRespuesta>(
      `${this.apiUrl}/${id}`,
      datos,
      this.opciones()
    );
  }

  // ======================================
  // 5. AGREGAR FOTOGRAFÍAS
  // POST /alojamientos/{id}/imagenes
  // ======================================

  agregarImagenes(
    id: number,
    imagenes: File[]
  ): Observable<ImagenAlojamiento[]> {

    const formData = new FormData();

    for (const imagen of imagenes) {

      formData.append(
        'archivos',
        imagen,
        imagen.name
      );
    }

    return this.http.post<ImagenAlojamiento[]>(
      `${this.apiUrl}/${id}/imagenes`,
      formData,
      this.opciones()
    );
  }

  // ======================================
  // 6. ENVIAR A REVISIÓN
  // POST /alojamientos/{id}/enviarrevision
  // ======================================

  enviarRevision(
    id: number
  ): Observable<AlojamientoRespuesta> {

    return this.http.post<AlojamientoRespuesta>(
      `${this.apiUrl}/${id}/enviarrevision`,
      {},
      this.opciones()
    );
  }
}
