
import { Injectable, inject } from '@angular/core';

import {
  HttpClient,
  HttpHeaders,
  HttpParams
} from '@angular/common/http';

import { Observable } from 'rxjs';

import { SessionService } from './session.service';

// ======================================
// 1. ESTADOS DEL ALOJAMIENTO
// ======================================


export type EstadoAlojamiento =
  | 'BORRADOR'
  | 'PENDIENTE_REVISION'
  | 'ACTIVA'
  | 'PAUSADA'
  | 'BLOQUEADA';

// ======================================
// 2. DATOS DEL ALOJAMIENTO
// ======================================


export interface DatosAlojamiento {

  titulo: string;

  descripcion?: string;
 
  ubicacion?: string;
  
  ciudad?: string;
  
  tipo?: string;

  latitud?: number;
 
  longitud?: number;


  capacidad?: number;

  habitaciones?: number;

  camas?: number;
  
  banos?: number;

 
  precioNoche?: number;

  
  porcentajeLimpieza?: number;
  
  porcentajeServicio?: number;

  
  horaEntrada?: string;
  
  horaSalida?: string;

  
  reglas?: string;

  
  idPolitica?: number;

  
  amenidades?: string[];
}

// ======================================
// 3. IMÁGENES
// ======================================


export interface ImagenAlojamiento {

  
  idImagen: number;

  
  url: string;


  idAlojamiento?: number;

  
  principal: boolean;
}

// ======================================
// 4. SERVICIOS ADICIONALES
// ======================================


export interface ServicioAdicional {

  
  idServicio?: number;

  idAlojamiento?: number;


  nombre: string;

  
  costo: number;
}

// ======================================
// 5. RESPUESTA DEL ALOJAMIENTO
// ======================================


export interface AlojamientoRespuesta
  extends DatosAlojamiento {

 
  idAlojamiento: number;

 
  estado: EstadoAlojamiento;

  
  idAnfitrion?: number;

  
  nombreAnfitrion?: string;

  
  nombrePolitica?: string;

  
  imagenes?: string[];

 
  detalleImagenes?: ImagenAlojamiento[];

 
  serviciosAdicionales?: ServicioAdicional[];

  
  fechaCreacion?: string;

 
  fechaActualizacion?: string;

  
  promedioCalificacion?: number | null;

 
  cantidadResenas?: number;

  
  cantidadReservas?: number;
}

// ======================================
// 6. POLÍTICAS DE CANCELACIÓN
// ======================================


export interface PoliticaCancelacion {

  
  idPolitica: number;

 
  nombre: string;

 
  condiciones: string;


  diasReembolsoTotal?: number;

 
  diasReembolsoParcial?: number;

 
  porcentajeParcial?: number;
}

// ======================================
// 7. RESPUESTA PAGINADA
// ======================================


export interface PaginaAlojamientos {


  contenido: AlojamientoRespuesta[];

  
  pagina: number;

  
  tamanio: number;

  
  totalElementos: number;

  
  totalPaginas: number;
}

// ======================================
// 8. HISTORIAL DEL ALOJAMIENTO
// ======================================


export interface HistorialAlojamiento {


  idHistorial: number;


  idAlojamiento: number;


  estadoAnterior: EstadoAlojamiento | null;

 
  estadoNuevo: EstadoAlojamiento;

 
  motivo: string | null;

  
  idUsuario: number;

 
  fecha: string;
}

// ======================================
// 9. PERÍODO DE DISPONIBILIDAD
// ======================================


export interface PeriodoDisponibilidad {

 
  idPeriodo: number;

  idAlojamiento: number;


  fechaInicio: string;

  
  fechaFin: string;
}

// Datos necesarios para crear o actualizar
// un período en Spring Boot.


export interface DatosPeriodoDisponibilidad {

 
  fechaInicio: string;

  
  fechaFin: string;
}

// ======================================
// 10. CALENDARIO DE DISPONIBILIDAD
// ======================================


