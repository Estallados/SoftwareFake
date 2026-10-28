
import { Component, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

import {
  Auth,
  RegistroRequest,
  RegistroRol
} from '../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html'
})
export class Register {

  private readonly authService = inject(Auth);
  private readonly router = inject(Router);

  // DATOS DEL FORMULARIO
  nombre = '';
  correo = '';
  telefono = '';
  contrasena = '';

  rol: RegistroRol = 'HUESPED';

  // MOSTRAR CONTRASEÑA
  showPassword = false;

  // ESTADOS DEL FORMULARIO
  guardando = false;
  pendienteVerificacion = false;
  verificando = false;
  reenviando = false;

  codigo = '';
  mensaje = '';
  error = '';

  // VALIDAR CONTRASEÑA
  private contrasenaValida(): boolean {
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/
      .test(this.contrasena);
  }

  // REGISTRAR USUARIO
  registrar(form: NgForm): void {

    if (this.guardando) return;

    this.mensaje = '';
    this.error = '';

    if (form.invalid || !this.nombre.trim() || !this.correo.trim()) {
      this.error = 'Completa correctamente los campos.';
      return;
    }

    if (!this.contrasenaValida()) {
      this.error =
        'La contraseña debe tener al menos 8 caracteres, ' +
        'una mayúscula, una minúscula, un número y un símbolo.';
      return;
    }

    const telefonoLimpio = this.telefono.trim();

    if (
      telefonoLimpio &&
      !/^\+?\d{7,15}$/.test(
        telefonoLimpio.replace(/[\s().-]/g, '')
      )
    ) {
      this.error = 'El teléfono debe tener entre 7 y 15 dígitos.';
      return;
    }

    const datos: RegistroRequest = {
      nombre: this.nombre.trim(),
      correo: this.correo.trim().toLowerCase(),
      telefono: telefonoLimpio,
      rol: this.rol,
      contrasenia: this.contrasena
    };

    this.guardando = true;

    this.authService.registrar(datos).subscribe({

      next: (respuesta) => {
        this.guardando = false;
        this.correo = respuesta.correo;
        this.mensaje = respuesta.message;

        this.pendienteVerificacion =
          respuesta.requiereVerificacion === true;

        this.contrasena = '';
      },

      error: (err: HttpErrorResponse) => {
        this.guardando = false;
        this.error = this.mensajeError(
          err,
          'No fue posible registrar la cuenta.'
        );
      }
    });
  }

  // VERIFICAR CORREO ELECTRÓNICO
  verificarCorreo(form: NgForm): void {

    if (this.verificando) return;

    this.mensaje = '';
    this.error = '';

    if (form.invalid || !/^\d{6}$/.test(this.codigo.trim())) {
      this.error = 'Ingresa un código válido de 6 dígitos.';
      return;
    }

    this.verificando = true;

    this.authService.verificarCorreo(
      this.correo,
      this.codigo.trim()
    ).subscribe({

      next: () => {
        this.verificando = false;
        this.router.navigateByUrl('/login');
      },

      error: (err: HttpErrorResponse) => {
        this.verificando = false;
        this.error = this.mensajeError(
          err,
          'No fue posible verificar el correo.'
        );
      }
    });
  }

  // REENVIAR CÓDIGO DE VERIFICACIÓN
  reenviarCodigo(): void {

    if (this.reenviando) return;

    this.mensaje = '';
    this.error = '';
    this.reenviando = true;

    this.authService.reenviarCodigo(this.correo).subscribe({

      next: (respuesta) => {
        this.reenviando = false;
        this.mensaje = respuesta.message;
      },

      error: (err: HttpErrorResponse) => {
        this.reenviando = false;
        this.error = this.mensajeError(
          err,
          'No fue posible reenviar el código.'
        );
      }
    });
  }

  // MANEJO DE ERRORES
  private mensajeError(
    err: HttpErrorResponse,
    mensajeAlternativo: string
  ): string {

    return typeof err.error?.message === 'string'
      ? err.error.message
      : mensajeAlternativo;
  }
}
