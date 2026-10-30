
import {
  Component,
  OnInit,
  inject,
  signal
} from '@angular/core';

import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

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
  PoliticaCancelacion,
  PublicacionHostService
} from '../../../services/publicacion-host.service';

// ======================================
// INTERFAZ DEL FORMULARIO
// ======================================

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

// ======================================
// INTERFAZ DE DISPONIBILIDAD
// ======================================

interface PeriodoFormulario {
  clave: number;
  fechaInicio: string;
  fechaFin: string;
  idPeriodo?: number;
}

// ======================================
// COMPONENTE
// ======================================

@Component({
  selector: 'app-create-listing',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './create-listing.html'
})
export class CreateListing implements OnInit {

  // ======================================
  // SERVICIOS
  // ======================================

  private readonly publicaciones =
    inject(PublicacionHostService);

  private readonly router = inject(Router);

  // ======================================
  // SERVICIOS INCLUIDOS
  // ======================================

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

  // ======================================
  // FORMULARIO DEL ALOJAMIENTO
  // ======================================

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
  // ESTADOS
  // ======================================

  readonly politicas =
    signal<PoliticaCancelacion[]>([]);

  readonly cargandoPoliticas =
    signal(false);

  readonly borradorId =
    signal<number | null>(null);

  readonly imagenesGuardadas =
    signal(0);

  readonly guardando =
    signal(false);

  readonly error =
    signal('');

  readonly mensaje =
    signal('');

  // ======================================
  // IMÁGENES
  // ======================================

  imagenesSeleccionadas: File[] = [];

  // ======================================
  // FECHAS DE DISPONIBILIDAD
  // ======================================

  periodos: PeriodoFormulario[] = [];

  private siguienteClave = 0;

  // Fecha local actual en formato YYYY-MM-DD.
  // Evita problemas de zona horaria con UTC.

  get fechaMinima(): string {

    const hoy = new Date();

    const anio = hoy.getFullYear();

    const mes = String(
      hoy.getMonth() + 1
    ).padStart(2, '0');

    const dia = String(
      hoy.getDate()
    ).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }

  // ======================================
  // 1. INICIALIZAR COMPONENTE
  // ======================================

  ngOnInit(): void {

    this.cargandoPoliticas.set(true);

    this.publicaciones.obtenerPoliticas()
      .pipe(
        finalize(() => {
          this.cargandoPoliticas.set(false);
        })
      )
      .subscribe({

        next: politicas => {
          this.politicas.set(politicas);
        },

        error: err => {
          this.error.set(
            this.mensajeError(err)
          );
        }
      });
  }

  // ======================================
  // 2. SELECCIONAR SERVICIOS
  // ======================================

  toggleService(servicio: string): void {

    const seleccionados =
      this.draft.amenidades;

    if (seleccionados.includes(servicio)) {

      this.draft.amenidades =
        seleccionados.filter(
          s => s !== servicio
        );

    } else {

      this.draft.amenidades = [
        ...seleccionados,
        servicio
      ];
    }
  }

  // ======================================
  // 3. SELECCIONAR FOTOGRAFÍAS
  // ======================================

  seleccionarImagenes(evento: Event): void {

    const input =
      evento.target as HTMLInputElement;

    const nuevas =
      Array.from(input.files ?? []);

    input.value = '';

    if (nuevas.length === 0) {
      return;
    }

    const tiposPermitidos = [
      'image/jpeg',
      'image/png',
      'image/webp'
    ];

    const archivoInvalido = nuevas.find(
      archivo =>
        !tiposPermitidos.includes(archivo.type) ||
        archivo.size === 0 ||
        archivo.size > 5 * 1024 * 1024
    );

    if (archivoInvalido) {

      this.error.set(
        `La fotografía ${archivoInvalido.name} ` +
        'no es válida. Usa JPG, PNG o WEBP ' +
        'de máximo 5 MB.'
      );

      return;
    }

    const cantidadTotal =
      this.imagenesGuardadas() +
      this.imagenesSeleccionadas.length +
      nuevas.length;

    if (cantidadTotal > 15) {

      this.error.set(
        'Puedes cargar máximo 15 fotografías por alojamiento.'
      );

      return;
    }

    this.imagenesSeleccionadas = [
      ...this.imagenesSeleccionadas,
      ...nuevas
    ];

    this.error.set('');
  }

