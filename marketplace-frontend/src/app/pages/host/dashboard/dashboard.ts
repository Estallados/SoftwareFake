
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

import { Router } from '@angular/router';

import {
  EMPTY,
  Observable,
  Subscription,
  catchError,
  expand,
  forkJoin,
  of,
  reduce,
  timeout
} from 'rxjs';

import {
  AlojamientoRespuesta,
  PaginaAlojamientos,
  PublicacionHostService
} from '../../../services/publicacion-host.service';

import { Reserva } from '../../../models/reservaDto';
import { API_URL } from '../../../services/api-url';
import { User } from '../../../services/user';

// ======================================
// INTERFACES
// ======================================

interface PaginaReservas<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

interface ResumenResenas {
  promedio: number | null;
  cantidad: number;
}

export interface HostMetric {
  label: string;
  value: string;
  icon: 'building' | 'calendar' | 'chart' | 'star';
  delta: string;
}

export interface MonthPerformance {
  month: string;
  percent: number;
  total: number;
}

export interface UpcomingStay {
  idReserva: number;
  date: string;
  accommodation: string;
  guest: string;
}

@Component({
  selector: 'app-host-dashboard',
  standalone: true,
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit, OnDestroy {

  // ======================================
  // SERVICIOS
  // ======================================

  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  private readonly publicaciones =
    inject(PublicacionHostService);

  private readonly usuarios = inject(User);

  private solicitud?: Subscription;

  // ======================================
  // ESTADOS DEL DASHBOARD
  // ======================================

  readonly hostName = signal('Anfitrión');

  readonly alojamientos =
    signal<AlojamientoRespuesta[]>([]);

  readonly reservas = signal<Reserva[]>([]);

  readonly resenas =
    signal<ResumenResenas | null>(null);

  readonly cargando = signal(true);
  readonly error = signal('');

  // ======================================
  // SALUDO SEGÚN LA HORA
  // ======================================

  readonly saludo = (() => {

    const hora = new Date().getHours();

    return hora < 12
      ? 'Buenos días'
      : hora < 18
        ? 'Buenas tardes'
        : 'Buenas noches';

  })();

  // ======================================
  // MÉTRICAS DEL ANFITRIÓN
  // ======================================

  readonly metrics = computed<HostMetric[]>(() => {

    const alojamientos = this.alojamientos();
    const reservas = this.reservas();

    const confirmadas = reservas.filter(
      r => r.estado === 'CONFIRMADA'
    );

    const ingreso = confirmadas.reduce(
      (total, r) => total + Number(r.total ?? 0),
      0
    );

    const resumen = this.resenas();

    const activos = alojamientos.filter(
      a => a.estado === 'ACTIVA'
    ).length;

    return [

      // ALOJAMIENTOS
      {
        label: 'Alojamientos',
        value: String(alojamientos.length),
        icon: 'building',
        delta: `${activos} activos`
      },

      // RESERVAS
      {
        label: 'Reservas confirmadas',
        value: String(confirmadas.length),
        icon: 'calendar',
        delta: `${reservas.length} en total`
      },

      // INGRESOS
      {
        label: 'Ingresos estimados',
        value: this.moneda(ingreso),
        icon: 'chart',
        delta: 'Reservas confirmadas'
      },

      // CALIFICACIÓN
      {
        label: 'Calificación promedio',

        value: resumen?.promedio == null
          ? '—'
          : `${resumen.promedio.toFixed(1)} / 5`,

        icon: 'star',

        delta: resumen == null
          ? 'No disponible'
          : `${resumen.cantidad} reseñas`
      }

    ];
  });

  // ======================================
  // RENDIMIENTO DE RESERVAS
  // ÚLTIMOS 6 MESES
  // ======================================

  readonly performance =
    computed<MonthPerformance[]>(() => {

      const hoy = new Date();

      const confirmadas = this.reservas().filter(
        r => r.estado === 'CONFIRMADA'
      );

      const meses = Array.from(
        { length: 6 },
        (_, i) => {

          const fecha = new Date(
            hoy.getFullYear(),
            hoy.getMonth() - (5 - i),
            1
          );

          const clave =
            `${fecha.getFullYear()}-` +
            `${String(fecha.getMonth() + 1).padStart(2, '0')}`;

          const mes = new Intl.DateTimeFormat(
            'es-CO',
            { month: 'short' }
          )
            .format(fecha)
            .replace('.', '');

          const total = confirmadas.filter(
            r => r.fechaEntrada?.startsWith(clave)
          ).length;

          return {
            month: mes,
            total
          };

        }
      );

      const maximo = Math.max(
        ...meses.map(m => m.total),
        1
      );

      return meses.map(m => ({
        ...m,
        percent: (m.total / maximo) * 100
      }));

    });

  // ======================================
  // INGRESOS MENSUALES REALES
  // ÚLTIMOS 6 MESES
  // ======================================

  readonly ingresosMensuales = computed(() => {

    const hoy = new Date();

    const confirmadas = this.reservas().filter(
      r => r.estado === 'CONFIRMADA'
    );

    return Array.from(
      { length: 6 },
      (_, indice) => {

        const fecha = new Date(
          hoy.getFullYear(),
          hoy.getMonth() - 5 + indice,
          1
        );

        const clave =
          `${fecha.getFullYear()}-` +
          `${String(fecha.getMonth() + 1).padStart(2, '0')}`;

        const mes = new Intl.DateTimeFormat(
          'es-CO',
          { month: 'short' }
        )
          .format(fecha)
          .replace('.', '');

        const total = confirmadas

          .filter(r =>
            r.fechaEntrada?.startsWith(clave)
          )

          .reduce(
            (suma, r) =>
              suma + Number(r.total ?? 0),
            0
          );

        return {
          mes,
          total
        };

      }
    );
  });

  // ======================================
  // INGRESOS DEL MES ACTUAL
  // ======================================

  readonly ingresoMesActualTexto = computed(() =>

    this.moneda(
      this.ingresosMensuales()[5]?.total ?? 0
    )

  );

  // ======================================
  // VARIACIÓN DE INGRESOS
  // RESPECTO AL MES ANTERIOR
  // ======================================

  readonly variacionIngresos = computed(() => {

    const meses = this.ingresosMensuales();

    const anterior = meses[4]?.total ?? 0;

    const actual = meses[5]?.total ?? 0;

    // Evitar división entre cero
    if (anterior === 0) {

      return {
        texto: actual > 0
          ? 'Ingresos nuevos este mes'
          : 'Sin base de comparación',

        positivo: null as boolean | null
      };

    }

    const cambio =
      ((actual - anterior) / anterior) * 100;

    return {

      texto:
        `${cambio > 0 ? '+' : ''}` +
        `${cambio.toFixed(1)}% vs. mes anterior`,

      positivo: cambio >= 0 as boolean | null

    };

  });

  // ======================================
  // GRÁFICA SVG DE INGRESOS
  // DISEÑO AURORA
  // ======================================

  readonly graficoIngresos = computed(() => {

    const valores = this.ingresosMensuales()
      .map(m => m.total);

    // No mostrar una curva ficticia
    // cuando no existen ingresos
    if (!valores.some(valor => valor > 0)) {
      return null;
    }

    const maximo = Math.max(
      ...valores,
      1
    );

    // Calcular puntos del gráfico SVG
    const puntos = valores.map(
      (valor, indice) => ({

        x: 8 + indice * (284 / 5),

        y: 85 - (valor / maximo) * 63

      })
    );

    // Construir una curva suave
    let linea =
      `M ${puntos[0].x} ${puntos[0].y}`;

    for (
      let i = 1;
      i < puntos.length;
      i++
    ) {

      const anterior = puntos[i - 1];
      const actual = puntos[i];

      const mitad =
        (anterior.x + actual.x) / 2;

      linea +=
        ` C ${mitad} ${anterior.y},` +
        ` ${mitad} ${actual.y},` +
        ` ${actual.x} ${actual.y}`;

    }

    return {

      // Línea de la gráfica
      linea,

      // Área con degradado
      area:
        `${linea} L 292 100 L 8 100 Z`,

      // Punto final
      puntoX: puntos[puntos.length - 1].x,

      puntoY: puntos[puntos.length - 1].y

    };

  });

  // ======================================
  // COMPROBAR SI HAY RENDIMIENTO
  // ======================================

  readonly hayRendimiento = computed(() =>

    this.performance().some(
      m => m.total > 0
    )

  );

  // ======================================
  // PRÓXIMAS ESTANCIAS
  // ======================================

  readonly upcomingStays =
    computed<UpcomingStay[]>(() => {

      const hoy = this.fechaLocal(
        new Date()
      );

      return this.reservas()

        .filter(r =>
          r.estado === 'CONFIRMADA' &&
          !r.completada &&
          r.fechaEntrada >= hoy
        )

        .sort((a, b) =>
          a.fechaEntrada.localeCompare(
            b.fechaEntrada
          )
        )

        .slice(0, 5)

        .map(r => ({

          idReserva: r.idReserva,

          date: this.fechaCorta(
            r.fechaEntrada
          ),

          accommodation:
            r.tituloAlojamiento ||
            `Alojamiento #${r.idAlojamiento}`,

          guest:
            r.nombreHuesped || 'Huésped'

        }));

    });

  // ======================================
  // INICIALIZACIÓN
  // ======================================

  ngOnInit(): void {
    this.cargarDashboard();
  }

  ngOnDestroy(): void {
    this.solicitud?.unsubscribe();
  }

  // ======================================
  // CONSULTAR ALOJAMIENTOS DEL ANFITRIÓN
  // GET /alojamientos/mios
  // ======================================

  private todosMisAlojamientos():
    Observable<AlojamientoRespuesta[]> {

    return this.publicaciones
      .obtenerMisAlojamientos(0, 100)
      .pipe(

        timeout(15000),

        expand((pagina: PaginaAlojamientos) => {

          const siguiente =
            pagina.pagina + 1;

          if (
            siguiente >= pagina.totalPaginas
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
          (acumulado, pagina) => [

            ...acumulado,

            ...(pagina.contenido ?? [])

          ],
          [] as AlojamientoRespuesta[]
        )

      );
  }

  // ======================================
  // CONSULTAR RESERVAS DEL ANFITRIÓN
  // GET /reservas/anfitrion
  // ======================================

  private todasMisReservas():
    Observable<Reserva[]> {

    const obtenerPagina = (numero: number) => {

      return this.http
        .get<PaginaReservas<Reserva>>(
          `${this.apiUrl}/reservas/anfitrion`,
          {
            params: {
              pagina: numero,
              tamanio: 100
            }
          }
        )
        .pipe(timeout(15000));

    };

    return obtenerPagina(0).pipe(

      expand(pagina => {

        const siguiente =
          pagina.pagina + 1;

        if (
          siguiente >= pagina.totalPaginas
        ) {
          return EMPTY;
        }

        return obtenerPagina(siguiente);

      }),

      reduce(
        (acumulado, pagina) => [

          ...acumulado,

          ...(pagina.contenido ?? [])

        ],
        [] as Reserva[]
      )

    );
  }

  // ======================================
  // CARGAR DATOS DEL DASHBOARD
  // ======================================

  cargarDashboard(): void {

    // Cancelar consulta anterior
    this.solicitud?.unsubscribe();

    this.cargando.set(true);
    this.error.set('');

    this.solicitud = forkJoin({

      // PERFIL DEL ANFITRIÓN
      perfil: this.usuarios
        .obtenerMiPerfil()
        .pipe(
          catchError(() => of(null))
        ),

      // ALOJAMIENTOS
      alojamientos:
        this.todosMisAlojamientos(),

      // RESERVAS
      reservas:
        this.todasMisReservas(),

      // RESEÑAS
      resenas: this.http
        .get<ResumenResenas>(
          `${this.apiUrl}/resenas/anfitrion`,
          {
            params: {
              pagina: 0,
              tamanio: 1
            }
          }
        )
        .pipe(
          catchError(() => of(null))
        )

    }).subscribe({

      // RESPUESTA CORRECTA
      next: datos => {

        const nombre =
          datos.perfil?.nombre?.trim();

        this.hostName.set(
          nombre || 'Anfitrión'
        );

        this.alojamientos.set(
          datos.alojamientos
        );

        this.reservas.set(
          datos.reservas
        );

        this.resenas.set(
          datos.resenas
        );

        this.cargando.set(false);

      },

      // ERROR
      error: err => {

        this.error.set(
          this.mensajeError(err)
        );

        this.cargando.set(false);

      }

    });
  }

  // ======================================
  // FORMATO DE FECHA LOCAL
  // ======================================

  private fechaLocal(fecha: Date): string {

    const anio = fecha.getFullYear();

    const mes = String(
      fecha.getMonth() + 1
    ).padStart(2, '0');

    const dia = String(
      fecha.getDate()
    ).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }

  // ======================================
  // FECHA CORTA DD/MM
  // ======================================

  private fechaCorta(fecha: string): string {

    const partes =
      /^(\d{4})-(\d{2})-(\d{2})$/.exec(
        fecha
      );

    return partes
      ? `${partes[3]}/${partes[2]}`
      : fecha;
  }

  // ======================================
  // MONEDA COLOMBIANA
  // ======================================

  private moneda(valor: number): string {

    return new Intl.NumberFormat(
      'es-CO',
      {
        style: 'currency',
        currency: 'COP',
        maximumFractionDigits: 0
      }
    ).format(valor);
  }

  // ======================================
  // MANEJO DE ERRORES
  // ======================================

  private mensajeError(err: unknown): string {

    if (err instanceof HttpErrorResponse) {

      if (err.status === 0) {
        return 'No hay conexión con Spring Boot (localhost:8081).';
      }

      if (err.status === 401) {
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      }

      if (err.status === 403) {
        return 'Tu cuenta no tiene permiso para consultar estos datos.';
      }

      return err.error?.message ??
        `No se pudo cargar el dashboard (HTTP ${err.status}).`;

    }

    return 'No se pudieron cargar los indicadores del dashboard.';
  }

  // ======================================
  // CREAR ALOJAMIENTO
  // ======================================

  createListing(): void {

    void this.router.navigateByUrl(
      '/host/alojamientos/crear'
    );
  }

  // ======================================
  // VER DETALLE DE RESERVA
  // ======================================

  verReserva(id: number): void {

    void this.router.navigate([
      '/host/reservas',
      id
    ]);
  }

  // ======================================
  // VER TODAS LAS RESERVAS
  // ======================================

  verReservas(): void {

    void this.router.navigateByUrl(
      '/host/reservas'
    );
  }
}
