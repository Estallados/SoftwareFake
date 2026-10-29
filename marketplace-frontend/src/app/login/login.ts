
import {
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';

import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import {
  finalize,
  switchMap,
  timeout
} from 'rxjs';

import { Auth, LoginResponse } from '../services/auth';

// ======================================
// INTERFAZ DE ERRORES DEL BACKEND
// ======================================

interface ErrorBackend {
  message?: string;
  mensaje?: string;
  error?: string;
  detalle?: string;
  requiereVerificacion?: boolean;
  requiereSegundoFactor?: boolean;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './login.html'
})
export class Login {

  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  // ======================================
  // VARIABLES DEL FORMULARIO
  // ======================================

  showPassword = false;

  correo = '';
  contrasenia = '';
  codigo = '';

  cargando = false;
  reenviando = false;

  error = '';
  mensaje = '';

  requiereVerificacion = false;
  requiereSegundoFactor = false;

  // ======================================
  // 1. INICIAR SESIÓN
  // ======================================

  iniciarSesion(form: NgForm): void {

    if (this.cargando || this.reenviando) {
      return;
    }

    if (form.invalid) {
      this.error = 'Completa correctamente los campos.';
      this.actualizarVista();
      return;
    }

    this.error = '';
    this.mensaje = '';

    // Verificar correo con código.
    if (this.requiereVerificacion) {
      this.verificarCorreo();
      return;
    }

    // Autenticación de dos factores.
    if (this.requiereSegundoFactor) {

      if (!this.codigoValido()) {
        this.error = 'Ingresa un código válido de 6 dígitos.';
        this.actualizarVista();
        return;
      }

      this.ejecutarLogin(this.codigo.trim());
      return;
    }

    // Inicio de sesión normal.
    this.ejecutarLogin();
  }

  // ======================================
  // 2. EJECUTAR LOGIN
  // ======================================

  private ejecutarLogin(
    codigoSegundoFactor?: string
  ): void {

    if (this.cargando) {
      return;
    }

    this.cargando = true;
    this.error = '';
    this.mensaje = '';

    this.actualizarVista();

    console.log('Iniciando petición de login...');

    this.auth.login({
      correo: this.correo.trim(),
      contrasenia: this.contrasenia,
      ...(codigoSegundoFactor
        ? { codigo: codigoSegundoFactor }
        : {})
    })
      .pipe(
        timeout(15000),

        finalize(() => {
          this.cargando = false;
          this.actualizarVista();
          console.log('Petición de login finalizada');
        })
      )
      .subscribe({

        next: (respuesta: LoginResponse) => {

          console.log('Login exitoso');

          this.guardarSesion(respuesta);
        },

        error: (err: unknown) => {

          this.procesarErrorLogin(err);
        }
      });
  }

  // ======================================
  // 3. PROCESAR ERROR DE LOGIN
  // ======================================

  private procesarErrorLogin(err: unknown): void {

    console.error('Error en login:', err);

    if (this.esTimeout(err)) {

      this.error =
        'El servidor tardó demasiado en responder. Intenta nuevamente.';

      this.actualizarVista();
      return;
    }

    if (!(err instanceof HttpErrorResponse)) {

      this.error =
        'Ocurrió un error inesperado al iniciar sesión.';

      this.actualizarVista();
      return;
    }

    const datos = this.obtenerDatosError(err);

    const mensajeBackend = this.obtenerMensaje(datos);

    const textoNormalizado = mensajeBackend
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    // ======================================
    // CORREO SIN VERIFICAR
    // ======================================

    const correoSinVerificar =
      datos.requiereVerificacion === true ||
      (
        err.status === 403 &&
        (
          textoNormalizado.includes('correo sin verificar') ||
          textoNormalizado.includes('correo no verificado') ||
          textoNormalizado.includes('correo no esta verificado') ||
          textoNormalizado.includes('verificar tu correo') ||
          textoNormalizado.includes('verificar el correo') ||
          textoNormalizado.includes('verifica tu correo') ||
          textoNormalizado.includes('correo aun no esta verificado') ||
          textoNormalizado.includes('email not verified')
        )
      );

    if (correoSinVerificar) {

      this.requiereVerificacion = true;
      this.requiereSegundoFactor = false;
      this.codigo = '';

      this.mensaje =
        'Tu correo todavía no está verificado. ' +
        'Ingresa el código recibido o solicita uno nuevo.';

      this.actualizarVista();
      return;
    }

    // ======================================
    // SEGUNDO FACTOR
    // ======================================

    if (datos.requiereSegundoFactor === true) {

      this.requiereSegundoFactor = true;
      this.requiereVerificacion = false;
      this.codigo = '';

      this.mensaje =
        'Ingresa el código de tu aplicación autenticadora.';

      this.actualizarVista();
      return;
    }

    // ======================================
    // ERROR DE CONEXIÓN
    // ======================================

    if (err.status === 0) {

      this.error =
        'No se pudo conectar con Spring Boot. ' +
        'Verifica que esté ejecutándose en el puerto 8081.';

      this.actualizarVista();
      return;
    }

    // ======================================
    // OTROS ERRORES
    // ======================================

    this.error =
      mensajeBackend ||
      (
        err.status === 403
          ? 'El servidor rechazó el acceso. Revisa la respuesta de /auth/login.'
          : 'No se pudo iniciar sesión. Verifica tus credenciales.'
      );

    this.actualizarVista();
  }

