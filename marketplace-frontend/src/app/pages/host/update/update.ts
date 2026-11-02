
import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal
} from '@angular/core';

import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import {
  Observable,
  concatMap,
  finalize,
  from,
  map,
  of,
  switchMap,
  tap,
  toArray
} from 'rxjs';

import {
  AlojamientoRespuesta,
  DatosAlojamiento,
  EstadoAlojamiento,
  ImagenAlojamiento,
  PeriodoDisponibilidad,
  PoliticaCancelacion,
  PublicacionHostService
} from '../../../services/publicacion-host.service';

interface FormularioAlojamiento {
  titulo: string;
  ciudad: string;
  descripcion: string;
  ubicacion: string;
  tipo: string;
  capacidad: number | null;
  habitaciones: number | null;
  camas: number | null;
  banos: number | null;
  precioNoche: number | null;
  horaEntrada: string;
  horaSalida: string;
  reglas: string;
  idPolitica: number | null;
  amenidades: string[];
}

interface ImagenPendiente {
  archivo: File;
  url: string;
}

interface PeriodoFormulario {
  clave: number;
  fechaInicio: string;
  fechaFin: string;
  idPeriodo?: number;
}

@Component({
  selector: 'app-update',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './update.html'
})
export class Update implements OnInit, OnDestroy {