export interface RangoReservado {

  fechaEntrada: string;

  fechaSalida: string;
}


export interface CalendarioDisponibilidad {


  idAlojamiento: number;

 
  estadoAlojamiento: EstadoAlojamiento;

 
  periodos: PeriodoDisponibilidad[];

 
  reservados: RangoReservado[];
}

// ======================================
// 11. SERVICIO DE PUBLICACIÓN
// ======================================


@Injectable({
  providedIn: 'root'
})
export class PublicacionHostService {

  // ====================================
  // SERVICIOS
  // ====================================

 
  private readonly http = inject(HttpClient);

 
  private readonly sesion = inject(SessionService);

  // ====================================
  // URL DEL BACKEND SPRING BOOT
  // ====================================

  
  private readonly apiUrl =
    'http://localhost:8081/alojamientos';

  // ====================================
  // 1. OBTENER JWT
  // ====================================

 
  private opciones(): {
    headers: HttpHeaders;
  } {

    const token = this.sesion.obtenerToken();

    return {

      headers: token
        ? new HttpHeaders({
          Authorization: `Bearer ${token}`
        })
        : new HttpHeaders()

    };
  }

  // ====================================
  // 2. CONSULTAR POLÍTICAS
  // GET /alojamientos/politicas
  // ====================================

 
  obtenerPoliticas(): Observable<PoliticaCancelacion[]> {

    return this.http.get<PoliticaCancelacion[]>(
      `${this.apiUrl}/politicas`
    );
  }

  // ====================================
  // 3. CREAR BORRADOR
  // POST /alojamientos
  // ====================================


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

    // Fotografías
    for (const imagen of imagenes) {

      formData.append(
        'archivos',
        imagen,
        imagen.name
      );
    }

    // No establecer Content-Type manualmente.
    // El navegador genera el multipart boundary.