  // ======================================
  // 4. VERIFICAR CORREO
  // ======================================

  verificarCorreo(): void {

    if (this.cargando) {
      return;
    }

    if (!this.codigoValido()) {

      this.error =
        'Ingresa el código de verificación de 6 dígitos.';

      this.actualizarVista();
      return;
    }

    this.cargando = true;
    this.error = '';
    this.mensaje = '';

    this.actualizarVista();

    const correoActual = this.correo.trim();
    const contraseniaActual = this.contrasenia;
    const codigoActual = this.codigo.trim();

    console.log('Verificando correo electrónico...');

    // Primero verifica el código y luego
    // ejecuta el login automáticamente.
    this.auth.verificarCorreo(
      correoActual,
      codigoActual
    )
      .pipe(
        timeout(15000),

        switchMap(() => {

          this.requiereVerificacion = false;
          this.requiereSegundoFactor = false;
          this.codigo = '';

          this.mensaje =
            'Correo verificado correctamente. Iniciando sesión...';

          this.actualizarVista();

          return this.auth.login({
            correo: correoActual,
            contrasenia: contraseniaActual
          })
            .pipe(
              timeout(15000)
            );
        }),

        finalize(() => {

          this.cargando = false;
          this.actualizarVista();
        })
      )
      .subscribe({

        next: (respuesta: LoginResponse) => {

          console.log('Correo verificado e inicio de sesión exitoso');

          this.guardarSesion(respuesta);
        },

        error: (err: unknown) => {

          console.error(
            'Error durante la verificación o el inicio de sesión:',
            err
          );

          if (this.requiereVerificacion) {

            this.procesarErrorVerificacion(err);

          } else {

            this.procesarErrorLogin(err);
          }
        }
      });
  }

  // ======================================
  // 5. PROCESAR ERROR DE VERIFICACIÓN
  // ======================================

  private procesarErrorVerificacion(err: unknown): void {

    if (this.esTimeout(err)) {

      this.error =
        'La verificación tardó demasiado. Intenta nuevamente.';

      this.actualizarVista();
      return;
    }

    if (err instanceof HttpErrorResponse) {

      if (err.status === 0) {

        this.error =
          'No se pudo conectar con el servidor para verificar el código.';

        this.actualizarVista();
        return;
      }

      const datos = this.obtenerDatosError(err);

      this.error =
        this.obtenerMensaje(datos) ||
        'El código es incorrecto o ha vencido. Solicita uno nuevo.';

      this.actualizarVista();
      return;
    }

    this.error =
      'Ocurrió un error inesperado al verificar el correo.';

    this.actualizarVista();
  }

  // ======================================
  // 6. REENVIAR CÓDIGO
  // ======================================

