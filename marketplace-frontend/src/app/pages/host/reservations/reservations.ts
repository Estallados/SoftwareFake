
import {
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';

import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';

import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { Reserva } from '../../../models/reservaDto';
import { API_URL } from '../../../services/api-url';

// ======================================
// RESPUESTA PAGINADA DEL BACKEND
// ======================================

interface PaginaReservas<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

@Component({
  selector: 'app-host-reservations',
  standalone: true,
  templateUrl: './reservations.html'
})
export class Reservations implements OnInit, OnDestroy {

  // ======================================
  // INYECCIÓN DE DEPENDENCIAS
  // ======================================

  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);
  private readonly router = inject(Router);

  private solicitud?: Subscription;

  // ======================================
  // CONFIGURACIÓN DE LA TABLA
  // ======================================

  readonly tamanio = 10;

  readonly headers = [
    'Código',
    'Huésped',
    'Alojamiento',
    'Entrada',
    'Salida',
    'Total',
    'Estado'
  ];

  // ======================================
  // ESTADOS DE LA PÁGINA
  // ======================================

  readonly reservations = signal<Reserva[]>([]);

  readonly cargando = signal(false);
  readonly error = signal('');

  // ======================================
  // PAGINACIÓN
  // ======================================

  readonly pagina = signal(0);
  readonly totalElementos = signal(0);
  readonly totalPaginas = signal(0);

  readonly inicioPagina = computed(() => {

    if (this.totalElementos() === 0) {
      return 0;
    }

    return this.pagina() * this.tamanio + 1;
  });

  readonly finPagina = computed(() => {

    return Math.min(
      (this.pagina() + 1) * this.tamanio,
      this.totalElementos()
    );
  });

  // ======================================
  // INICIALIZACIÓN
  // ======================================

  ngOnInit(): void {
    this.cargar();
  }

  ngOnDestroy(): void {
    this.solicitud?.unsubscribe();
  }

  // ======================================
  // CONSULTAR RESERVAS DEL ANFITRIÓN
  // GET /reservas/anfitrion
  // ======================================

  cargar(): void {

    // Cancelar una consulta anterior
    this.solicitud?.unsubscribe();

    this.cargando.set(true);
    this.error.set('');

    this.solicitud = this.http
      .get<PaginaReservas<Reserva>>(
        `${this.apiUrl}/reservas/anfitrion`,
        {
          params: {
            pagina: this.pagina(),
            tamanio: this.tamanio
          }
        }
      )
      .subscribe({

        // CONSULTA EXITOSA
        next: respuesta => {

          this.reservations.set(
            respuesta.contenido ?? []
          );

          this.totalElementos.set(
            respuesta.totalElementos ?? 0
          );

          this.totalPaginas.set(
            respuesta.totalPaginas ?? 0
          );

          this.cargando.set(false);
        },

        // ERROR AL CONSULTAR
        error: (err: unknown) => {

          this.reservations.set([]);

          this.totalElementos.set(0);
          this.totalPaginas.set(0);

          this.cargando.set(false);

          this.error.set(
            this.mensajeError(err)
          );
        }
      });
  }

  // ======================================
  // ACTUALIZAR RESERVAS
  // ======================================

  actualizar(): void {
    this.cargar();
  }

  // ======================================
  // CAMBIAR DE PÁGINA
  // ======================================

  cambiarPagina(nueva: number): void {

    if (
      this.cargando() ||
      nueva < 0 ||
      nueva >= this.totalPaginas() ||
      nueva === this.pagina()
    ) {
      return;
    }

    this.pagina.set(nueva);

    this.cargar();
  }

  // ======================================
  // ABRIR DETALLE DE RESERVA
  // ======================================

  open(id: number): void {

    void this.router.navigate([
      '/host/reservas',
      id
    ]);
  }

  // ======================================
  // TRADUCIR ESTADOS DEL BACKEND
  // ======================================

  etiquetaEstado(r: Reserva): string {

    if (
      r.estado === 'CONFIRMADA' &&
      r.completada
    ) {
      return 'Finalizada';
    }

    switch (r.estado) {

      case 'CONFIRMADA':
        return 'Confirmada';

      case 'CANCELADA':
        return 'Cancelada';

      case 'EN_REVISION':
        return 'En revisión';

      case 'PENDIENTE_PAGO':
        return 'Pendiente de pago';

      default:
        return r.estado || 'Sin estado';
    }
  }

  // ======================================
  // COLORES SEGÚN EL ESTADO
  // ======================================

  badgeClass(r: Reserva): string {

    let tono = 'bg-amber-50 text-amber-800';

    if (r.estado === 'CANCELADA') {

      tono = 'bg-red-50 text-red-700';

    } else if (r.estado === 'EN_REVISION') {

      tono = 'bg-orange-50 text-orange-700';

    } else if (r.estado === 'CONFIRMADA') {

      tono = r.completada
        ? 'bg-slate-100 text-slate-600'
        : 'bg-emerald-50 text-emerald-700';
    }

    return `
      inline-flex rounded-full px-3 py-1
      text-xs font-bold ${tono}
    `;
  }

  // ======================================
  // FORMATO DE FECHAS
  // ======================================

  fecha(valor: string): string {

    const partes =
      /^(\d{4})-(\d{2})-(\d{2})$/.exec(
        valor ?? ''
      );

    if (partes) {

      return `${partes[3]}/${partes[2]}/${partes[1]}`;
    }

    return valor || '—';
  }

  // ======================================
  // FORMATO DE PRECIOS EN PESOS
  // ======================================

  moneda(valor: number): string {

    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0
    }).format(valor ?? 0);
  }

  // ======================================
  // MENSAJES DE ERROR DEL BACKEND
  // ======================================

  private mensajeError(err: unknown): string {

    if (!(err instanceof HttpErrorResponse)) {

      return 'Ocurrió un error inesperado al consultar las reservas.';
    }

    // BACKEND DESCONECTADO
    if (err.status === 0) {

      return 'No se pudo conectar con el backend. Verifica que Spring Boot esté ejecutándose.';
    }

    // SESIÓN NO VÁLIDA
    if (err.status === 401) {

      return 'Tu sesión expiró o no has iniciado sesión.';
    }

    // SIN PERMISOS
    if (err.status === 403) {

      return 'No tienes permiso para ver las reservas de un anfitrión.';
    }

    // OTROS ERRORES
    const mensaje =
      err.error?.mensaje ??
      err.error?.message;

    if (
      typeof mensaje === 'string' &&
      mensaje.trim()
    ) {
      return mensaje;
    }

    return `No se pudieron consultar las reservas (HTTP ${err.status}).`;
  }
}
