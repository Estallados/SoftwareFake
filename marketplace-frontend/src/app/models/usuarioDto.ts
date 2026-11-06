import {EstadoUsuario} from './estadoUsuarioDto';
import {Role} from '../layout/navbar/navbar.component';

/** Datos de un usuario tal como los devuelve el backend. */
export interface Usuario {
  /** Identificador del usuario. */
  idUsuario: number;
  /** Nombre completo. */
  nombre: string;
  /** Correo. */
  correo: string;
  /** Teléfono de contacto. */
  telefono: string;
  /** Rol del usuario. */
  rol: Role;
  /** Estado de la cuenta. */
  estado: EstadoUsuario;
  /** Contraseña; solo se envía al registrarse. */
  contrasenia: string;
  /** Motivo de la restricción, si un administrador la restringió. */
  motivoRestriccion: string;
  /** Indica si tiene activo el segundo factor. */
  totpHabilitado: boolean;
  /** Fecha en que se creó la cuenta (ISO). */
  fechaRegistro: string; // LocalDateTime se representa como string en JSON correoVerificado: boolean;
}
