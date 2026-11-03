import {ChangeDetectorRef, Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {debounceTime} from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { photos } from '../../../services/mockData';
import {AlojamientoService} from '../../../services/alojamiento.service';
import {Alojamiento} from '../../../models/alojamientoDto';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {CurrencyPipe, DatePipe, DecimalPipe} from '@angular/common';
import {Resena, ResumenResenas} from '../../../models/resenaDto';
import {FavoritoService} from '../../../services/favorito.service';
import {SessionService} from '../../../services/session.service';
import {ReservaService} from '../../../services/reserva.service';
import {Cotizacion} from '../../../models/cotizacionDto';
import {Reserva} from '../../../models/reservaDto';
import {HttpErrorResponse} from '@angular/common/http';
import {PagoReserva} from '../pago-reserva/pago-reserva';
import {DisponibilidadService} from '../../../services/disponibilidad.service';
import {MensajeriaService} from '../../../services/mensajeria.service';
@Component({
  selector: 'app-detail',
  standalone: true,
  templateUrl: './detail.html',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    PagoReserva
  ]
})
export class Detail implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly alojamientoService = inject(AlojamientoService);
  private readonly favoritoService = inject(FavoritoService);
  protected readonly session = inject(SessionService);
  private readonly reservaService = inject(ReservaService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly disponibilidadService = inject(DisponibilidadService);
  private readonly mensajeriaService = inject(MensajeriaService);
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
  /** Estado reactivo: se lee directamente del servicio compartido. */
  get favorite(): boolean {
    return this.favoritoService.esFavorito(this.stay?.idAlojamiento);
  }
  toggleFavorite(): void {
    this.favoritoService.toggle(this.stay?.idAlojamiento);
  }
  // ==================== Reserva ====================
  serviciosSeleccionados = signal<number[]>([]);
  enviando = false;
  /** Cotización vigente: mientras exista se muestra el modal de pago. */
  cotizacion: Cotizacion | null = null;
  reservaConfirmada: Reserva | null = null;
  errorReserva: string | null = null;
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
    this.favoritoService.cargarSiCorresponde();
    this.cargarAlojamiento();
    // Al elegir fechas se consulta si están disponibles
    this.reservaForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.consultarFechas());
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
          this.cargarDisponibilidad(idAlojamiento);
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
  openMap(id: number): void {
    this.router.navigate(['/mapa', id]);
  }
  contactando = false;
  errorContacto: string | null = null;
  /** Inicia (o retoma) la conversación con el anfitrión y abre la mensajería en ella. */
  contactHost(): void {
    const id = this.stay?.idAlojamiento;
    if (id == null || this.contactando) {
      return;
    }
    if (!this.session.autenticado()) {
      this.errorContacto = 'Inicia sesión para contactar al anfitrión.';
      return;
    }
    this.contactando = true;
    this.errorContacto = null;
    this.mensajeriaService.iniciar(id).subscribe({
      next: (conversacion) => {
        this.contactando = false;
        this.router.navigate(['/mensajes'], { queryParams: { conversacion: conversacion.id } });
      },
      error: (error) => {
        this.errorContacto = this.mensajeriaService.mensajeDeError(error, 'No se pudo iniciar la conversación.');
        this.contactando = false;
        this.cdr.detectChanges();
      },
    });
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
  // ==================== Disponibilidad ====================
  readonly LIMITE_RANGOS = 6;
  loadingDisponibilidad = false;
  errorDisponibilidad = false;
  /** Rangos de fechas libres: cada uno va de la entrada más temprana a la salida más tardía posibles. */
  rangosLibres: { desde: string; hasta: string }[] = [];
  mostrarTodosLosRangos = false;
  /** Resultado de consultar las fechas elegidas en el formulario. */
  consulta: { estado: 'cargando' | 'ok' | 'no' | 'error'; motivo?: string } | null = null;
  private consultaClave = '';
  get rangosVisibles(): { desde: string; hasta: string }[] {
    return this.mostrarTodosLosRangos ? this.rangosLibres : this.rangosLibres.slice(0, this.LIMITE_RANGOS);
  }
  private cargarDisponibilidad(idAlojamiento: number): void {
    this.loadingDisponibilidad = true;
    this.errorDisponibilidad = false;
    this.disponibilidadService.calendario(idAlojamiento).subscribe({
      next: ({ periodos, reservas }) => {
        this.rangosLibres = this.calcularRangosLibres(periodos, reservas);
        this.loadingDisponibilidad = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar la disponibilidad:', error);
        this.errorDisponibilidad = true;
        this.loadingDisponibilidad = false;
        this.cdr.detectChanges();
      },
    });
  }
  /** Períodos ofrecidos menos las fechas ya reservadas, desde hoy. La salida de una reserva queda libre para entrar. */
  private calcularRangosLibres(
    periodos: { fechaLlegada: string; fechaSalida: string }[],
    reservas: { fechaLlegada: string; fechaSalida: string }[],
  ): { desde: string; hasta: string }[] {
    const aDia = (s: string) => {
      const [a, m, d] = s.slice(0, 10).split('-').map(Number);
      return Date.UTC(a, m - 1, d) / 86400000;
    };
    const aTexto = (n: number) => new Date(n * 86400000).toISOString().slice(0, 10);
    const hoy = aDia(this.fechaMinima);
    const ocupadas = reservas.map((r) => [aDia(r.fechaLlegada), aDia(r.fechaSalida)]).sort((x, y) => x[0] - y[0]);
    const libres: [number, number][] = [];
    for (const p of periodos) {
      let inicio = Math.max(aDia(p.fechaLlegada), hoy);
      const fin = aDia(p.fechaSalida);
      for (const [desde, hasta] of ocupadas) {
        if (inicio >= fin) {
          break;
        }
        if (hasta <= inicio || desde >= fin) {
          continue;
        }
        if (desde > inicio) {
          libres.push([inicio, desde]);
        }
        inicio = Math.max(inicio, hasta);
      }
      if (inicio < fin) {
        libres.push([inicio, fin]);
      }
    }
    libres.sort((x, y) => x[0] - y[0]);
    const unidos: [number, number][] = [];
    for (const rango of libres) {
      const ultimo = unidos.at(-1);
      if (ultimo && rango[0] <= ultimo[1]) {
        ultimo[1] = Math.max(ultimo[1], rango[1]);
      } else {
        unidos.push([...rango]);
      }
    }
    return unidos.map(([a, b]) => ({ desde: aTexto(a), hasta: aTexto(b) }));
  }
  /** Consulta al backend si las fechas elegidas están disponibles (se ignoran fechas incompletas o inválidas). */
  private consultarFechas(): void {
    const id = this.stay?.idAlojamiento;
    const { fechaEntrada, fechaSalida } = this.reservaForm.getRawValue();
    if (id == null || !fechaEntrada || !fechaSalida || this.noches <= 0 || fechaEntrada < this.fechaMinima) {
      this.consulta = null;
      this.consultaClave = '';
      this.cdr.detectChanges();
      return;
    }
    const clave = `${fechaEntrada}|${fechaSalida}`;
    this.consultaClave = clave;
    this.consulta = { estado: 'cargando' };
    this.cdr.detectChanges();
    this.disponibilidadService.consultar(id, fechaEntrada, fechaSalida).subscribe({
      next: (r) => {
        if (this.consultaClave !== clave) {
          return; // el usuario ya cambió las fechas
        }
        this.consulta = r.disponible ? { estado: 'ok' } : { estado: 'no', motivo: r.motivo ?? undefined };
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        if (this.consultaClave !== clave) {
          return;
        }
        this.consulta = { estado: 'error', motivo: this.reservaService.mensajeDeError(error, 'No se pudo verificar la disponibilidad.') };
        this.cdr.detectChanges();
      },
    });
  }
  // ==================== Envío de reserva ====================
  reserve(): void {
    this.reservaConfirmada = null;
    this.errorReserva = null;
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
      this.errorReserva = `Este alojamiento admite hasta ${this.stay.capacidad ?? 0} huéspedes.`;
      return;
    }
    if (this.consulta?.estado === 'no') {
      this.errorReserva = `El alojamiento no está disponible en esas fechas${this.consulta.motivo ? `: ${this.consulta.motivo}` : '.'}`;
      return;
    }
    const idAlojamiento = this.stay.idAlojamiento;
    if (idAlojamiento == null) {
      this.errorReserva = 'No se pudo identificar el alojamiento.';
      return;
    }
    this.enviando = true;
    this.reservaService
      .cotizar({
        idAlojamiento,
        fechaEntrada,
        fechaSalida,
        huespedes,
        serviciosSeleccionados: this.serviciosSeleccionados(),
      })
      .subscribe({
        next: (cotizacion) => {
          this.cotizacion = cotizacion;
          this.enviando = false;
          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) => {
          this.errorReserva = this.reservaService.mensajeDeError(error, 'No se pudo cotizar la estancia.');
          this.enviando = false;
          this.cdr.detectChanges();
        },
      });
  }
  /** El pago se aprobó y la reserva quedó CONFIRMADA. */
  onReservado(reserva: Reserva): void {
    this.cotizacion = null;
    this.reservaConfirmada = reserva;
    this.cdr.detectChanges();
  }
  cerrarPago(): void {
    this.cotizacion = null;
  }
  /** Cierra el modal y pide una cotización nueva con los mismos datos del formulario. */
  recotizar(): void {
    this.cotizacion = null;
    this.reserve();
  }
}
