import {ChangeDetectorRef, Component, computed, inject, OnInit, signal} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { photos } from '../../../services/mockData';
import {AlojamientoService} from '../../../services/alojamiento.service';
import {Alojamiento} from '../../../models/alojamientoDto';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {CurrencyPipe, DatePipe, DecimalPipe} from '@angular/common';
import {Resena, ResumenResenas} from '../../../models/resenaDto';

@Component({
  selector: 'app-detail',
  standalone: true,
  templateUrl: './detail.html',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule
  ]
})
export class Detail implements OnInit {

  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly alojamientoService = inject(AlojamientoService);
  private readonly fb = inject(FormBuilder);

  // ==================== Alojamiento ====================

  readonly photos = photos;

  amenities: string[] = [];

  stay: Alojamiento | null = null;

  loading = true;
  errorCarga = false;

  // ==================== Reseñas ====================

  reviews: Resena[] = [];

  loadingReviews = false;
  errorResenas = false;

  promedioResenas = 0;
  cantidadResenas = 0;

  readonly starList = [1, 2, 3, 4, 5];

  /**
   * Muestra las cuatro reseñas más recientes que entrega el backend.
   * No filtra por título porque el endpoint ya corresponde al alojamiento.
   */
  visibleReviews = computed(() => this.reviews.slice(0, 4));

  /**
   * Calcula la distribución utilizando las reseñas recibidas.
   *
   * Nota: si el backend devuelve solo una parte de las reseñas,
   * la distribución representa únicamente las reseñas recibidas.
   */
  get distribution(): { stars: number; count: number; width: number }[] {
    return [5, 4, 3, 2, 1].map((stars) => {
      const count = this.reviews.filter(
        (resena) => resena.calificacion === stars
      ).length;

      const width = this.reviews.length > 0
        ? (count / this.reviews.length) * 100
        : 0;

      return {
        stars,
        count,
        width
      };
    });
  }

  // ==================== Favoritos ====================

  favorite = signal(false);

  // ==================== Reserva ====================

  serviciosSeleccionados = signal<number[]>([]);

  enviando = false;
  solicitudEnviada = false;
  errorFechas = false;

  reservaForm = this.fb.group({
    fechaEntrada: ['', Validators.required],
    fechaSalida: ['', Validators.required],
    huespedes: [1, [Validators.required, Validators.min(1)]]
  });

  // ==================== Información visual ====================

  readonly highlights = [
    {
      icon: 'home',
      title: 'Un espacio solo para ti',
      text: 'Tendrás toda la casa durante tu estancia.'
    },
    {
      icon: 'pin',
      title: 'Ubicación privilegiada',
      text: 'Cerca de naturaleza, restaurantes y puntos de interés.'
    },
    {
      icon: 'star',
      title: 'Experiencia destacada',
      text: 'Una de las propiedades mejor calificadas en Medellín.'
    }
  ];

  readonly horizontalLines = [18, 38, 58, 78];

  readonly verticalLines = [15, 35, 55, 75];

  private readonly markerPositions: Record<string, [number, number]> = {
    'Casa Bosque Verde': [58, 42],
    'Finca El Mirador': [36, 58],
    'Cabaña Montaña Viva': [67, 31],
    'Loft Urbano LR': [48, 65]
  };

  get marker(): [number, number] {
    if (!this.stay) {
      return [50, 50];
    }

    return this.markerPositions[this.stay.titulo] ?? [50, 50];
  }

  // ==================== Inicialización ====================

  ngOnInit(): void {
    this.cargarAlojamiento();
  }

  private cargarAlojamiento(): void {
    this.loading = true;
    this.errorCarga = false;

    const idParam = this.route.snapshot.paramMap.get('id');

    if (!idParam || idParam.trim() === '') {
      this.errorCarga = true;
      this.loading = false;
      return;
    }

    const id = Number(idParam);

    if (!Number.isInteger(id) || id <= 0) {
      this.errorCarga = true;
      this.loading = false;
      return;
    }

    this.alojamientoService.obtener(id).subscribe({
      next: (respuesta) => {
        if (!respuesta) {
          this.errorCarga = true;
          this.loading = false;
          return;
        }

        this.stay = respuesta;
        this.amenities = respuesta.amenidades ?? [];
        this.loading = false;

        const idAlojamiento = respuesta.idAlojamiento;

        if (idAlojamiento != null) {
          this.cargarResenas(idAlojamiento);
        }

        this.cdr.detectChanges();
      },

      error: (error) => {
        console.error('Error al cargar el alojamiento:', error);

        this.errorCarga = true;
        this.loading = false;

        this.cdr.detectChanges();
      }
    });
  }

  // ==================== Carga de reseñas ====================

