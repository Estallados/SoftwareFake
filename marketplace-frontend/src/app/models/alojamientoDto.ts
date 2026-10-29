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
  idAlojamiento?: number;
  titulo: string;
  descripcion?: string;
  ubicacion?: string;
  ciudad?: string;
  latitud?: number;
  longitud?: number;
  tipo?: string;
  capacidad?: number;
  precioNoche?: number;
  porcentajeLimpieza?: number;
  porcentajeServicio?: number;
  reglas?: string;
  habitaciones?: number;
  camas?: number;
  banos?: number;
  /** Hora de entrada (check-in), formato 'HH:mm' */
  horaEntrada?: string;
  /** Hora de salida (check-out), formato 'HH:mm' */
  horaSalida?: string;
  idPolitica?: number;
  amenidades?: string[];
  serviciosAdicionales?: ServicioAdicional[];

  // ----- Solo salida (los asigna el backend) -----
  estado?: EstadoAlojamiento;
  idAnfitrion?: number;
  nombreAnfitrion?: string;
  nombrePolitica?: string;
  fechaCreacion?: string;
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
