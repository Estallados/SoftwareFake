import { Alojamiento } from './alojamientoDto';

/**
 * Datos que se envían al crear o editar un alojamiento (sin los campos de solo salida).
 * Al crear, solo el título es obligatorio (queda en BORRADOR).
 * Al editar, los campos undefined/null no cambian y las amenidades reemplazan las anteriores.
 */
export type AlojamientoEntrada = Pick<
  Alojamiento,
  | 'titulo'
  | 'descripcion'
  | 'ubicacion'
  | 'ciudad'
  | 'latitud'
  | 'longitud'
  | 'tipo'
  | 'capacidad'
  | 'precioNoche'
  | 'porcentajeLimpieza'
  | 'porcentajeServicio'
  | 'reglas'
  | 'habitaciones'
  | 'banos'
  | 'horaEntrada'
  | 'horaSalida'
  | 'idPolitica'
  | 'amenidades'
>;
