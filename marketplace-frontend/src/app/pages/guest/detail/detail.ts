import {ChangeDetectorRef, Component, computed, inject, signal} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { photos } from '../../../services/mockData';
import {AlojamientoService} from '../../../services/alojamiento.service';
import {Alojamiento} from '../../../models/alojamientoDto';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {CurrencyPipe} from '@angular/common';
import {Resena} from '../../../models/resenaDto';


@Component({
  selector: 'app-detail',
  standalone: true,
  templateUrl: './detail.html',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule
  ]
})
export class Detail {
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private alojamientoService = inject(AlojamientoService);

  amenities: String[] = [];
  readonly photos = photos;

  loading = true;
  errorCarga = false;
  stay: Alojamiento | null = null;
  serviciosSeleccionados = signal<number[]>([]);

  private fb = inject(FormBuilder);

  reservaForm = this.fb.group({
    fechaEntrada: ['', Validators.required],
    fechaSalida: ['', Validators.required],
    huespedes: [1, [Validators.required, Validators.min(1)]],
  });

  enviando = false;
  solicitudEnviada = false;
  errorFechas = false;


  ngOnInit(): void {
    this.loading = true;
    this.errorCarga = false;

    const idParam = this.route.snapshot.paramMap.get('id');

    if (idParam === null || idParam.trim() === '') {
      console.error('[DETAIL] No se recibió el ID del alojamiento.');
      this.errorCarga = true;
      this.loading = false;
      return;
    }

    const id = Number(idParam);

    if (!Number.isInteger(id) || id <= 0) {
      console.error('[DETAIL] ID de alojamiento inválido:', idParam);
      this.errorCarga = true;
      this.loading = false;
      return;
    }

    this.alojamientoService.obtener(id).subscribe({
      next: (respuesta) => {
        if (!respuesta) {
          console.error('[DETAIL] El servicio devolvió una respuesta vacía.');
          this.errorCarga = true;
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }

        this.stay = respuesta;
        this.loading = false;
        this.amenities = this.stay.amenidades!;
        this.cdr.detectChanges();

      },

      error: (err) => {
        console.error('[DETAIL] Error al cargar el alojamiento:', err);
        this.errorCarga = true;
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }


    reviews = signal<Resena[]>([]);
    favorite = signal(false);

    visibleReviews = computed(() =>
      this.reviews()
        .filter((r) => r.accommodation === this.stay!.titulo)
        .slice(0, 4),
    );

    readonly highlights = [
      {icon: 'home', title: 'Un espacio solo para ti', text: 'Tendrás toda la casa durante tu estancia.'},
      {icon: 'pin', title: 'Ubicación privilegiada', text: 'Cerca de naturaleza, restaurantes y puntos de interés.'},
      {icon: 'star', title: 'Experiencia destacada', text: 'Una de las propiedades mejor calificadas en Medellín.'},
    ];


    readonly distribution = [
      {stars: 5, width: 82, count: 105},
      {stars: 4, width: 13, count: 17},
      {stars: 3, width: 4, count: 5},
      {stars: 2, width: 1, count: 1},
      {stars: 1, width: 0, count: 0},
    ];

    // ---- Mapa ilustrativo ----
    readonly horizontalLines = [18, 38, 58, 78];
    readonly verticalLines = [15, 35, 55, 75];
    private readonly markerPositions: Record<string, [number, number]> = {
      'Casa Bosque Verde': [58, 42],
      'Finca El Mirador': [36, 58],
      'Cabaña Montaña Viva': [67, 31],
      'Loft Urbano LR': [48, 65],
    };

    get marker(): [number, number] {
      return this.markerPositions[this.stay!.titulo] ?? [50, 50];
    }

    readonly starList = [1, 2, 3, 4, 5];

    // ---- Acciones ----
    toggleFavorite() {
      this.favorite.update((v) => !v);
    }

    openMap(id: number) {
      this.router.navigate(['/mapa', id]);
    }

    contactHost() {
      this.router.navigateByUrl('/mensajes');
    }

    initials(name: string) {
      return name
        .split(' ')
        .map((w) => w[0])
        .join('');
    }


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


  get subtotal(): number {
    return (this.stay!.precioNoche ?? 0) * this.noches;
  }

  get total(): number {
    if (this.noches <= 0) {
      return 0;
    }

    return this.subtotal + this.totalServiciosAdicionales;
  }

  get opcionesHuespedes(): number[] {
    const capacidad = Math.max(0, Math.floor(this.stay!.capacidad ?? 0));

    return Array.from(
      { length: capacidad },
      (_, index) => index + 1,
    );
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

  reserve(): void {
    this.solicitudEnviada = false;
    this.errorFechas = false;

    this.reservaForm.markAllAsTouched();

    if (this.reservaForm.invalid) {
      return;
    }

    const { fechaEntrada, fechaSalida, huespedes } =
      this.reservaForm.getRawValue();

    if (!fechaEntrada || !fechaSalida || !huespedes) {
      return;
    }

    if (
      this.fechaDesdeTexto(fechaEntrada).getTime() <
      this.fechaDesdeTexto(this.fechaMinima).getTime() ||
      this.fechaDesdeTexto(fechaSalida).getTime() <=
      this.fechaDesdeTexto(fechaEntrada).getTime()
    ) {
      this.errorFechas = true;
      return;
    }

    if (huespedes > (this.stay!.capacidad ?? 0)) {
      return;
    }

    const solicitud = {
      idAlojamiento: this.stay!.idAlojamiento,
      fechaEntrada,
      fechaSalida,
      huespedes,
      noches: this.noches,
      subtotal: this.subtotal,
      serviciosAdicionales: this.serviciosSeleccionados(),
      totalServiciosAdicionales: this.totalServiciosAdicionales,
      total: this.total,
    };

    console.log('Solicitud de reserva:', solicitud);

    // TODO: enviar solicitud al servicio de reservas.
  }

}