  // ======================================
  // 4. QUITAR FOTOGRAFÍA
  // ======================================

  quitarImagen(indice: number): void {

    this.imagenesSeleccionadas =
      this.imagenesSeleccionadas.filter(
        (_, i) => i !== indice
      );
  }

  // ======================================
  // 5. AGREGAR PERÍODO DISPONIBLE
  // ======================================

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

  // ======================================
  // 6. QUITAR PERÍODO
  // ======================================

  quitarPeriodo(clave: number): void {

    // Los períodos guardados en Spring Boot
    // se administran desde Ver disponibilidad.

    this.periodos = this.periodos.filter(
      periodo =>
        periodo.clave !== clave ||
        periodo.idPeriodo !== undefined
    );
  }

  // ======================================
  // 7. GUARDAR BORRADOR
  // ======================================

  saveDraft(): void {
    this.guardar(false);
  }

  // ======================================
  // 8. ENVIAR A REVISIÓN
  // ======================================

  submit(): void {
    this.guardar(true);
  }

  // ======================================
  // 9. PROCESO PRINCIPAL DE GUARDADO
  // ======================================

  private guardar(
    enviarARevision: boolean
  ): void {

    if (this.guardando()) {
      return;
    }

    this.error.set('');
    this.mensaje.set('');

    // Validar información.
    const problema =
      this.validar(enviarARevision);

    if (problema) {

      this.error.set(problema);

      return;
    }

    this.guardando.set(true);

    const datos =
      this.construirDatos();

    // PASO 1:
    // Crear o actualizar alojamiento.

    const datosGuardados$ =
      this.guardarDatos(datos);

    datosGuardados$
      .pipe(

        // PASO 2:
        // Guardar períodos disponibles.

        switchMap(alojamiento =>

          this.guardarPeriodos(
            alojamiento.idAlojamiento
          ).pipe(

            map(() => alojamiento)
          )
        ),

        // PASO 3:
        // Enviar a revisión, si corresponde.

        switchMap(alojamiento => {

          if (!enviarARevision) {
            return of(alojamiento);
          }

          return this.publicaciones
            .enviarRevision(
              alojamiento.idAlojamiento
            );
        }),

        finalize(() => {
          this.guardando.set(false);
        })
      )
      .subscribe({

        next: alojamiento => {

          if (enviarARevision) {

            this.mensaje.set(
              'El alojamiento fue enviado a revisión correctamente.'
            );

            void this.router.navigateByUrl(
              '/host/alojamientos'
            );

          } else {

            this.mensaje.set(
              `Borrador #${alojamiento.idAlojamiento} ` +
              'y fechas guardados correctamente.'
            );
          }
        },

        error: err => {

          const id =
            this.borradorId();

          let texto =
            this.mensajeError(err);

          if (id !== null) {

            texto +=
              ` El borrador #${id} se conserva. ` +
              'Revisa los datos y las fechas ' +
              'antes de volver a intentarlo.';
          }

          this.error.set(texto);
        }
      });
  }

  // ======================================
  // 10. CREAR O ACTUALIZAR ALOJAMIENTO
  // ======================================

  private guardarDatos(
    datos: DatosAlojamiento
  ): Observable<AlojamientoRespuesta> {

    const id =
      this.borradorId();

    const archivos = [
      ...this.imagenesSeleccionadas
    ];

    // CREAR UN NUEVO BORRADOR.

    if (id === null) {

      return this.publicaciones
        .crearBorrador(
          datos,
          archivos
        )
        .pipe(

          tap(alojamiento => {

            this.borradorId.set(
              alojamiento.idAlojamiento
            );

            const totalImagenes =
              alojamiento.detalleImagenes?.length ??
              alojamiento.imagenes?.length ??
              archivos.length;

            this.imagenesGuardadas.set(
              totalImagenes
            );

            this.imagenesSeleccionadas = [];
          })
        );
    }

    // ACTUALIZAR BORRADOR EXISTENTE.

    return this.publicaciones
      .actualizarBorrador(id, datos)
      .pipe(

        switchMap(alojamiento => {

          if (archivos.length === 0) {
            return of(alojamiento);
          }

          return this.publicaciones
            .agregarImagenes(
              id,
              archivos
            )
            .pipe(

              tap(imagenes => {

                this.imagenesGuardadas.set(
                  imagenes.length
                );

                this.imagenesSeleccionadas = [];
              }),

              map(() => alojamiento)
            );
        })
      );
  }

