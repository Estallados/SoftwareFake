
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import {
  AlojamientoRespuesta,
  EstadoAlojamiento,
  PublicacionHostService
} from '../../../services/publicacion-host.service';

import { API_URL } from '../../../services/api-url';

@Component({
  selector: 'app-host-details',
  standalone: true,
  templateUrl: './details.html'
})
export class Details implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly publicaciones = inject(PublicacionHostService);
  private readonly apiUrl = inject(API_URL);

  readonly id = Number(this.route.snapshot.paramMap.get('id'));

  readonly alojamiento = signal<AlojamientoRespuesta | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');

  // ======================================
  // IMÁGENES DEL ALOJAMIENTO
  // ======================================

  readonly imagenes = computed(() => {
    const a = this.alojamiento();

    if (!a) return [] as string[];

    const imagenes = a.detalleImagenes?.length
      ? [...a.detalleImagenes]
        .sort(
          (primera, segunda) =>
            Number(segunda.principal) - Number(primera.principal)
        )
        .map(imagen => imagen.url)
      : (a.imagenes ?? []);

    return [...new Set(imagenes.filter(url => !!url))]
      .map(url => this.normalizarImagen(url));
  });

  readonly imagenPrincipal = computed(
    () => this.imagenes()[0] ?? null
  );

  readonly otrasImagenes = computed(
    () => this.imagenes().slice(1, 5)
  );

  // ======================================
  // CARGAR DATOS DESDE SPRING BOOT
  // ======================================

  ngOnInit(): void {
    this.cargarAlojamiento();
  }

  cargarAlojamiento(): void {

    if (!Number.isInteger(this.id) || this.id <= 0) {
      this.error.set('El identificador del alojamiento no es válido.');
      this.cargando.set(false);
      return;
    }

    this.error.set('');
    this.cargando.set(true);

    this.publicaciones.obtenerAlojamiento(this.id)
      .pipe(
        timeout(15000),
        finalize(() => this.cargando.set(false))
      )
      .subscribe({
        next: alojamiento => {
          this.alojamiento.set(alojamiento);
        },
        error: err => {
          this.error.set(this.mensajeError(err));
        }
      });
  }

  private normalizarImagen(url: string): string {

    if (/^https?:\/\//i.test(url)) {
      return url;
    }

    return `${this.apiUrl}${url.startsWith('/') ? '' : '/'}${url}`;
  }

  // ======================================
  // ESTADOS
  // ======================================

  nombreEstado(estado: EstadoAlojamiento): string {

    const nombres: Record<EstadoAlojamiento, string> = {
      BORRADOR: 'Borrador',
      PENDIENTE_REVISION: 'Pendiente de revisión',
      ACTIVA: 'Activo',
      PAUSADA: 'Pausado',
      BLOQUEADA: 'Bloqueado'
    };

    return nombres[estado];
  }

  claseEstado(estado: EstadoAlojamiento): string {

    const clases: Record<EstadoAlojamiento, string> = {
      BORRADOR: 'bg-slate-100 text-slate-700',
      PENDIENTE_REVISION: 'bg-amber-50 text-amber-800',
      ACTIVA: 'bg-emerald-50 text-emerald-700',
      PAUSADA: 'bg-blue-50 text-blue-700',
      BLOQUEADA: 'bg-red-50 text-red-700'
    };

    return `rounded-full px-3 py-1 text-xs font-bold ${clases[estado]}`;
  }

  // ======================================
  // FORMATO DE PRECIO Y FECHAS
  // ======================================

  precio(valor?: number): string {

    if (valor == null) return 'Sin definir';

    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0
    }).format(valor);
  }

  fecha(valor?: string): string {

    if (!valor) return 'No registrada';

    const fecha = new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
      return valor;
    }

    return fecha.toLocaleDateString('es-CO', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  // ======================================
  // NAVEGACIÓN
  // ======================================

  volver(): void {
    void this.router.navigateByUrl('/host/alojamientos');
  }

  editar(): void {
    void this.router.navigate([
      '/host/alojamientos',
      this.id,
      'editar'
    ]);
  }

  disponibilidad(): void {
    void this.router.navigate([
      '/host/alojamientos',
      this.id,
      'disponibilidad'
    ]);
  }

  // ======================================
  // MANEJO DE ERRORES
  // ======================================

  private mensajeError(err: unknown): string {

    if (err instanceof Error && err.name === 'TimeoutError') {
      return 'El servidor tardó demasiado en responder.';
    }

    if (err instanceof HttpErrorResponse) {

      if (err.status === 0) {
        return 'No se pudo conectar con el backend en localhost:8081.';
      }

      if (err.status === 401) {
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      }

      if (err.status === 403) {
        return 'No tienes permisos para consultar este alojamiento.';
      }

      if (err.status === 404) {
        return 'No se encontró el alojamiento solicitado.';
      }

      if (typeof err.error?.message === 'string') {
        return err.error.message;
      }

      return `Error HTTP ${err.status} al consultar el alojamiento.`;
    }

    return 'No fue posible cargar los detalles del alojamiento.';
  }
}

