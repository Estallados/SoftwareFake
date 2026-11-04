
import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';

import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';

import {
  EMPTY,
  Observable,
  Subscription,
  expand,
  reduce,
  timeout
} from 'rxjs';

import { API_URL } from '../../../services/api-url';

import {
  AlojamientoRespuesta,
  PaginaAlojamientos,
  PublicacionHostService
} from '../../../services/publicacion-host.service';

import { Resena } from '../../../models/resenaDto';

// ======================================
// RESPUESTAS DEL BACKEND
// ======================================

interface PaginaResenas {
  contenido: Resena[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

interface ResumenResenasAnfitrion {
  idAlojamiento: number | null;
  promedio: number | null;
  cantidad: number;
  resenas: PaginaResenas;
}

// ======================================
// COMPONENTE RESEÑAS DEL ANFITRIÓN
// ======================================

@Component({
  selector: 'app-host-reviews',
  standalone: true,
  templateUrl: './reviews.html'
})
export class Reviews implements OnInit, OnDestroy {

  // ======================================
  // SERVICIOS
  // ======================================

  private readonly http = inject(HttpClient);

  private readonly apiUrl = inject(API_URL);

  private readonly publicaciones =
    inject(PublicacionHostService);

  private solicitudResenas?: Subscription;
  private solicitudAlojamientos?: Subscription;

  // ======================================
  // CONFIGURACIÓN
  // ======================================

  readonly starList = [1, 2, 3, 4, 5];

  readonly tamanio = 10;

  // ======================================
  // DATOS REALES DEL BACKEND
  // ======================================

  readonly reviews = signal<Resena[]>([]);

  readonly accommodations =
    signal<AlojamientoRespuesta[]>([]);

  readonly filter = signal('Todos');

  readonly pagina = signal(0);

  readonly totalPaginas = signal(0);

  readonly resumen =
    signal<ResumenResenasAnfitrion | null>(null);

  // ======================================
  // ESTADOS DE CARGA Y ERRORES
  // ======================================

  readonly cargando = signal(true);

  readonly cargandoAlojamientos = signal(true);

  readonly error = signal('');

  readonly errorAlojamientos = signal('');

  // ======================================
  // TOTAL REAL DE RESEÑAS
  // ======================================

  readonly cantidad = computed(() =>
    this.resumen()?.cantidad ?? 0
  );

  // ======================================
  // PROMEDIO REAL DE CALIFICACIÓN
  // ======================================

  readonly average = computed(() => {

    const promedio = this.resumen()?.promedio;

    if (promedio == null) {
      return '—';
    }

    return promedio
      .toFixed(1)
      .replace('.', ',');

  });

  // ======================================
  // RANGO DE PAGINACIÓN
  // ======================================

  readonly inicioPagina = computed(() => {

    if (this.cantidad() === 0) {
      return 0;
    }

    return this.pagina() * this.tamanio + 1;

  });

  readonly finPagina = computed(() => {

    return Math.min(
      (this.pagina() + 1) * this.tamanio,
      this.cantidad()
    );

  });

  // ======================================
  // NOMBRE DEL ALOJAMIENTO FILTRADO
  // ======================================

  readonly nombreFiltro = computed(() => {

    if (this.filter() === 'Todos') {
      return 'Todos mis alojamientos';
    }

    const id = Number(this.filter());

    const alojamiento = this.accommodations()
      .find(a => a.idAlojamiento === id);

    return alojamiento?.titulo ??
      `Alojamiento #${id}`;

  });

  // ======================================
  // INICIALIZACIÓN
  // ======================================

  ngOnInit(): void {

    this.cargarAlojamientos();

    this.cargarResenas();

  }

  // ======================================
  // LIMPIAR SUSCRIPCIONES
  // ======================================

  ngOnDestroy(): void {

    this.solicitudResenas?.unsubscribe();

    this.solicitudAlojamientos?.unsubscribe();

  }

  // ======================================
  // OBTENER TODOS LOS ALOJAMIENTOS
  // GET /alojamientos/mios
  // ======================================

  private listarTodosMisAlojamientos():
    Observable<AlojamientoRespuesta[]> {

    return this.publicaciones
      .obtenerMisAlojamientos(0, 100)
      .pipe(

        timeout(15000),

        expand((respuesta: PaginaAlojamientos) => {

          const siguiente =
            respuesta.pagina + 1;

          if (siguiente >= respuesta.totalPaginas) {
            return EMPTY;
          }

          return this.publicaciones
            .obtenerMisAlojamientos(siguiente, 100)
            .pipe(timeout(15000));

        }),

        reduce(
          (todos, respuesta) => [

            ...todos,

            ...(respuesta.contenido ?? [])

          ],
          [] as AlojamientoRespuesta[]
        )

      );
  }

  // ======================================
  // CARGAR ALOJAMIENTOS PARA EL FILTRO
  // ======================================

  cargarAlojamientos(): void {

    this.solicitudAlojamientos?.unsubscribe();

    this.cargandoAlojamientos.set(true);

    this.errorAlojamientos.set('');

    this.solicitudAlojamientos =
      this.listarTodosMisAlojamientos()
        .subscribe({

          next: lista => {

            // Ordenar alfabéticamente

            const ordenados = [...lista].sort(
              (a, b) =>
                a.titulo.localeCompare(
                  b.titulo,
                  'es'
                )
            );

            this.accommodations.set(ordenados);

            this.cargandoAlojamientos.set(false);

          },

          error: err => {

            this.cargandoAlojamientos.set(false);

            this.errorAlojamientos.set(
              this.mensajeError(
                err,
                'No fue posible consultar tus alojamientos.'
              )
            );

          }

        });
  }

  // ======================================
  // CARGAR RESEÑAS DEL ANFITRIÓN
  // GET /resenas/anfitrion
  // ======================================

  cargarResenas(): void {

    // Cancelar consulta anterior

    this.solicitudResenas?.unsubscribe();

    this.cargando.set(true);

    this.error.set('');

    // Construir parámetros

    const params: Record<string, string> = {

      pagina: String(this.pagina()),

      tamanio: String(this.tamanio)

    };

    // Agregar filtro por alojamiento
    // únicamente si se seleccionó uno

    if (this.filter() !== 'Todos') {

      params['idAlojamiento'] =
        this.filter();

    }

    // Consultar Spring Boot

    this.solicitudResenas =
      this.http.get<ResumenResenasAnfitrion>(
        `${this.apiUrl}/resenas/anfitrion`,
        { params }
      ).subscribe({

        next: respuesta => {

          // Guardar resumen completo

          this.resumen.set(respuesta);

          // Guardar reseñas de la página

          this.reviews.set(
            respuesta.resenas?.contenido ?? []
          );

          // Total de páginas

          this.totalPaginas.set(
            respuesta.resenas?.totalPaginas ?? 0
          );

          this.cargando.set(false);

        },

        error: err => {

          this.resumen.set(null);

          this.reviews.set([]);

          this.totalPaginas.set(0);

          this.cargando.set(false);

          this.error.set(
            this.mensajeError(
              err,
              'No fue posible cargar las reseñas.'
            )
          );

        }

      });
  }

  // ======================================
  // FILTRAR POR ALOJAMIENTO
  // ======================================

  cambiarFiltro(valor: string): void {

    if (valor === this.filter()) {
      return;
    }

    this.filter.set(valor);

    // Al filtrar, regresar a página 1

    this.pagina.set(0);

    this.cargarResenas();

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

    this.cargarResenas();

  }

  // ======================================
  // ACTUALIZAR DATOS
  // ======================================

  actualizar(): void {

    this.cargarResenas();

    this.cargarAlojamientos();

  }

  // ======================================
  // FORMATEAR FECHA DEL BACKEND
  // ======================================

  fechaFormateada(
    valor: string | null | undefined
  ): string {

    if (!valor) {
      return 'Fecha no disponible';
    }

    const partes =
      /^(\d{4})-(\d{2})-(\d{2})/.exec(valor);

    if (partes) {

      return (
        `${partes[3]}/` +
        `${partes[2]}/` +
        `${partes[1]}`
      );

    }

    return valor;

  }

  // ======================================
  // MANEJO DE ERRORES
  // ======================================

  private mensajeError(
    err: unknown,
    porDefecto: string
  ): string {

    if (!(err instanceof HttpErrorResponse)) {
      return porDefecto;
    }

    if (err.status === 0) {

      return (
        'No se pudo conectar con Spring Boot. ' +
        'Comprueba que esté en ejecución.'
      );

    }

    if (err.status === 401) {

      return (
        'Tu sesión expiró. ' +
        'Inicia sesión de nuevo.'
      );

    }

    if (err.status === 403) {

      return (
        'Tu cuenta no tiene permisos ' +
        'para ver estas reseñas.'
      );

    }

    if (err.status === 404) {

      return (
        'El alojamiento seleccionado no existe ' +
        'o no te pertenece.'
      );

    }

    const mensaje =
      err.error?.mensaje ??
      err.error?.message;

    if (
      typeof mensaje === 'string' &&
      mensaje.trim()
    ) {

      return mensaje;

    }

    return `${porDefecto} (HTTP ${err.status}).`;

  }

}
