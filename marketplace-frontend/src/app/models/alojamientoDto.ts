import { EstadoAlojamiento } from './estadoAlojamientoDto';
import { EstadoUsuario } from './estadoUsuarioDto';
import { Imagen } from './imagenDto';
import { ServicioAdicional } from './servicioAdicionalDto';

/**
 * Alojamiento (AlojamientoDTO).
 *
 * Serialización (Jackson → JSON):
 *  - BigDecimal    → number
 *  - LocalDateTime → 'AAAA-MM-DDTHH:mm:ss'
 *  - LocalTime     → 'HH:mm' (por @JsonFormat)
 *  - Set<String>   → string[]
 *
 * De entrada el backend ignora: idAlojamiento, estado, idAnfitrion y fechaCreacion.
 */
export interface Alojamiento {
  /** Identificador del alojamiento. */
  idAlojamiento?: number;
  /** Título de la publicación. */
  titulo: string;
  /** Descripción del alojamiento. */
  descripcion?: string;
  /** Dirección o referencia de la ubicación. */
  ubicacion?: string;
  /** Ciudad donde queda. */
  ciudad?: string;
  /** Latitud para el mapa. */
  latitud?: number;
  /** Longitud para el mapa. */
  longitud?: number;
  /** Tipo de alojamiento, por ejemplo Casa o Apartamento. */
  tipo?: string;
  /** Cantidad máxima de huéspedes. */
  capacidad?: number;
  /** Precio por noche en pesos colombianos. */
  precioNoche?: number;
  /** Porcentaje propio de limpieza; si no viene se usa el global. */
  porcentajeLimpieza?: number;
  /** Porcentaje propio de servicio; si no viene se usa el global. */
  porcentajeServicio?: number;
  /** Reglas de la casa. */
  reglas?: string;
  /** Cantidad de habitaciones. */
  habitaciones?: number;
  /** Cantidad de camas. */
  camas?: number;
  /** Cantidad de baños. */
  banos?: number;
  /** Hora de entrada (check-in), formato 'HH:mm' */
  horaEntrada?: string;
  /** Hora de salida (check-out), formato 'HH:mm' */
  horaSalida?: string;
  /** Política de cancelación elegida. */
  idPolitica?: number;
  /** Amenidades, por ejemplo wifi o piscina. */
  amenidades?: string[];
  /** Servicios adicionales con costo. */
  serviciosAdicionales?: ServicioAdicional[];

  // ----- Solo salida (los asigna el backend) -----
  /** Estado de la publicación. */
  estado?: EstadoAlojamiento;
  /** Anfitrión dueño. */
  idAnfitrion?: number;
  /** Nombre del anfitrión. */
  nombreAnfitrion?: string;
  /** Nombre de la política de cancelación. */
  nombrePolitica?: string;
  /** Fecha de creación, 'AAAA-MM-DDTHH:mm:ss'. */
  fechaCreacion?: string;
  /** Última edición de los datos o de las imágenes. */
  fechaActualizacion?: string;
  /** URLs de las imágenes, la principal primero */
  imagenes?: string[];
  /** Imágenes con id y portada, la principal primero */
  detalleImagenes?: Imagen[];
  /** Promedio de reseñas visibles (1 decimal); null si no tiene */
  promedioCalificacion?: number | null;
  /** Cantidad de reseñas visibles */
  cantidadResenas?: number;
  /** Reservas no canceladas del alojamiento */
  cantidadReservas?: number;
  /** Solo para el administrador */
  correoAnfitrion?: string;
  /** Solo para el administrador */
  estadoAnfitrion?: EstadoUsuario;
}