  reenviarCodigo(): void {

    if (!this.correo.trim()) {

      this.error =
        'Ingresa primero tu correo electrónico.';

      this.actualizarVista();
      return;
    }

    if (this.reenviando || this.cargando) {
      return;
    }

    this.reenviando = true;
    this.error = '';
    this.mensaje = '';

    this.actualizarVista();

    console.log('Solicitando nuevo código de verificación...');

    this.auth.reenviarCodigo(
      this.correo.trim()
    )
      .pipe(
        timeout(15000),

        finalize(() => {

          this.reenviando = false;
          this.actualizarVista();
        })
      )
      .subscribe({

        next: (respuesta) => {

          this.codigo = '';

          // Mostrar el campo para ingresar
          // el código nuevo.
          this.requiereVerificacion = true;
          this.requiereSegundoFactor = false;

          this.mensaje =
            respuesta?.message ||
            'Solicitud procesada. Revisa tu correo electrónico.';

          this.actualizarVista();

          console.log('Reenvío procesado correctamente');
        },

        error: (err: unknown) => {

          console.error('Error reenviando código:', err);

          if (this.esTimeout(err)) {

            this.error =
              'El servidor tardó demasiado en responder.';

            this.actualizarVista();
            return;
          }

          if (err instanceof HttpErrorResponse) {

            if (err.status === 0) {

              this.error =
                'No se pudo conectar con el servidor.';

              this.actualizarVista();
              return;
            }

            const datos = this.obtenerDatosError(err);

            this.error =
              this.obtenerMensaje(datos) ||
              'No se pudo reenviar el código de verificación.';

            this.actualizarVista();
            return;
          }

          this.error =
            'Ocurrió un error inesperado al reenviar el código.';

          this.actualizarVista();
        }
      });
  }

  // ======================================
  // 7. GUARDAR SESIÓN
  // ======================================

  private guardarSesion(respuesta: LoginResponse): void {

    if (!respuesta.token) {

      this.error =
        'El servidor no devolvió un token de autenticación.';

      this.actualizarVista();
      return;
    }

    sessionStorage.setItem('token', respuesta.token);
    sessionStorage.setItem('rol', respuesta.rol);
    sessionStorage.setItem('correo', respuesta.correo);

    console.log('Sesión iniciada correctamente');

    this.router.navigateByUrl('/');
  }

  // ======================================
  // 8. CAMBIAR CORREO
  // ======================================

  cambiarCorreo(valor: string): void {

    this.correo = valor;
    this.reiniciarVerificacion();
  }

  // ======================================
  // 9. CAMBIAR CONTRASEÑA
  // ======================================

  cambiarContrasenia(valor: string): void {

    this.contrasenia = valor;
    this.reiniciarVerificacion();
  }

  // ======================================
  // 10. REINICIAR VERIFICACIÓN
  // ======================================

  private reiniciarVerificacion(): void {

    this.requiereVerificacion = false;
    this.requiereSegundoFactor = false;

    this.codigo = '';
    this.error = '';
    this.mensaje = '';
  }

  // ======================================
  // 11. VALIDAR CÓDIGO
  // ======================================

  private codigoValido(): boolean {

    return /^\d{6}$/.test(
      this.codigo.trim()
    );
  }

  // ======================================
  // 12. DETECTAR TIMEOUT
  // ======================================

  private esTimeout(err: unknown): boolean {

    return (
      err instanceof Error &&
      err.name === 'TimeoutError'
    );
  }

  // ======================================
  // 13. INTERPRETAR ERRORES DEL BACKEND
  // CORRECCIÓN DEL ERROR TS2322
  // ======================================

  private obtenerDatosError(
    err: HttpErrorResponse
  ): ErrorBackend {

    let datos: unknown = err.error;

    if (typeof datos === 'string') {

      try {

        datos = JSON.parse(datos);

      } catch {

        // CORREGIDO:
        // Convertimos el valor a string.
        return {
          message: String(datos)
        };
      }
    }

    if (
      datos !== null &&
      typeof datos === 'object' &&
      !Array.isArray(datos)
    ) {

      return datos as ErrorBackend;
    }

    return {};
  }

  // ======================================
  // 14. OBTENER MENSAJE DEL BACKEND
  // ======================================

  private obtenerMensaje(datos: ErrorBackend): string {

    return (
      datos.message ||
      datos.mensaje ||
      datos.detalle ||
      datos.error ||
      ''
    );
  }

  // ======================================
  // 15. ACTUALIZAR VISTA
  // ======================================

  private actualizarVista(): void {

    this.cdr.markForCheck();
  }
}