  private cargarResenas(idAlojamiento: number): void {
    this.loadingReviews = true;
    this.errorResenas = false;

    this.alojamientoService.resenasDeAlojamiento(idAlojamiento).subscribe({
      next: (respuesta: ResumenResenas) => {
        // El backend devuelve las reseñas dentro de resenas.contenido.
        this.reviews = respuesta.resenas?.contenido ?? [];

        // Promedio y cantidad globales enviados por el backend.
        this.promedioResenas = respuesta.promedio ?? 0;
        this.cantidadResenas = respuesta.cantidad ?? 0;

        this.loadingReviews = false;

        this.cdr.detectChanges();
      },

      error: (error) => {
        console.error('Error al cargar las reseñas:', error);

        this.reviews = [];
        this.promedioResenas = 0;
        this.cantidadResenas = 0;

        this.loadingReviews = false;
        this.errorResenas = true;

        this.cdr.detectChanges();
      }
    });
  }

  // ==================== Acciones ====================

  toggleFavorite(): void {
    this.favorite.update((value) => !value);
  }

  openMap(id: number): void {
    this.router.navigate(['/mapa', id]);
  }

  contactHost(): void {
    this.router.navigateByUrl('/mensajes');
  }

  initials(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .map((word) => word[0])
      .join('')
      .toUpperCase();
  }

  // ==================== Fechas de reserva ====================

  get fechaMinima(): string {
    return this.fechaLocal(new Date());
  }

  get fechaMinimaSalida(): string {
    const entrada = this.reservaForm.controls.fechaEntrada.value;

    if (!entrada) {
      return this.fechaMinima;
    }

    const fecha = this.fechaDesdeTexto(entrada);
    fecha.setDate(fecha.getDate() + 1);

    return this.fechaLocal(fecha);
  }

  get noches(): number {
    const { fechaEntrada, fechaSalida } = this.reservaForm.getRawValue();

    if (!fechaEntrada || !fechaSalida) {
      return 0;
    }

    const entrada = this.fechaDesdeTexto(fechaEntrada);
    const salida = this.fechaDesdeTexto(fechaSalida);

    const diferencia = salida.getTime() - entrada.getTime();

    return diferencia > 0
      ? Math.round(diferencia / 86400000)
      : 0;
  }

  private fechaLocal(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }

  private fechaDesdeTexto(valor: string): Date {
    const [anio, mes, dia] = valor.split('-').map(Number);

    return new Date(anio, mes - 1, dia);
  }

  // ==================== Servicios adicionales ====================

  servicioSeleccionado(idServicio: number): boolean {
    return this.serviciosSeleccionados().includes(idServicio);
  }

  toggleServicio(idServicio: number, seleccionado: boolean): void {
    this.serviciosSeleccionados.update((actuales) => {
      if (seleccionado) {
        return actuales.includes(idServicio)
          ? actuales
          : [...actuales, idServicio];
      }

      return actuales.filter((id) => id !== idServicio);
    });
  }

  get totalServiciosAdicionales(): number {
    const seleccionados = this.serviciosSeleccionados();

    return (this.stay?.serviciosAdicionales ?? [])
      .filter(
        (servicio) =>
          servicio.idServicio !== undefined &&
          seleccionados.includes(servicio.idServicio)
      )
      .reduce((total, servicio) => total + servicio.costo, 0);
  }

  // ==================== Cálculos de reserva ====================

  get subtotal(): number {
    return (this.stay?.precioNoche ?? 0) * this.noches;
  }

  get total(): number {
    if (this.noches <= 0) {
      return 0;
    }

    return this.subtotal + this.totalServiciosAdicionales;
  }

  get opcionesHuespedes(): number[] {
    const capacidad = Math.max(
      0,
      Math.floor(this.stay?.capacidad ?? 0)
    );

    return Array.from(
      { length: capacidad },
      (_, index) => index + 1
    );
  }

  // ==================== Envío de reserva ====================

  reserve(): void {
    this.solicitudEnviada = false;
    this.errorFechas = false;

    this.reservaForm.markAllAsTouched();

    if (this.reservaForm.invalid || !this.stay) {
      return;
    }

    const { fechaEntrada, fechaSalida, huespedes } =
      this.reservaForm.getRawValue();

    if (!fechaEntrada || !fechaSalida || !huespedes) {
      return;
    }

    const entrada = this.fechaDesdeTexto(fechaEntrada);
    const salida = this.fechaDesdeTexto(fechaSalida);
    const hoy = this.fechaDesdeTexto(this.fechaMinima);

    if (
      entrada.getTime() < hoy.getTime() ||
      salida.getTime() <= entrada.getTime()
    ) {
      this.errorFechas = true;
      return;
    }

    if (huespedes > (this.stay.capacidad ?? 0)) {
      return;
    }

    const solicitud = {
      idAlojamiento: this.stay.idAlojamiento,
      fechaEntrada,
      fechaSalida,
      huespedes,
      noches: this.noches,
      subtotal: this.subtotal,
      serviciosAdicionales: this.serviciosSeleccionados(),
      totalServiciosAdicionales: this.totalServiciosAdicionales,
      total: this.total
    };

    console.log('Solicitud de reserva:', solicitud);

    // TODO: enviar la solicitud al servicio de reservas.
  }
}
