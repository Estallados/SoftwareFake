import { EstadoAlojamiento } from './estadoAlojamientoDto';
import { EstadoUsuario } from './estadoUsuarioDto';
import { Imagen } from './imagenDto';
import { ServicioAdicional } from './servicioAdicionalDto';


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

  horaEntrada?: string;

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
 
  imagenes?: string[];
 
  detalleImagenes?: Imagen[];
 
  promedioCalificacion?: number | null;
 
  cantidadResenas?: number;
  
  cantidadReservas?: number;

  correoAnfitrion?: string;
 
  estadoAnfitrion?: EstadoUsuario;
}
