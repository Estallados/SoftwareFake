
import {
  Component,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';

import {
  HttpErrorResponse
} from '@angular/common/http';

import { Router } from '@angular/router';

import {
  EMPTY,
  expand,
  finalize,
  reduce,
  timeout
} from 'rxjs';

import {
  AlojamientoRespuesta,
  PublicacionHostService,
  EstadoAlojamiento
} from '../../../services/publicacion-host.service';

// ======================================
// TIPOS
// ======================================

export type FiltroAlojamiento =
  | 'TODOS'
  | EstadoAlojamiento;

@Component({
  selector: 'app-host-listings',
  standalone: true,
  templateUrl: './listings.html'
})
export class Listings implements OnInit {

  // ======================================
  // SERVICIOS
  // ======================================

  private readonly router = inject(Router);

  private readonly publicaciones =
    inject(PublicacionHostService);

  // ======================================
  // ALOJAMIENTOS REALES
  // ======================================

  readonly listings = signal<AlojamientoRespuesta[]>([]);

  readonly cargando = signal(false);

  readonly error = signal('');

  readonly filtro = signal<FiltroAlojamiento>('TODOS');

  // ======================================
  // OPCIONES DE FILTRADO
  // ======================================

  readonly filtros: {
    valor: FiltroAlojamiento;
    nombre: string;
  }[] = [

    {
      valor: 'TODOS',
      nombre: 'Todos los alojamientos'
    },

    {
      valor: 'BORRADOR',
      nombre: 'Borradores'
    },

    {
      valor: 'PENDIENTE_REVISION',
      nombre: 'Pendientes de revisión'
    },

    {
      valor: 'ACTIVA',
      nombre: 'Activos'
    },

    {
      valor: 'PAUSADA',
      nombre: 'Pausados'
    },

    {
      valor: 'BLOQUEADA',
      nombre: 'Bloqueados'
    }
  ];

  // ======================================
  // MÉTRICAS
  // ======================================

  readonly total = computed(() =>
    this.listings().length
  );

  readonly active = computed(() =>
    this.contar('ACTIVA')
  );

  readonly paused = computed(() =>
    this.contar('PAUSADA')
  );

  readonly blocked = computed(() =>
    this.contar('BLOQUEADA')
  );

  readonly pending = computed(() =>
    this.contar('PENDIENTE_REVISION')
  );

  readonly drafts = computed(() =>
    this.contar('BORRADOR')
  );

  private contar(
    estado: EstadoAlojamiento
  ): number {

    return this.listings().filter(
      alojamiento => alojamiento.estado === estado
    ).length;
  }

  // ======================================
  // FILTRAR LOS ALOJAMIENTOS
  // ======================================

  readonly alojamientosFiltrados = computed(() => {

    const estadoSeleccionado = this.filtro();

    if (estadoSeleccionado === 'TODOS') {
      return this.listings();
    }

    return this.listings().filter(
      alojamiento =>
        alojamiento.estado === estadoSeleccionado
    );
  });

  cambiarFiltro(evento: Event): void {

    const select =
      evento.target as HTMLSelectElement;

    this.filtro.set(
      select.value as FiltroAlojamiento
    );
  }

  // ======================================
  // INICIALIZAR
  // ======================================

  ngOnInit(): void {
    this.cargarAlojamientos();
  }

  // ======================================
  // OBTENER ALOJAMIENTOS DEL BACKEND
  // ======================================

  cargarAlojamientos(): void {

    if (this.cargando()) {
      return;
    }

    this.cargando.set(true);
    this.error.set('');

    // El backend devuelve resultados paginados.
    // Obtenemos todas las páginas para calcular
    // correctamente los contadores y filtros.

    this.publicaciones
      .obtenerMisAlojamientos(0, 100)
      .pipe(

        timeout(15000),

        expand(respuesta => {

          const siguiente =
            respuesta.pagina + 1;

          if (
            siguiente >= respuesta.totalPaginas
          ) {
            return EMPTY;
          }

          return this.publicaciones
            .obtenerMisAlojamientos(
              siguiente,
              100
            )
            .pipe(timeout(15000));
        }),

        reduce(
          (acumulado, respuesta) => [
            ...acumulado,
            ...respuesta.contenido
          ],
          [] as AlojamientoRespuesta[]
        ),

        finalize(() => {
          this.cargando.set(false);
        })
      )
      .subscribe({

        next: (alojamientos) => {

          this.listings.set(alojamientos);
        },

        error: (err: unknown) => {

          this.error.set(
            this.obtenerMensajeError(err)
          );
        }
      });
  }

  // ======================================
  // MOSTRAR ESTADOS
  // ======================================

  nombreEstado(estado: string): string {

    switch (estado) {

      case 'BORRADOR':
        return 'Borrador';

      case 'PENDIENTE_REVISION':
        return 'Pendiente de revisión';

      case 'ACTIVA':
        return 'Activo';

      case 'PAUSADA':
        return 'Pausado';

      case 'BLOQUEADA':
        return 'Bloqueado';

      default:
        return estado;
    }
  }

  // ======================================
  // COLORES DE LOS ESTADOS
  // ======================================

  badgeClass(
    l: AlojamientoRespuesta
  ): string {

    const base =
      'inline-flex rounded-full px-3 py-1 ' +
      'text-xs font-bold ';

    switch (l.estado) {

      case 'ACTIVA':

        return base +
          'bg-emerald-50 text-emerald-700';

      case 'PENDIENTE_REVISION':

        return base +
          'bg-amber-50 text-amber-700';

      case 'BORRADOR':

        return base +
          'bg-slate-100 text-slate-700';

      case 'PAUSADA':

        return base +
          'bg-blue-50 text-blue-700';

      case 'BLOQUEADA':

        return base +
          'bg-red-50 text-red-700';

      default:

        return base +
          'bg-slate-100 text-slate-600';
    }
  }

  // ======================================
  // PRECIO EN PESOS COLOMBIANOS
  // ======================================

  formatearPrecio(
    precio?: number
  ): string {

    if (
      precio === null ||
      precio === undefined
    ) {
      return 'Sin definir';
    }

    return new Intl.NumberFormat(
      'es-CO',
      {
        style: 'currency',
        currency: 'COP',
        maximumFractionDigits: 0
      }
    ).format(precio);
  }

  // ======================================
  // FECHAS
  // ======================================

  formatearFecha(
    valor?: string
  ): string {

    if (!valor) {
      return 'Sin información';
    }

    // LocalDateTime de Java no lleva zona horaria.
    const fecha = new Date(valor);

    if (Number.isNaN(fecha.getTime())) {
      return valor;
    }

    return fecha.toLocaleDateString(
      'es-CO',
      {
        day: '2-digit',
        month: 'long',
        year: 'numeric'
      }
    );
  }

  // ======================================
  // IMAGEN PRINCIPAL
  // ======================================

  obtenerImagen(
    alojamiento: AlojamientoRespuesta
  ): string | null {

    const imagen =
      alojamiento.imagenes?.[0] ??
      alojamiento.detalleImagenes?.[0]?.url;

    if (!imagen) {
      return null;
    }

    // URL absoluta enviada por Spring Boot.
    if (
      /^https?:\/\//i.test(imagen)
    ) {
      return imagen;
    }

    // Ruta relativa a la raíz del servidor.
    if (imagen.startsWith('/')) {
      return `http://localhost:8081${imagen}`;
    }

    // Ruta relativa sin barra inicial.
    return `http://localhost:8081/${imagen}`;
  }

  // ======================================
  // CALIFICACIÓN
  // ======================================

  mostrarCalificacion(
    alojamiento: AlojamientoRespuesta
  ): string {

    const promedio =
      alojamiento.promedioCalificacion;

    if (
      promedio === null ||
      promedio === undefined
    ) {
      return 'Sin calificaciones';
    }

    return `★ ${promedio.toFixed(1)}`;
  }

  // ======================================
  // NAVEGACIÓN
  // ======================================

  create(): void {

    void this.router.navigateByUrl(
      '/host/alojamientos/crear'
    );
  }

  details(id: number): void {

    void this.router.navigate([
      '/host/alojamientos',
      id
    ]);
  }

  edit(id: number): void {

    void this.router.navigate([
      '/host/alojamientos',
      id,
      'editar'
    ]);
  }

  availability(id: number): void {

    void this.router.navigate([
      '/host/alojamientos',
      id,
      'disponibilidad'
    ]);
  }

  // ======================================
  // ERRORES DE SPRING BOOT
  // ======================================

  private obtenerMensajeError(
    error: unknown
  ): string {

    if (
      error instanceof Error &&
      error.name === 'TimeoutError'
    ) {
      return 'El servidor tardó demasiado en responder.';
    }

    if (
      error instanceof HttpErrorResponse
    ) {

      if (error.status === 0) {

        return (
          'No se pudo conectar con Spring Boot ' +
          'en http://localhost:8081.'
        );
      }

      if (error.status === 401) {

        return (
          'Tu sesión ha expirado. ' +
          'Inicia sesión nuevamente.'
        );
      }

      if (error.status === 403) {

        return (
          'No tienes permiso para consultar ' +
          'los alojamientos de este anfitrión.'
        );
      }

      const respuesta: unknown =
        error.error;

      if (
        respuesta !== null &&
        typeof respuesta === 'object' &&
        'message' in respuesta
      ) {

        const mensaje =
          (respuesta as {
            message?: unknown
          }).message;

        if (
          typeof mensaje === 'string'
        ) {
          return mensaje;
        }
      }
    }

    return 'No se pudieron cargar tus alojamientos.';
  }
}