    return this.http.post<AlojamientoRespuesta>(
      this.apiUrl,
      formData,
      this.opciones()
    );
  }

  // ====================================
  // 4. ACTUALIZAR ALOJAMIENTO
  // PUT /alojamientos/{id}
  // ====================================

  
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

  // ====================================
  // 5. AGREGAR FOTOGRAFÍAS
  // POST /alojamientos/{id}/imagenes
  // ====================================

 
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

  // ====================================
  // 6. ENVIAR A REVISIÓN
  // POST /alojamientos/{id}/enviarrevision
  // ====================================

  enviarRevision(
    id: number
  ): Observable<AlojamientoRespuesta> {

    return this.http.post<AlojamientoRespuesta>(
      `${this.apiUrl}/${id}/enviarrevision`,
      {},
      this.opciones()
    );
  }

  // ====================================
  // 7. CONSULTAR MIS ALOJAMIENTOS
  // GET /alojamientos/mios
  // ====================================

 
  obtenerMisAlojamientos(
    pagina: number = 0,
    tamanio: number = 100
  ): Observable<PaginaAlojamientos> {

    const params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanio', tamanio.toString());

    return this.http.get<PaginaAlojamientos>(
      `${this.apiUrl}/mios`,
      {
        ...this.opciones(),
        params
      }
    );
  }

  // ====================================
  // 8. OBTENER DETALLE
  // GET /alojamientos/{id}
  // ====================================

 
  obtenerAlojamiento(
    id: number
  ): Observable<AlojamientoRespuesta> {

    return this.http.get<AlojamientoRespuesta>(
      `${this.apiUrl}/${id}`,
      this.opciones()
    );
  }

  // ====================================
  // 9. OBTENER HISTORIAL
  // GET /alojamientos/{id}/historial
  // ====================================

  
  obtenerHistorial(
    id: number
  ): Observable<HistorialAlojamiento[]> {

    return this.http.get<HistorialAlojamiento[]>(
      `${this.apiUrl}/${id}/historial`,
      this.opciones()
    );
  }

  // ====================================
  // 10. PAUSAR ALOJAMIENTO
  // POST /alojamientos/{id}/pausar
  // ====================================

  pausarAlojamiento(
    id: number
  ): Observable<AlojamientoRespuesta> {

    return this.http.post<AlojamientoRespuesta>(
      `${this.apiUrl}/${id}/pausar`,
      {},
      this.opciones()
    );
  }

  // ====================================
  // 11. REANUDAR ALOJAMIENTO
  // POST /alojamientos/{id}/reanudar
  // ====================================

  reanudarAlojamiento(
    id: number
  ): Observable<AlojamientoRespuesta> {

    return this.http.post<AlojamientoRespuesta>(
      `${this.apiUrl}/${id}/reanudar`,
      {},
      this.opciones()
    );
  }

  // ====================================
  // 12. ELIMINAR FOTOGRAFÍA
  // DELETE /alojamientos/{id}/imagenes/{idImagen}
  // ====================================

  
  eliminarImagen(
    idAlojamiento: number,
    idImagen: number
  ): Observable<ImagenAlojamiento[]> {

    return this.http.delete<ImagenAlojamiento[]>(
      `${this.apiUrl}/${idAlojamiento}/imagenes/${idImagen}`,
      this.opciones()
    );
  }

  // ====================================
  // 13. MARCAR FOTO PRINCIPAL
  // PATCH /alojamientos/{id}/imagenes/{idImagen}/principal
  // ====================================

 
  marcarImagenPrincipal(
    idAlojamiento: number,
    idImagen: number
  ): Observable<ImagenAlojamiento[]> {

    return this.http.patch<ImagenAlojamiento[]>(
      `${this.apiUrl}/${idAlojamiento}/imagenes/${idImagen}/principal`,
      {},
      this.opciones()
    );
  }

  // ====================================
  // 14. AGREGAR FECHAS DE DISPONIBILIDAD
  // POST /alojamientos/{id}/disponibilidad
  // ====================================

 
  agregarPeriodoDisponibilidad(
    idAlojamiento: number,
    periodo: DatosPeriodoDisponibilidad
  ): Observable<PeriodoDisponibilidad> {

    return this.http.post<PeriodoDisponibilidad>(
      `${this.apiUrl}/${idAlojamiento}/disponibilidad`,
      periodo,
      this.opciones()
    );
  }

  // ====================================
  // 15. CONSULTAR DISPONIBILIDAD
  // GET /alojamientos/{id}/disponibilidad
  // ====================================

 
  obtenerDisponibilidad(
    idAlojamiento: number
  ): Observable<CalendarioDisponibilidad> {

    return this.http.get<CalendarioDisponibilidad>(
      `${this.apiUrl}/${idAlojamiento}/disponibilidad`,
      this.opciones()
    );
  }

  // ====================================
  // 16. ACTUALIZAR PERÍODO
  // PUT /alojamientos/{id}/disponibilidad/{idPeriodo}
  // ====================================

  
  actualizarPeriodoDisponibilidad(
    idAlojamiento: number,
    idPeriodo: number,
    periodo: DatosPeriodoDisponibilidad
  ): Observable<PeriodoDisponibilidad> {

    return this.http.put<PeriodoDisponibilidad>(
      `${this.apiUrl}/${idAlojamiento}/disponibilidad/${idPeriodo}`,
      periodo,
      this.opciones()
    );
  }

  // ====================================
  // 17. ELIMINAR PERÍODO
  // DELETE /alojamientos/{id}/disponibilidad/{idPeriodo}
  // ====================================

  
  eliminarPeriodoDisponibilidad(
    idAlojamiento: number,
    idPeriodo: number
  ): Observable<{ message: string }> {

    return this.http.delete<{ message: string }>(
      `${this.apiUrl}/${idAlojamiento}/disponibilidad/${idPeriodo}`,
      this.opciones()
    );
  }
}
