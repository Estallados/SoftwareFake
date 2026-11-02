
import {
  Component,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';

import { HttpErrorResponse } from '@angular/common/http';
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
  EstadoAlojamiento,
  PublicacionHostService
} from '../../../services/publicacion-host.service';

export type FiltroAlojamiento =
  | 'TODOS'
  | EstadoAlojamiento;

@Component({
  selector: 'app-host-listings',
  standalone: true,
  templateUrl: './listings.html'
})
export class Listings implements OnInit {

  private readonly router = inject(Router);
  private readonly publicaciones = inject(PublicacionHostService);

  // ======================================
  // ESTADOS DEL COMPONENTE
  // ======================================

  readonly listings = signal<AlojamientoRespuesta[]>([]);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly filtro = signal<FiltroAlojamiento>('TODOS');

  // ======================================
  // FILTROS
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
      valor: 'ACTIVA',
      nombre: 'Disponibles'
    },
    {
      valor: 'PENDIENTE_REVISION',
      nombre: 'En revisión'
    },
    {
      valor: 'BORRADOR',
      nombre: 'Sin publicar'
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
  // CONTADORES
  // ======================================

  readonly total = computed(() => this.listings().length);

  readonly active = computed(() => this.contar('ACTIVA'));

  readonly pending = computed(() =>
    this.contar('PENDIENTE_REVISION')
  );

  readonly drafts = computed(() => this.contar('BORRADOR'));

  readonly paused = computed(() => this.contar('PAUSADA'));

  readonly blocked = computed(() => this.contar('BLOQUEADA'));

  private contar(estado: EstadoAlojamiento): number {
    return this.listings().filter(
      alojamiento => alojamiento.estado === estado
    ).length;
  }

  // ======================================
  // FILTRAR ALOJAMIENTOS
  // ======================================

  readonly alojamientosFiltrados = computed(() => {

    const estadoSeleccionado = this.filtro();

    if (estadoSeleccionado === 'TODOS') {
      return this.listings();
    }

    return this.listings().filter(
      alojamiento => alojamiento.estado === estadoSeleccionado
    );
  });

  cambiarFiltro(evento: Event): void {

    const select = evento.target as HTMLSelectElement;

    this.filtro.set(select.value as FiltroAlojamiento);
  }

  // ======================================
  // INICIALIZACIÓN
  // ======================================

  ngOnInit(): void {
    this.cargarAlojamientos();
  }

  // ======================================
  // OBTENER ALOJAMIENTOS DESDE SPRING BOOT
  // ======================================

  cargarAlojamientos(): void {

    if (this.cargando()) return;

    this.cargando.set(true);
    this.error.set('');

    this.publicaciones
      .obtenerMisAlojamientos(0, 100)
      .pipe(
        timeout(15000),

        expand(respuesta => {

          const siguiente = respuesta.pagina + 1;

          if (siguiente >= respuesta.totalPaginas) {
            return EMPTY;
          }

          return this.publicaciones
            .obtenerMisAlojamientos(siguiente, 100)
            .pipe(timeout(15000));
        }),

        reduce(
          (acumulado, respuesta) => [
            ...acumulado,
            ...respuesta.contenido
          ],
          [] as AlojamientoRespuesta[]
        ),

        finalize(() => this.cargando.set(false))
      )
      .subscribe({
        next: alojamientos => {
          this.listings.set(alojamientos);
        },
        error: err => {
          this.error.set(this.obtenerMensajeError(err));
        }
      });
  }

  // ======================================
  // ETIQUETAS DE ESTADO
  // ======================================

  nombreEstado(estado: string): string {

    const nombres: Record<string, string> = {
      BORRADOR: 'Borrador',
      PENDIENTE_REVISION: 'Pendiente de revisión',
      ACTIVA: 'Disponible',
      PAUSADA: 'Pausado',
      BLOQUEADA: 'Bloqueado'
    };

    return nombres[estado] ?? estado;
  }

  // ======================================
  // COLORES DE ESTADO
  // ======================================

  badgeClass(l: AlojamientoRespuesta): string {

    const base =
      'inline-flex rounded-full px-3 py-1 text-xs font-bold ';

    switch (l.estado) {

      case 'ACTIVA':
        return base + 'bg-emerald-50 text-emerald-700';

      case 'PENDIENTE_REVISION':
        return base + 'bg-amber-50 text-amber-700';

      case 'BORRADOR':
        return base + 'bg-slate-100 text-slate-700';

      case 'PAUSADA':
        return base + 'bg-blue-50 text-blue-700';

      case 'BLOQUEADA':
        return base + 'bg-red-50 text-red-700';

      default:
        return base + 'bg-slate-100 text-slate-600';
    }
  }

  // ======================================
  // FORMATEAR PRECIO
  // ======================================

  formatearPrecio(precio?: number): string {

    if (precio === null || precio === undefined) {
      return 'Sin definir';
    }

    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0
    }).format(precio);
  }

  // ======================================
  // FORMATEAR FECHA
  // ======================================

  formatearFecha(valor?: string): string {

    if (!valor) return 'Sin información';

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
  // IMAGEN PRINCIPAL
  // ======================================

  obtenerImagen(alojamiento: AlojamientoRespuesta): string | null {

    const imagenPrincipal =
      alojamiento.detalleImagenes?.find(
        imagen => imagen.principal
      )?.url;

    const imagen =
      imagenPrincipal ??
      alojamiento.imagenes?.[0] ??
      alojamiento.detalleImagenes?.[0]?.url;

    if (!imagen) return null;

    // Imágenes de Cloudinary o URL absoluta.
    if (/^https?:\/\//i.test(imagen)) {
      return imagen;
    }

    // Imágenes relativas servidas por Spring Boot.
    if (imagen.startsWith('/')) {
      return `http://localhost:8081${imagen}`;
    }

    return `http://localhost:8081/${imagen}`;
  }

  // ======================================
  // CALIFICACIÓN
  // ======================================

  mostrarCalificacion(alojamiento: AlojamientoRespuesta): string {

    const promedio = alojamiento.promedioCalificacion;

    if (promedio === null || promedio === undefined) {
      return 'Sin calificaciones';
    }

    return promedio.toFixed(1);
  }

  // ======================================
  // NAVEGACIÓN
  // ======================================

  create(): void {
    void this.router.navigateByUrl('/host/alojamientos/crear');
  }

  details(id: number): void {
    void this.router.navigate([
      '/host/alojamientos',
      id
    ]);
  }

  // BOTÓN EDITAR -> NUEVO COMPONENTE UPDATE
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
  // ERRORES
  // ======================================

  private obtenerMensajeError(err: unknown): string {

    if (err instanceof Error && err.name === 'TimeoutError') {
      return 'El servidor tardó demasiado en responder.';
    }

    if (err instanceof HttpErrorResponse) {

      if (err.status === 0) {
        return 'No se pudo conectar con Spring Boot en localhost:8081.';
      }

      if (err.status === 401) {
        return 'Tu sesión ha expirado. Inicia sesión nuevamente.';
      }

      if (err.status === 403) {
        return 'No tienes permisos para consultar los alojamientos.';
      }

      if (typeof err.error?.message === 'string') {
        return err.error.message;
      }

      return `Error HTTP ${err.status} al consultar alojamientos.`;
    }

    return 'No se pudieron cargar tus alojamientos.';
  }
}
