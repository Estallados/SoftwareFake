import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { Alojamiento } from '../../../models/alojamientoDto';
import { AlojamientoService } from '../../../services/alojamiento.service';
import { FavoritoService } from '../../../services/favorito.service';
import { photos } from '../../../services/mockData';
type SortOption =
  | 'Recomendados'
  | 'Precio menor'
  | 'Precio mayor'
  | 'Mejor calificados';
@Component({
  selector: 'app-explore',
  standalone: true,
  templateUrl: './explore.html',
  imports: [CurrencyPipe, DecimalPipe],
})
export class Explore implements OnInit {
  private readonly router = inject(Router);
  protected readonly alojamientoService = inject(AlojamientoService);
  protected readonly favoritoService = inject(FavoritoService);
  // ==================== DATOS ====================
  readonly photos = photos;
  stays = signal<Alojamiento[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  // ==================== FILTROS ====================
  filterType = signal('Todos');
  sortBy = signal<SortOption>('Recomendados');
  precioMin = signal(0);
  precioMax = signal(0);
  serviciosSeleccionados = signal<string[]>([]);
  readonly sortOptions: SortOption[] = [
    'Recomendados',
    'Precio menor',
    'Precio mayor',
    'Mejor calificados',
  ];
  readonly searchFields = [
    {
      icon: 'pin',
      label: '¿A dónde?',
      value: 'Ciudad o destino',
    },
    {
      icon: 'calendar',
      label: 'Entrada',
      value: 'Seleccionar fecha',
    },
    {
      icon: 'calendar',
      label: 'Salida',
      value: 'Seleccionar fecha',
    },
  ];
  // ==================== PRECIOS DISPONIBLES ====================
  readonly preciosDisponibles = computed(() => {
    return this.stays()
      .map((alojamiento) => alojamiento.precioNoche)
      .filter(
        (precio): precio is number =>
          typeof precio === 'number' &&
          Number.isFinite(precio) &&
          precio >= 0,
      );
  });
  readonly precioMinimoDisponible = computed(() => {
    const precios = this.preciosDisponibles();
    return precios.length > 0
      ? Math.min(...precios)
      : 0;
  });
  readonly precioMaximoDisponible = computed(() => {
    const precios = this.preciosDisponibles();
    return precios.length > 0
      ? Math.max(...precios)
      : 0;
  });
  /** Paso del slider: ~100 posiciones, redondeado a 1, 10, 100, 1.000... */
  readonly paso = computed(() => {
    const rango =
      this.precioMaximoDisponible() - this.precioMinimoDisponible();
    if (rango <= 0) {
      return 1;
    }
    const potencia = Math.pow(10, Math.floor(Math.log10(rango / 100)));
    return Math.max(1, potencia);
  });
  readonly sinRangoDePrecios = computed(
    () => this.precioMaximoDisponible() <= this.precioMinimoDisponible(),
  );
  readonly porcentajeMin = computed(() => this.aPorcentaje(this.precioMin()));
  readonly porcentajeMax = computed(() => this.aPorcentaje(this.precioMax()));
  private aPorcentaje(valor: number): number {
    const min = this.precioMinimoDisponible();
    const rango = this.precioMaximoDisponible() - min;
    if (rango <= 0) {
      return 0;
    }
    return Math.min(100, Math.max(0, ((valor - min) / rango) * 100));
  }
  // ==================== INICIALIZACIÓN ====================
  ngOnInit(): void {
    this.favoritoService.cargarSiCorresponde();
    this.cargarAlojamientos();
  }
  reintentar(): void {
    this.cargarAlojamientos();
  }
  private cargarAlojamientos(): void {
    this.loading.set(true);
    this.errorCarga.set(false);
    this.alojamientoService.disponibles(100).subscribe({
      next: (respuesta: Alojamiento[]) => {
        this.stays.set(respuesta ?? []);
        this.precioMin.set(
          this.precioMinimoDisponible(),
        );
        this.precioMax.set(
          this.precioMaximoDisponible(),
        );
        this.loading.set(false);
      },
      error: (error) => {
        console.error(
          'Error al cargar alojamientos:',
          error,
        );
        this.stays.set([]);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }
  // ==================== TIPOS DINÁMICOS ====================
  readonly types = computed(() => {
    const tipos = [
      ...new Set(
        this.stays()
          .map((alojamiento) => alojamiento.tipo)
          .filter(
            (tipo): tipo is string =>
              typeof tipo === 'string' &&
              tipo.trim().length > 0,
          ),
      ),
    ].sort((a, b) => a.localeCompare(b, 'es'));
    return ['Todos', ...tipos];
  });
  // ==================== SERVICIOS DINÁMICOS ====================
  readonly services = computed(() => {
    const servicios = this.stays().flatMap(
      (alojamiento) => alojamiento.amenidades ?? [],
    );
    return [
      ...new Set(
        servicios
          .filter(
            (servicio): servicio is string =>
              typeof servicio === 'string' &&
              servicio.trim().length > 0,
          )
          .map((servicio) => servicio.trim()),
      ),
    ].sort((a, b) => a.localeCompare(b, 'es'));
  });
  // ==================== FILTRO DE SERVICIOS ====================
  private tieneServiciosSeleccionados(
    alojamiento: Alojamiento,
  ): boolean {
    const seleccionados = this.serviciosSeleccionados();
    if (seleccionados.length === 0) {
      return true;
    }
    const amenidades = (
      alojamiento.amenidades ?? []
    ).map((amenidad) =>
      amenidad.trim().toLocaleLowerCase('es'),
    );
    return seleccionados.every((servicio) =>
      amenidades.includes(
        servicio.toLocaleLowerCase('es'),
      ),
    );
  }
  toggleServicio(
    servicio: string,
    seleccionado: boolean,
  ): void {
    this.serviciosSeleccionados.update((actuales) => {
      if (seleccionado) {
        return actuales.includes(servicio)
          ? actuales
          : [...actuales, servicio];
      }
      return actuales.filter(
        (actual) => actual !== servicio,
      );
    });
  }
  servicioSeleccionado(servicio: string): boolean {
    return this.serviciosSeleccionados().includes(servicio);
  }
  // ==================== FILTRADO Y ORDENAMIENTO ====================
  readonly visible = computed(() => {
    let resultado = [...this.stays()];
    // FILTRO POR TIPO
    const tipo = this.filterType();
    if (tipo !== 'Todos') {
      resultado = resultado.filter(
        (alojamiento) =>
          alojamiento.tipo === tipo,
      );
    }
    // FILTRO POR PRECIO
    const minimo = this.precioMin();
    const maximo = this.precioMax();
    resultado = resultado.filter((alojamiento) => {
      const precio = alojamiento.precioNoche;
      if (
        typeof precio !== 'number' ||
        !Number.isFinite(precio)
      ) {
        return false;
      }
      return precio >= minimo && precio <= maximo;
    });
    // FILTRO POR SERVICIOS
    resultado = resultado.filter((alojamiento) =>
      this.tieneServiciosSeleccionados(alojamiento),
    );
    // ORDENAMIENTO
    switch (this.sortBy()) {
      case 'Precio menor':
        resultado.sort(
          (a, b) =>
            (a.precioNoche ?? 0) -
            (b.precioNoche ?? 0),
        );
        break;
      case 'Precio mayor':
        resultado.sort(
          (a, b) =>
            (b.precioNoche ?? 0) -
            (a.precioNoche ?? 0),
        );
        break;
      case 'Mejor calificados':
        resultado.sort(
          (a, b) =>
            (b.promedioCalificacion ?? 0) -
            (a.promedioCalificacion ?? 0),
        );
        break;
      case 'Recomendados':
      default:
        break;
    }
    return resultado;
  });
  // ==================== CAMBIO DE ORDENAMIENTO ====================
  onSortChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const valor = select.value as SortOption;
    if (this.sortOptions.includes(valor)) {
      this.sortBy.set(valor);
    }
  }
  // ==================== EVENTOS DEL PRECIO ====================
  actualizarPrecioMin(event: Event): void {
    const input = event.target as HTMLInputElement;
    const valor = Number(input.value);
    const minimo = this.precioMinimoDisponible();
    const maximoActual = this.precioMax();
    const nuevo = Number.isFinite(valor) && input.value !== ''
      ? Math.min(Math.max(valor, minimo), maximoActual)
      : this.precioMin();
    this.precioMin.set(nuevo);
    // Mantiene el control sincronizado si el valor fue ajustado
    input.value = String(nuevo);
  }
  actualizarPrecioMax(event: Event): void {
    const input = event.target as HTMLInputElement;
    const valor = Number(input.value);
    const maximo = this.precioMaximoDisponible();
    const minimoActual = this.precioMin();
    const nuevo = Number.isFinite(valor) && input.value !== ''
      ? Math.max(Math.min(valor, maximo), minimoActual)
      : this.precioMax();
    this.precioMax.set(nuevo);
    input.value = String(nuevo);
  }
  // ==================== NAVEGACIÓN ====================
  openStay(id: number | undefined): void {
    if (id == null) {
      console.error('El alojamiento no tiene un ID válido.');
      return;
    }
    this.router.navigate([
      '/alojamiento',
      id,
    ]);
  }
  goSearch(): void {
    this.router.navigateByUrl('/explorar');
  }
  // ==================== LIMPIAR FILTROS ====================
  limpiarFiltros(): void {
    this.filterType.set('Todos');
    this.serviciosSeleccionados.set([]);
    this.sortBy.set('Recomendados');
    this.precioMin.set(
      this.precioMinimoDisponible(),
    );
    this.precioMax.set(
      this.precioMaximoDisponible(),
    );
  }
}