  private readonly publicaciones = inject(PublicacionHostService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly id = Number(this.route.snapshot.paramMap.get('id'));

  readonly servicios = [
    'Wifi',
    'Piscina',
    'Parqueadero',
    'Cocina',
    'Aire acondicionado',
    'Lavadora',
    'Televisor',
    'Jacuzzi',
    'Admite mascotas'
  ];

  readonly cargando = signal(true);
  readonly cargaExitosa = signal(false);
  readonly guardando = signal(false);
  readonly trabajandoImagen = signal(false);
  readonly cargandoPoliticas = signal(false);
  readonly cargandoDisponibilidad = signal(false);

  readonly politicas = signal<PoliticaCancelacion[]>([]);
  readonly imagenesGuardadas = signal<ImagenAlojamiento[]>([]);

  readonly error = signal('');
  readonly mensaje = signal('');
  readonly estado = signal<EstadoAlojamiento>('BORRADOR');

  readonly imagenAEliminar = signal<ImagenAlojamiento | null>(null);
  readonly confirmarSalida = signal(false);

  imagenesSeleccionadas: ImagenPendiente[] = [];
  periodos: PeriodoFormulario[] = [];

  private siguienteClave = 0;
  private original = '';
  private salidaPendiente: (() => void) | null = null;

  draft: FormularioAlojamiento = {
    titulo: '',
    ciudad: '',
    descripcion: '',
    ubicacion: '',
    tipo: '',
    capacidad: null,
    habitaciones: null,
    camas: null,
    banos: null,
    precioNoche: null,
    horaEntrada: '',
    horaSalida: '',
    reglas: '',
    idPolitica: null,
    amenidades: []
  };

  // ======================================
  // CARGAR DATOS DESDE SPRING BOOT
  // ======================================

  ngOnInit(): void {

    if (!Number.isInteger(this.id) || this.id <= 0) {
      this.error.set('El identificador del alojamiento es inválido.');
      this.cargando.set(false);
      return;
    }

    this.cargandoPoliticas.set(true);

    this.publicaciones.obtenerPoliticas()
      .pipe(finalize(() => this.cargandoPoliticas.set(false)))
      .subscribe({
        next: politicas => this.politicas.set(politicas),
        error: () => {
          this.error.set(
            'No se pudieron cargar las políticas de cancelación.'
          );
        }
      });

    this.publicaciones.obtenerAlojamiento(this.id)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: alojamiento => {
          this.cargarFormulario(alojamiento);
          this.cargaExitosa.set(true);
          this.cargarDisponibilidad();
        },
        error: err => this.error.set(this.mensajeError(err))
      });
  }

  ngOnDestroy(): void {
    this.liberarVistasPrevias();
  }

  private cargarFormulario(a: AlojamientoRespuesta): void {

    this.estado.set(a.estado);

    this.draft = {
      titulo: a.titulo ?? '',
      ciudad: a.ciudad ?? '',
      descripcion: a.descripcion ?? '',
      ubicacion: a.ubicacion ?? '',
      tipo: a.tipo ?? '',
      capacidad: a.capacidad ?? null,
      habitaciones: a.habitaciones ?? null,
      camas: a.camas ?? null,
      banos: a.banos ?? null,
      precioNoche: a.precioNoche ?? null,
      horaEntrada: a.horaEntrada?.slice(0, 5) ?? '',
      horaSalida: a.horaSalida?.slice(0, 5) ?? '',
      reglas: a.reglas ?? '',
      idPolitica: a.idPolitica ?? null,
      amenidades: [...(a.amenidades ?? [])]
    };

    this.imagenesGuardadas.set(a.detalleImagenes ?? []);
    this.original = JSON.stringify(this.draft);
  }

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

  claseEstado(): string {
    const clases: Record<EstadoAlojamiento, string> = {
      BORRADOR: 'bg-slate-100 text-slate-700',
      PENDIENTE_REVISION: 'bg-amber-50 text-amber-700',
      ACTIVA: 'bg-emerald-50 text-emerald-700',
      PAUSADA: 'bg-blue-50 text-blue-700',
      BLOQUEADA: 'bg-red-50 text-red-700'
    };

    return 'rounded-full px-3 py-1 text-xs font-bold ' +
      clases[this.estado()];
  }

  get fechaMinima(): string {
    const hoy = new Date();

    return [
      hoy.getFullYear(),
      String(hoy.getMonth() + 1).padStart(2, '0'),
      String(hoy.getDate()).padStart(2, '0')
    ].join('-');
  }

  get hayCambios(): boolean {
    return (
      JSON.stringify(this.draft) !== this.original ||
      this.imagenesSeleccionadas.length > 0 ||
      this.periodos.some(p => p.idPeriodo === undefined)
    );
  }

  // ======================================
  // AMENIDADES
  // ======================================

  toggleService(servicio: string): void {
    const tiene = this.tieneServicio(servicio);

    this.draft.amenidades = tiene
      ? this.draft.amenidades.filter(
        x => x.toLowerCase() !== servicio.toLowerCase()
      )
      : [...this.draft.amenidades, servicio];
  }

  tieneServicio(servicio: string): boolean {
    return this.draft.amenidades.some(
      x => x.toLowerCase() === servicio.toLowerCase()
    );
  }

  // ======================================
  // NUEVAS FOTOGRAFÍAS
  // ======================================

  seleccionarImagenes(evento: Event): void {

    const input = evento.target as HTMLInputElement;
    const nuevos = Array.from(input.files ?? []);

    input.value = '';

    if (!nuevos.length) return;

    const total = this.imagenesGuardadas().length +
      this.imagenesSeleccionadas.length + nuevos.length;

    if (total > 15) {
      this.error.set('Puedes tener máximo 15 fotografías.');
      return;
    }

    const invalido = nuevos.find(
      f =>
        !['image/jpeg', 'image/png', 'image/webp'].includes(f.type) ||
        f.size === 0 ||
        f.size > 5 * 1024 * 1024
    );

    if (invalido) {
      this.error.set(
        `La imagen "${invalido.name}" debe ser JPG, PNG o WEBP y no superar 5 MB.`
      );
      return;
    }

    this.error.set('');

    this.imagenesSeleccionadas = [
      ...this.imagenesSeleccionadas,
      ...nuevos.map(archivo => ({
        archivo,
        url: URL.createObjectURL(archivo)
      }))
    ];
  }

  quitarImagen(indice: number): void {
    const imagen = this.imagenesSeleccionadas[indice];

    if (!imagen) return;

    URL.revokeObjectURL(imagen.url);

    this.imagenesSeleccionadas =
      this.imagenesSeleccionadas.filter((_, i) => i !== indice);
  }

  private liberarVistasPrevias(): void {
    for (const item of this.imagenesSeleccionadas) {
      URL.revokeObjectURL(item.url);
    }

    this.imagenesSeleccionadas = [];
  }

  marcarPrincipal(imagen: ImagenAlojamiento): void {

    if (
      this.trabajandoImagen() ||
      this.guardando() ||
      imagen.principal
    ) return;

    this.trabajandoImagen.set(true);
    this.error.set('');

    this.publicaciones
      .marcarImagenPrincipal(this.id, imagen.idImagen)
      .pipe(finalize(() => this.trabajandoImagen.set(false)))
      .subscribe({
        next: imagenes => {
          this.imagenesGuardadas.set(imagenes);
          this.mensaje.set('Fotografía principal actualizada.');
        },
        error: err => this.error.set(this.mensajeError(err))
      });
  }

  eliminarImagen(): void {

    const imagen = this.imagenAEliminar();

    if (
      !imagen ||
      this.trabajandoImagen() ||
      this.guardando()
    ) return;

    this.imagenAEliminar.set(null);

    if (this.imagenesGuardadas().length <= 1) {
      this.error.set(
        'Debes conservar al menos una fotografía.'
      );
      return;
    }

    this.trabajandoImagen.set(true);
    this.error.set('');

    this.publicaciones
      .eliminarImagen(this.id, imagen.idImagen)
      .pipe(finalize(() => this.trabajandoImagen.set(false)))
      .subscribe({
        next: imagenes => {
          this.imagenesGuardadas.set(imagenes);
          this.mensaje.set('Fotografía eliminada correctamente.');
        },
        error: err => this.error.set(this.mensajeError(err))
      });
  }

  // ======================================
  // DISPONIBILIDAD
  // ======================================

  private cargarDisponibilidad(): void {

    this.cargandoDisponibilidad.set(true);

    this.publicaciones.obtenerDisponibilidad(this.id)
      .pipe(finalize(() => this.cargandoDisponibilidad.set(false)))
      .subscribe({
        next: calendario => {
          this.periodos = calendario.periodos.map(
            (p: PeriodoDisponibilidad) => ({
              clave: ++this.siguienteClave,
              idPeriodo: p.idPeriodo,
              fechaInicio: p.fechaInicio,
              fechaFin: p.fechaFin
            })
          );
        },
        error: () => {
          this.error.set(
            'No se pudo consultar la disponibilidad. ' +
            'Puedes seguir editando el alojamiento.'
          );
        }
      });
  }

  agregarPeriodo(): void {
    this.periodos = [
      ...this.periodos,
      {
        clave: ++this.siguienteClave,
        fechaInicio: '',
        fechaFin: ''
      }
    ];
  }

  quitarPeriodo(clave: number): void {
    this.periodos = this.periodos.filter(
      p => p.clave !== clave || p.idPeriodo !== undefined
    );
  }

  verDisponibilidad(): void {
    this.salir(() =>
      void this.router.navigate([
        '/host/alojamientos',
        this.id,
        'disponibilidad'
      ])
    );
  }

  // ======================================
  // VALIDACIÓN
  // ======================================

  private validar(): string | null {

    const d = this.draft;

    if (!d.titulo.trim()) {
      return 'Escribe el título del alojamiento.';
    }

    if (
      d.capacidad !== null &&
      (!Number.isInteger(d.capacidad) || d.capacidad <= 0)
    ) {
      return 'La capacidad debe ser mayor que cero.';
    }

    for (const [nombre, valor] of [
      ['habitaciones', d.habitaciones],
      ['camas', d.camas],
      ['baños', d.banos]
    ] as const) {
      if (
        valor !== null &&
        (!Number.isInteger(valor) || valor < 0)
      ) {
        return `La cantidad de ${nombre} debe ser válida.`;
      }
    }

    if (
      d.precioNoche !== null &&
      d.precioNoche <= 0
    ) {
      return 'El precio por noche debe ser mayor que cero.';
    }

    for (const p of this.periodos.filter(
      x => x.idPeriodo === undefined
    )) {

      if (!p.fechaInicio || !p.fechaFin) {
        return 'Completa las fechas de los períodos nuevos.';
      }

      if (p.fechaInicio < this.fechaMinima) {
        return 'No puedes agregar disponibilidad en el pasado.';
      }

      if (p.fechaFin <= p.fechaInicio) {
        return 'La fecha final debe ser posterior a la inicial.';
      }

      const superpuesto = this.periodos.some(
        otro =>
          otro.clave !== p.clave &&
          p.fechaInicio <= otro.fechaFin &&
          otro.fechaInicio <= p.fechaFin
      );

      if (superpuesto) {
        return 'Los períodos no pueden superponerse.';
      }
    }

    return null;
  }

  private construirDatos(): DatosAlojamiento {

    const d = this.draft;

    return {
      titulo: d.titulo.trim(),
      descripcion: d.descripcion.trim(),
      ubicacion: d.ubicacion.trim(),
      ciudad: d.ciudad.trim(),
      tipo: d.tipo.trim(),

      ...(d.capacidad !== null
        ? { capacidad: d.capacidad } : {}),

      ...(d.habitaciones !== null
        ? { habitaciones: d.habitaciones } : {}),

      ...(d.camas !== null
        ? { camas: d.camas } : {}),

      ...(d.banos !== null
        ? { banos: d.banos } : {}),

      ...(d.precioNoche !== null
        ? { precioNoche: d.precioNoche } : {}),

      ...(d.idPolitica !== null
        ? { idPolitica: d.idPolitica } : {}),

      ...(d.horaEntrada
        ? { horaEntrada: d.horaEntrada } : {}),

      ...(d.horaSalida
        ? { horaSalida: d.horaSalida } : {}),

      reglas: d.reglas.trim(),
      amenidades: [...d.amenidades]
    };
  }

  // ======================================
  // GUARDAR FOTOGRAFÍAS
  // ======================================

  private guardarNuevasImagenes(): Observable<void> {

    const archivos = this.imagenesSeleccionadas.map(
      p => p.archivo
    );

    if (!archivos.length) {
      return of(undefined);
    }

    return this.publicaciones
      .agregarImagenes(this.id, archivos)
      .pipe(
        tap(imagenes => {
          this.imagenesGuardadas.set(imagenes);
          this.liberarVistasPrevias();
        }),
        map(() => undefined)
      );
  }

  // ======================================
  // GUARDAR PERÍODOS
  // ======================================

  private guardarNuevosPeriodos(): Observable<void> {

    const nuevos = this.periodos.filter(
      p => p.idPeriodo === undefined
    );

    if (!nuevos.length) {
      return of(undefined);
    }

    return from(nuevos).pipe(
      concatMap(periodo =>
        this.publicaciones
          .agregarPeriodoDisponibilidad(this.id, {
            fechaInicio: periodo.fechaInicio,
            fechaFin: periodo.fechaFin
          })
          .pipe(
            tap(guardado =>
              periodo.idPeriodo = guardado.idPeriodo
            )
          )
      ),
      toArray(),
      map(() => undefined)
    );
  }

  // ======================================
  // GUARDAR CAMBIOS DEL ALOJAMIENTO
  // ======================================

  guardar(form: NgForm): void {

    if (
      this.guardando() ||
      this.cargando() ||
      this.trabajandoImagen() ||
      this.estado() === 'BLOQUEADA'
    ) return;

    this.error.set('');
    this.mensaje.set('');

    const problema = this.validar();

    if (problema) {
      this.error.set(problema);
      return;
    }

    this.guardando.set(true);

    this.publicaciones
      .actualizarBorrador(
        this.id,
        this.construirDatos()
      )
      .pipe(
        tap(a => {
          this.original = JSON.stringify(this.draft);
          this.estado.set(a.estado);
        }),
        switchMap(() => this.guardarNuevasImagenes()),
        switchMap(() => this.guardarNuevosPeriodos()),
        finalize(() => this.guardando.set(false))
      )
      .subscribe({
        next: () => {
          this.mensaje.set(
            'Los cambios se guardaron correctamente.'
          );
          form.form.markAsPristine();
        },
        error: err => {
          this.error.set(
            this.mensajeError(err) +
            ' Algunos cambios anteriores podrían haberse guardado.'
          );
        }
      });
  }

  // ======================================
  // NAVEGACIÓN
  // ======================================

  private salir(accion: () => void): void {

    if (this.hayCambios) {
      this.salidaPendiente = accion;
      this.confirmarSalida.set(true);
    } else {
      accion();
    }
  }

  cancelar(): void {
    this.salir(() =>
      void this.router.navigateByUrl('/host/alojamientos')
    );
  }

  verDetalles(): void {
    this.salir(() =>
      void this.router.navigate([
        '/host/alojamientos',
        this.id
      ])
    );
  }

  salirSinGuardar(): void {

    this.confirmarSalida.set(false);

    const accion = this.salidaPendiente;
    this.salidaPendiente = null;

    accion?.();
  }

  // ======================================
  // MANEJO DE ERRORES
  // ======================================

  private mensajeError(err: unknown): string {

    if (err instanceof HttpErrorResponse) {

      if (err.status === 0) {
        return 'No se pudo conectar con Spring Boot en localhost:8081.';
      }

      if (err.status === 401) {
        return 'La sesión expiró. Inicia sesión nuevamente.';
      }

      if (err.status === 403) {
        return 'No tienes permisos para editar este alojamiento.';
      }

      if (err.status === 404) {
        return 'El alojamiento solicitado no existe.';
      }

      if (typeof err.error?.message === 'string') {
        return err.error.message;
      }

      return `Error del servidor (HTTP ${err.status}).`;
    }

    return 'No se pudo completar la operación.';
  }
}
