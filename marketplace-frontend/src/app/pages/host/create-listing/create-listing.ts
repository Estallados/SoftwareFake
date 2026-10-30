
import {
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  signal
} from '@angular/core';

import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import {
  Observable,
  finalize,
  map,
  of,
  switchMap,
  tap,
  timeout
} from 'rxjs';

import { SessionService } from '../../../services/session.service';

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

  private readonly sesion =
    inject(SessionService);

  private readonly router =
    inject(Router);

  @ViewChild('fotoInput')
  fotoInput?: ElementRef<HTMLInputElement>;

  // ======================================
  // SERVICIOS INCLUIDOS
  // ======================================

  readonly servicios: string[] = [
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
  // FORMULARIO
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
  // IMÁGENES
  // ======================================

  imagenesSeleccionadas: File[] = [];

  readonly imagenesGuardadas = signal(0);

  // ID del borrador creado en Spring Boot.
  readonly borradorId = signal<number | null>(null);

  // ======================================
  // POLÍTICAS DE CANCELACIÓN
  // ======================================

  readonly politicas =
    signal<PoliticaCancelacion[]>([]);

  readonly cargandoPoliticas = signal(false);

  // ======================================
  // ESTADOS DE LA PÁGINA
  // ======================================

  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');

  // ======================================
  // 1. INICIALIZAR
  // ======================================

  ngOnInit(): void {

    // Solo los anfitriones pueden crear.
    if (
      !this.sesion.autenticado() ||
      !this.sesion.tieneRol('Anfitrión')
    ) {
      void this.router.navigateByUrl('/home');
      return;
    }

    this.cargarPoliticas();
  }

  // ======================================
  // 2. CONSULTAR POLÍTICAS
  // ======================================

  private cargarPoliticas(): void {

    this.cargandoPoliticas.set(true);

    this.publicaciones.obtenerPoliticas()
      .pipe(
        timeout(15000),
        finalize(() => {
          this.cargandoPoliticas.set(false);
        })
      )
      .subscribe({

        next: (respuesta) => {
          this.politicas.set(respuesta);
        },

        error: (err: unknown) => {
          this.error.set(
            this.mensajeError(
              err,
              'No se pudieron cargar las políticas de cancelación.'
            )
          );
        }
      });
  }

  // ======================================
  // 3. SELECCIONAR SERVICIOS
  // ======================================

  toggleService(servicio: string): void {

    const actuales = this.draft.amenidades;

    this.draft.amenidades =
      actuales.includes(servicio)
        ? actuales.filter(s => s !== servicio)
        : [...actuales, servicio];
  }

  // ======================================
  // 4. SELECCIONAR FOTOGRAFÍAS
  // ======================================

  seleccionarImagenes(evento: Event): void {

    const input =
      evento.target as HTMLInputElement;

    const archivos =
      Array.from(input.files ?? []);

    if (archivos.length === 0) {
      return;
    }

    const total =
      this.imagenesGuardadas() +
      this.imagenesSeleccionadas.length +
      archivos.length;

    if (total > 15) {
      this.error.set(
        'Solo puedes agregar un máximo de 15 fotografías por alojamiento.'
      );

      input.value = '';
      return;
    }

    const tiposValidos = [
      'image/jpeg',
      'image/png',
      'image/webp'
    ];

    const archivoInvalido = archivos.find(
      archivo =>
        !tiposValidos.includes(archivo.type) ||
        archivo.size === 0 ||
        archivo.size > 5 * 1024 * 1024
    );

    if (archivoInvalido) {

      this.error.set(
        `El archivo ${archivoInvalido.name} no es válido. ` +
        'Solo se permiten JPG, PNG o WEBP de máximo 5 MB.'
      );

      input.value = '';
      return;
    }

    this.imagenesSeleccionadas = [
      ...this.imagenesSeleccionadas,
      ...archivos
    ];

    this.error.set('');

    // Permite seleccionar más fotografías.
    input.value = '';
  }

  // ======================================
  // 5. QUITAR FOTOGRAFÍA SELECCIONADA
  // ======================================

  quitarImagen(indice: number): void {

    this.imagenesSeleccionadas =
      this.imagenesSeleccionadas.filter(
        (_, i) => i !== indice
      );
  }

  // ======================================
  // 6. GUARDAR BORRADOR
  // ======================================

  saveDraft(): void {
    this.guardar(false);
  }

  // ======================================
  // 7. ENVIAR A REVISIÓN
  // ======================================

  submit(): void {
    this.guardar(true);
  }

  // ======================================
  // 8. GUARDAR DATOS EN SPRING BOOT
  // ======================================

  private guardar(
    enviarRevision: boolean
  ): void {

    if (this.guardando()) {
      return;
    }

    this.error.set('');
    this.mensaje.set('');

    const errorValidacion =
      this.validar(enviarRevision);

    if (errorValidacion) {
      this.error.set(errorValidacion);
      return;
    }

    const datos = this.construirDatos();

    const id = this.borradorId();

    const fotos = [
      ...this.imagenesSeleccionadas
    ];

    let guardar$: Observable<AlojamientoRespuesta>;

    // ====================================
    // CREAR UN NUEVO BORRADOR
    // ====================================

    if (id === null) {

      guardar$ = this.publicaciones
        .crearBorrador(datos, fotos)
        .pipe(

          tap(creado => {

            this.borradorId.set(
              creado.idAlojamiento
            );

            const cantidad =
              creado.detalleImagenes?.length ??
              creado.imagenes?.length ??
              fotos.length;

            this.imagenesGuardadas.set(
              cantidad
            );

            this.limpiarSeleccionImagenes();
          })
        );

    } else {

      // ====================================
      // ACTUALIZAR BORRADOR EXISTENTE
      // ====================================

      guardar$ = this.publicaciones
        .actualizarBorrador(id, datos)
        .pipe(

          switchMap(actualizado => {

            if (fotos.length === 0) {
              return of(actualizado);
            }

            // Subir nuevas fotografías.
            return this.publicaciones
              .agregarImagenes(id, fotos)
              .pipe(

                tap(imagenes => {

                  this.imagenesGuardadas.set(
                    imagenes.length
                  );

                  this.limpiarSeleccionImagenes();
                }),

                map(() => actualizado)
              );
          })
        );
    }

    // ====================================
    // OPCIONALMENTE ENVIAR A REVISIÓN
    // ====================================

    const operacion$ = enviarRevision
      ? guardar$.pipe(
        switchMap(resultado =>
          this.publicaciones.enviarRevision(
            resultado.idAlojamiento
          )
        )
      )
      : guardar$;

    this.guardando.set(true);

    operacion$
      .pipe(
        timeout(30000),

        finalize(() => {
          this.guardando.set(false);
        })
      )
      .subscribe({

        next: (resultado) => {

          if (enviarRevision) {

            this.mensaje.set(
              'Alojamiento enviado a revisión correctamente.'
            );

            void this.router.navigateByUrl(
              '/host/alojamientos'
            );

          } else {

            this.mensaje.set(
              `Borrador #${resultado.idAlojamiento} ` +
              'guardado correctamente.'
            );
          }
        },

        error: (err: unknown) => {

          const detalle = this.mensajeError(
            err,
            'No se pudo guardar el alojamiento.'
          );

          if (
            enviarRevision &&
            this.borradorId() !== null
          ) {

            this.error.set(
              `${detalle} ` +
              `El borrador #${this.borradorId()} ` +
              'se conserva. Puedes corregirlo y reintentar.'
            );

          } else {

            this.error.set(detalle);
          }
        }
      });
  }

  // ======================================
  // 9. VALIDAR CAMPOS
  // ======================================

  private validar(
    enviarRevision: boolean
  ): string | null {

    if (!this.draft.titulo.trim()) {
      return 'El título del alojamiento es obligatorio.';
    }

    if (
      this.borradorId() === null &&
      this.imagenesSeleccionadas.length === 0
    ) {
      return 'Debes agregar al menos una fotografía para crear el alojamiento.';
    }

    if (
      this.imagenesGuardadas() +
      this.imagenesSeleccionadas.length > 15
    ) {
      return 'El alojamiento admite máximo 15 fotografías.';
    }

    if (
      this.draft.capacidad !== null &&
      (
        !Number.isInteger(this.draft.capacidad) ||
        this.draft.capacidad <= 0
      )
    ) {
      return 'La capacidad debe ser un entero mayor que cero.';
    }

    for (const [campo, cantidad] of [
      ['habitaciones', this.draft.habitaciones],
      ['camas', this.draft.camas],
      ['baños', this.draft.banos]
    ] as const) {

      if (
        cantidad !== null &&
        (
          !Number.isInteger(cantidad) ||
          cantidad < 0
        )
      ) {
        return `El número de ${campo} no puede ser negativo.`;
      }
    }

    if (
      this.draft.precioNoche !== null &&
      this.draft.precioNoche <= 0
    ) {
      return 'El precio por noche debe ser mayor que cero.';
    }

    // Un borrador puede estar incompleto.
    if (!enviarRevision) {
      return null;
    }

    const campos: Array<[string, boolean]> = [
      ['descripción', !!this.draft.descripcion.trim()],
      ['ubicación', !!this.draft.ubicacion.trim()],
      ['ciudad', !!this.draft.ciudad.trim()],
      ['tipo de alojamiento', !!this.draft.tipo.trim()],
      ['capacidad', this.draft.capacidad !== null],
      ['habitaciones', this.draft.habitaciones !== null],
      ['baños', this.draft.banos !== null],
      ['precio por noche', this.draft.precioNoche !== null],
      ['hora de entrada', !!this.draft.horaEntrada],
      ['hora de salida', !!this.draft.horaSalida],
      ['reglas', !!this.draft.reglas.trim()],
      ['política de cancelación', this.draft.idPolitica !== null]
    ];

    const faltantes = campos
      .filter(([, completo]) => !completo)
      .map(([nombre]) => nombre);

    if (faltantes.length > 0) {
      return (
        'Para enviar a revisión debes completar: ' +
        faltantes.join(', ') +
        '.'
      );
    }

    return null;
  }

  // ======================================
  // 10. CONSTRUIR DTO DEL BACKEND
  // ======================================

  private construirDatos(): DatosAlojamiento {

    const d = this.draft;

    return {
      titulo: d.titulo.trim(),

      ...(d.descripcion.trim()
        ? { descripcion: d.descripcion.trim() }
        : {}),

      ...(d.ubicacion.trim()
        ? { ubicacion: d.ubicacion.trim() }
        : {}),

      ...(d.ciudad.trim()
        ? { ciudad: d.ciudad.trim() }
        : {}),

      ...(d.tipo.trim()
        ? { tipo: d.tipo.trim() }
        : {}),

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

      ...(d.horaEntrada
        ? { horaEntrada: d.horaEntrada }
        : {}),

      ...(d.horaSalida
        ? { horaSalida: d.horaSalida }
        : {}),

      ...(d.reglas.trim()
        ? { reglas: d.reglas.trim() }
        : {}),

      ...(d.idPolitica !== null
        ? { idPolitica: d.idPolitica }
        : {}),

      amenidades: [...d.amenidades]
    };
  }

  // ======================================
  // 11. LIMPIAR ARCHIVOS GUARDADOS
  // ======================================

  private limpiarSeleccionImagenes(): void {

    this.imagenesSeleccionadas = [];

    if (this.fotoInput) {
      this.fotoInput.nativeElement.value = '';
    }
  }

  // ======================================
  // 12. MANEJAR ERRORES DEL BACKEND
  // ======================================

  private mensajeError(
    err: unknown,
    mensajeAlternativo: string
  ): string {

    if (
      err instanceof Error &&
      err.name === 'TimeoutError'
    ) {
      return (
        'La solicitud tardó demasiado. ' +
        'Revisa Mis alojamientos antes de reintentar.'
      );
    }

    if (err instanceof HttpErrorResponse) {

      if (err.status === 0) {
        return 'No se pudo conectar con Spring Boot en el puerto 8081.';
      }

      if (err.status === 401) {
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      }

      if (err.status === 403) {
        return 'Esta operación requiere una cuenta de Anfitrión autorizada.';
      }

      const respuesta: unknown = err.error;

      if (
        respuesta !== null &&
        typeof respuesta === 'object' &&
        'message' in respuesta
      ) {

        const mensaje =
          (respuesta as { message?: unknown }).message;

        if (typeof mensaje === 'string') {
          return mensaje;
        }
      }

      if (typeof respuesta === 'string') {
        return respuesta;
      }
    }

    return mensajeAlternativo;
  }
}