  // ======================================
  // 11. GUARDAR FECHAS DE DISPONIBILIDAD
  // ======================================

  private guardarPeriodos(
    idAlojamiento: number
  ): Observable<void> {

    // Solo enviar los períodos nuevos.
    // Los existentes no se duplican.

    const nuevos = this.periodos.filter(
      periodo =>
        periodo.idPeriodo === undefined
    );

    if (nuevos.length === 0) {
      return of(undefined);
    }

    // Se guardan uno por uno.
    // Si alguno falla, los anteriores
    // conservan su identificador.

    return from(nuevos)
      .pipe(

        concatMap(periodo =>

          this.publicaciones
            .agregarPeriodoDisponibilidad(
              idAlojamiento,
              {
                fechaInicio: periodo.fechaInicio,
                fechaFin: periodo.fechaFin
              }
            )
            .pipe(

              tap(guardado => {

                periodo.idPeriodo =
                  guardado.idPeriodo;
              })
            )
        ),

        toArray(),

        map(() => undefined)
      );
  }

  // ======================================
  // 12. VALIDAR FORMULARIO
  // ======================================

  private validar(
    enviarARevision: boolean
  ): string | null {

    const d = this.draft;

    // Título obligatorio.
    if (!d.titulo.trim()) {

      return (
        'Debes indicar el título del alojamiento.'
      );
    }

    // Para crear el alojamiento, el backend
    // exige al menos una fotografía.

    if (
      this.borradorId() === null &&
      this.imagenesSeleccionadas.length === 0
    ) {

      return (
        'Debes seleccionar al menos una fotografía ' +
        'para crear el alojamiento.'
      );
    }

    // Capacidad.
    if (
      d.capacidad !== null &&
      (
        !Number.isInteger(d.capacidad) ||
        d.capacidad <= 0
      )
    ) {

      return (
        'La capacidad debe ser un número ' +
        'entero mayor que cero.'
      );
    }

    // Habitaciones, camas y baños.
    for (const [nombre, valor] of [
      ['habitaciones', d.habitaciones],
      ['camas', d.camas],
      ['baños', d.banos]
    ] as const) {

      if (
        valor !== null &&
        (
          !Number.isInteger(valor) ||
          valor < 0
        )
      ) {

        return (
          `La cantidad de ${nombre} ` +
          'debe ser un entero no negativo.'
        );
      }
    }

    // Precio por noche.
    if (
      d.precioNoche !== null &&
      !(d.precioNoche > 0)
    ) {

      return (
        'El precio por noche debe ser mayor que cero.'
      );
    }

    // ====================================
    // VALIDAR FECHAS
    // ====================================

    for (const periodo of this.periodos) {

      if (
        !periodo.fechaInicio ||
        !periodo.fechaFin
      ) {

        return (
          'Completa la fecha de inicio y fin ' +
          'de cada período o elimina ' +
          'el período vacío.'
        );
      }

      if (
        periodo.fechaInicio < this.fechaMinima
      ) {

        return (
          'La fecha inicial de cada período ' +
          'debe ser hoy o posterior.'
        );
      }

      if (
        periodo.fechaFin <=
        periodo.fechaInicio
      ) {

        return (
          'La fecha final debe ser posterior ' +
          'a la inicial. Debe existir ' +
          'al menos una noche disponible.'
        );
      }
    }

    // ====================================
    // VALIDAR FECHAS SUPERPUESTAS
    // ====================================

    for (
      let i = 0;
      i < this.periodos.length;
      i++
    ) {

      for (
        let j = i + 1;
        j < this.periodos.length;
        j++
      ) {

        const a = this.periodos[i];

        const b = this.periodos[j];

        // Consideramos cruce incluso cuando
        // ambos períodos comparten un límite.

        const seCruzan =
          a.fechaInicio <= b.fechaFin &&
          b.fechaInicio <= a.fechaFin;

        if (seCruzan) {

          return (
            'Dos períodos de disponibilidad ' +
            'se cruzan. Ajusta las fechas ' +
            'para que no se superpongan.'
          );
        }
      }
    }

    // Guardar borrador permite
    // datos y fechas incompletos,
    // siempre que los períodos agregados
    // tengan fechas válidas.

    if (!enviarARevision) {
      return null;
    }

    // ====================================
    // AL ENVIAR A REVISIÓN
    // ====================================

    if (this.periodos.length === 0) {

      return (
        'Para enviar a revisión debes agregar ' +
        'al menos un período de disponibilidad.'
      );
    }

    const faltantes: string[] = [];

    if (!d.descripcion.trim()) {
      faltantes.push('descripción');
    }

    if (!d.ubicacion.trim()) {
      faltantes.push('ubicación');
    }

    if (!d.ciudad.trim()) {
      faltantes.push('ciudad');
    }

    if (!d.tipo.trim()) {
      faltantes.push('tipo de alojamiento');
    }

    if (d.capacidad === null) {
      faltantes.push('capacidad');
    }

    if (d.habitaciones === null) {
      faltantes.push('habitaciones');
    }

    if (d.banos === null) {
      faltantes.push('baños');
    }

    if (d.precioNoche === null) {
      faltantes.push('precio por noche');
    }

    if (!d.horaEntrada) {
      faltantes.push('hora de entrada');
    }

    if (!d.horaSalida) {
      faltantes.push('hora de salida');
    }

    if (!d.reglas.trim()) {
      faltantes.push('reglas');
    }

    if (d.idPolitica === null) {
      faltantes.push('política de cancelación');
    }

    if (faltantes.length > 0) {

      return (
        'Para enviar a revisión falta: ' +
        faltantes.join(', ') +
        '.'
      );
    }

    return null;
  }

