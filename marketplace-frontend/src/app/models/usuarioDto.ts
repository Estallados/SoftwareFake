import {EstadoUsuario} from './estadoUsuarioDto';
import {Role} from '../layout/navbar/navbar.component';


export interface Usuario {
  
  idUsuario: number;
  
  nombre: string;
 
  correo: string;
  
  telefono: string;
  
  rol: Role;
  
  estado: EstadoUsuario;
  
  contrasenia: string;
  
  motivoRestriccion: string;
  
  totpHabilitado: boolean;
  
  fechaRegistro: string; // LocalDateTime se representa como string en JSON correoVerificado: boolean;
}