  // ======================================
  // 13. CONSTRUIR DATOS PARA EL BACKEND
  // ======================================

  private construirDatos(): DatosAlojamiento {

    const d = this.draft;

    return {

      titulo: d.titulo.trim(),

      descripcion: d.descripcion.trim(),

      ubicacion: d.ubicacion.trim(),

      ciudad: d.ciudad.trim(),

      tipo: d.tipo.trim(),

      ...(d.capacidad !== null
        ? { capacidad: d.capacidad }
        : {}),

      ...(d.habitaciones !== null
        ? { habitaciones: d.habitaciones }
        : {}),

      ...(d.camas !== null
        ? { camas: d.camas }
        : {}),

      ...(d.banos !== null
        ? { banos: d.banos }
        : {}),

      ...(d.precioNoche !== null
        ? { precioNoche: d.precioNoche }
        : {}),

      ...(d.idPolitica !== null
        ? { idPolitica: d.idPolitica }
        : {}),

      ...(d.horaEntrada
        ? { horaEntrada: d.horaEntrada }
        : {}),

      ...(d.horaSalida
        ? { horaSalida: d.horaSalida }
        : {}),

      reglas: d.reglas.trim(),

      amenidades: [
        ...d.amenidades
      ]
    };
  }

  // ======================================
  // 14. MENSAJES DE ERROR
  // ======================================

  private mensajeError(
    err: unknown
  ): string {

    if (err instanceof HttpErrorResponse) {

      if (err.status === 0) {

        return (
          'No se pudo conectar con Spring Boot ' +
          'en localhost:8081.'
        );
      }

      if (err.status === 401) {

        return (
          'Tu sesión expiró. ' +
          'Inicia sesión nuevamente.'
        );
      }

      if (err.status === 403) {

        return (
          'No tienes permisos para realizar ' +
          'esta operación como anfitrión.'
        );
      }

      if (
        err.error &&
        typeof err.error.message === 'string'
      ) {

        return err.error.message;
      }

      return (
        `Ocurrió un error en el servidor ` +
        `(HTTP ${err.status}).`
      );
    }

    return (
      'No se pudo completar la operación. ' +
      'Revisa la conexión e inténtalo nuevamente.'
    );
  }
}
