
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  ViewChild,
  inject
} from '@angular/core';

import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

import {
  finalize,
  switchMap,
  timeout
} from 'rxjs';

import {
  Auth,
  LoginResponse,
  RegistroRol
} from '../services/auth';

import { SessionService } from '../services/session.service';

// ======================================
// INTERFAZ DE ERRORES DEL BACKEND
// ======================================

interface ErrorBackend {
  message?: string;
  mensaje?: string;
  detalle?: string;
  error?: string;
  requiereVerificacion?: boolean;
  requiereSegundoFactor?: boolean;
}

// ======================================
// GOOGLE IDENTITY SERVICES
// ======================================

interface CredencialGoogle {
  credential?: string;
}

interface GoogleSdk {
  accounts: {
    id: {
      initialize(config: {
        client_id: string;
        callback: (
          response: CredencialGoogle
        ) => void;
        auto_select?: boolean;
      }): void;

      renderButton(
        element: HTMLElement,
        config: {
          type?: 'standard' | 'icon';
          theme?: 'outline' | 'filled_black';
          shape?: 'pill' | 'circle';
          size?: 'large';
          text?: 'continue_with';
          logo_alignment?: 'left';
          width?: number;
          locale?: string;
        }
      ): void;
    };
  };
}

declare global {
  interface Window {
    google?: GoogleSdk;
  }
}

// ======================================
// COMPONENTE LOGIN
// ======================================

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule
  ],
  templateUrl: './login.html'
})
export class Login implements AfterViewInit {

  // ======================================
  // SERVICIOS
  // ======================================

  private readonly auth = inject(Auth);

  private readonly router = inject(Router);

  private readonly cdr = inject(ChangeDetectorRef);

  private readonly sesion = inject(SessionService);

  // ======================================
  // CONTENEDOR DEL BOTÓN GOOGLE
  // ======================================

  @ViewChild('googleButton')
  googleButton?: ElementRef<HTMLDivElement>;

  // ======================================
  // FORMULARIO DE LOGIN
  // ======================================

  correo = '';

  contrasenia = '';

  codigo = '';

  showPassword = false;

  // ======================================
  // ESTADOS DEL LOGIN
  // ======================================

  cargando = false;

  reenviando = false;

  error = '';

  mensaje = '';

  requiereVerificacion = false;

  requiereSegundoFactor = false;

  // ======================================
  // GOOGLE
  // ======================================

  rolGoogle: RegistroRol = 'HUESPED';

  googleDisponible = false;

  googleCargando = false;

  googleError = '';

  googleSegundoFactor = false;

  codigoGoogle = '';

  private googleIdToken = '';

  // ======================================
  // 1. INICIAR SESIÓN NORMAL
  // ======================================

  iniciarSesion(form: NgForm): void {

    if (
      this.cargando ||
      this.reenviando
    ) {
      return;
    }

    if (form.invalid) {

      this.error =
        'Completa correctamente los campos.';

      this.actualizarVista();

      return;
    }

    this.error = '';

    this.mensaje = '';

    // Verificar correo pendiente

    if (this.requiereVerificacion) {

      this.verificarCorreo();

      return;
    }

    // Verificar segundo factor

    if (this.requiereSegundoFactor) {

      if (!this.codigoValido()) {

        this.error =
          'Ingresa un código válido de 6 dígitos.';

        this.actualizarVista();

        return;
      }

      this.ejecutarLogin(
        this.codigo.trim()
      );

      return;
    }

    // Login normal

    this.ejecutarLogin();
  }

  // ======================================
  // 2. ENVIAR LOGIN AL BACKEND
  // POST /auth/login
  // ======================================

  private ejecutarLogin(
    codigo?: string
  ): void {

    if (this.cargando) {
      return;
    }

    this.cargando = true;

    this.error = '';

    this.mensaje = '';

    this.actualizarVista();

    this.auth.login({

      correo: this.correo.trim(),

      contrasenia: this.contrasenia,

      ...(codigo
        ? { codigo }
        : {})

    })
      .pipe(

        timeout(15000),

        finalize(() => {

          this.cargando = false;

          this.actualizarVista();

        })

      )
      .subscribe({

        next: respuesta => {

          this.guardarSesion(respuesta);

        },

        error: err => {

          this.procesarErrorLogin(err);

        }

      });
  }

  // ======================================
  // 3. PROCESAR ERRORES DE LOGIN
  // ======================================

  private procesarErrorLogin(
    err: unknown
  ): void {

    if (this.esTimeout(err)) {

      this.error =
        'El servidor tardó demasiado en responder. Intenta nuevamente.';

      this.actualizarVista();

      return;
    }

    if (
      !(err instanceof HttpErrorResponse)
    ) {

      this.error =
        'Ocurrió un error inesperado al iniciar sesión.';

      this.actualizarVista();

      return;
    }

    const datos =
      this.obtenerDatosError(err);

    const mensaje =
      this.obtenerMensaje(datos);

    const texto = mensaje
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    // ======================================
    // CORREO NO VERIFICADO
    // ======================================

    const correoSinVerificar =

      datos.requiereVerificacion === true ||

      (
        err.status === 403 &&

        (
          texto.includes('correo sin verificar') ||

          texto.includes('correo no verificado') ||

          texto.includes('correo no esta verificado') ||

          texto.includes('verificar tu correo') ||

          texto.includes('verificar el correo') ||

          texto.includes('verifica tu correo') ||

          texto.includes('email not verified')
        )
      );

    if (correoSinVerificar) {

      this.requiereVerificacion = true;

      this.requiereSegundoFactor = false;

      this.codigo = '';

      this.mensaje =
        'Tu correo no está verificado. ' +
        'Introduce el código recibido o solicita otro.';

    } else if (
      datos.requiereSegundoFactor === true
    ) {

      // ======================================
      // SEGUNDO FACTOR REQUERIDO
      // ======================================

      this.requiereSegundoFactor = true;

      this.requiereVerificacion = false;

      this.codigo = '';

      this.mensaje =
        'Ingresa el código de tu aplicación autenticadora.';

    } else if (err.status === 0) {

      // ======================================
      // BACKEND NO DISPONIBLE
      // ======================================

      this.error =
        'No fue posible conectar con Spring Boot en el puerto 8081.';

    } else {

      this.error =
        mensaje ||
        'No se pudo iniciar sesión. Comprueba tus credenciales.';

    }

    this.actualizarVista();
  }

  // ======================================
  // 4. VERIFICAR CORREO ELECTRÓNICO
  // POST /auth/verificar
  // ======================================

  verificarCorreo(): void {

    if (this.cargando) {
      return;
    }

    if (!this.codigoValido()) {

      this.error =
        'Ingresa un código de verificación de 6 dígitos.';

      this.actualizarVista();

      return;
    }

    const correo =
      this.correo.trim();

    const contrasenia =
      this.contrasenia;

    const codigo =
      this.codigo.trim();

    this.cargando = true;

    this.error = '';

    this.mensaje = '';

    this.actualizarVista();

    this.auth.verificarCorreo(
      correo,
      codigo
    )
      .pipe(

        timeout(15000),

        switchMap(() => {

          this.requiereVerificacion = false;

          this.requiereSegundoFactor = false;

          this.codigo = '';

          this.mensaje =
            'Correo verificado. Iniciando sesión...';

          this.actualizarVista();

          return this.auth.login({

            correo,

            contrasenia

          }).pipe(
            timeout(15000)
          );

        }),

        finalize(() => {

          this.cargando = false;

          this.actualizarVista();

        })

      )
      .subscribe({

        next: respuesta => {

          this.guardarSesion(respuesta);

        },

        error: err => {

          if (this.requiereVerificacion) {

            this.error = this.mensajeError(
              err,
              'El código es incorrecto o ha vencido.'
            );

            this.actualizarVista();

          } else {

            this.procesarErrorLogin(err);

          }

        }

      });
  }

  // ======================================
  // 5. REENVIAR CÓDIGO
  // POST /auth/reenviarcodigo
  // ======================================

  reenviarCodigo(): void {

    if (
      this.cargando ||
      this.reenviando
    ) {
      return;
    }

    if (!this.correo.trim()) {

      this.error =
        'Ingresa primero tu correo electrónico.';

      this.actualizarVista();

      return;
    }

    this.reenviando = true;

    this.error = '';

    this.mensaje = '';

    this.actualizarVista();

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

        next: respuesta => {

          this.codigo = '';

          this.requiereVerificacion = true;

          this.requiereSegundoFactor = false;

          this.mensaje =
            respuesta?.message ||
            'Solicitud procesada. Revisa tu correo electrónico.';

          this.actualizarVista();

        },

        error: err => {

          this.error = this.mensajeError(
            err,
            'No se pudo reenviar el código.'
          );

          this.actualizarVista();

        }

      });
  }

  // ======================================
  // 6. CAMBIAR CORREO
  // ======================================

  cambiarCorreo(valor: string): void {

    this.correo = valor;

    this.reiniciarVerificacion();
  }

  // ======================================
  // 7. CAMBIAR CONTRASEÑA
  // ======================================

  cambiarContrasenia(
    valor: string
  ): void {

    this.contrasenia = valor;

    this.reiniciarVerificacion();
  }

  // ======================================
  // 8. REINICIAR VERIFICACIÓN
  // ======================================

  private reiniciarVerificacion(): void {

    this.requiereVerificacion = false;

    this.requiereSegundoFactor = false;

    this.codigo = '';

    this.error = '';

    this.mensaje = '';
  }

  // ======================================
  // 9. VALIDAR CÓDIGO
  // ======================================

  private codigoValido(): boolean {

    return /^\d{6}$/.test(
      this.codigo.trim()
    );
  }

  // ======================================
  // 10. GUARDAR SESIÓN JWT
  // ======================================

  private guardarSesion(
    respuesta: LoginResponse
  ): void {

    if (
      !respuesta.token ||
      !respuesta.rol ||
      !respuesta.correo
    ) {

      this.error =
        'El servidor no devolvió los datos necesarios de la sesión.';

      this.actualizarVista();

      return;
    }

    try {

      this.sesion.iniciarSesion({

        token: respuesta.token,

        rol: respuesta.rol,

        correo: respuesta.correo

      });

      // ======================================
      // REDIRECCIÓN SEGÚN ROL
      // ======================================

      switch (this.sesion.rol()) {

        case 'Huésped':

          void this.router.navigateByUrl(
            '/home'
          );

          break;

        case 'Anfitrión':

          void this.router.navigateByUrl(
            '/host/dashboard'
          );

          break;

        case 'Administrador':

          void this.router.navigateByUrl(
            '/admin/dashboard'
          );

          break;

        default:

          this.sesion.cerrarSesion();

          this.error =
            'El rol recibido no es válido.';

          this.actualizarVista();

      }

    } catch {

      this.error =
        'No se pudo establecer la sesión con el JWT recibido.';

      this.actualizarVista();

    }
  }

  // ======================================
  // 11. INICIALIZAR GOOGLE
  // ======================================

  ngAfterViewInit(): void {

    this.auth.configuracionGoogle()
      .pipe(
        timeout(15000)
      )
      .subscribe({

        next: config => {

          if (
            !config.habilitado ||
            !config.clientId
          ) {

            this.googleError =
              'Google no está configurado en el servidor.';

            this.actualizarVista();

            return;
          }

          void this.inicializarGoogle(
            config.clientId
          );

        },

        error: () => {

          this.googleError =
            'No se pudo consultar la configuración de Google.';

          this.actualizarVista();

        }

      });
  }

  // ======================================
  // 12. SELECCIONAR ROL GOOGLE
  // ======================================

  cambiarRolGoogle(
    valor: string
  ): void {

    if (
      valor === 'HUESPED' ||
      valor === 'ANFITRION'
    ) {

      this.rolGoogle = valor;

    }
  }

  // ======================================
  // 13. CARGAR LIBRERÍA DE GOOGLE
  // ======================================

  private cargarLibreriaGoogle():
    Promise<void> {

    if (
      window.google?.accounts?.id
    ) {

      return Promise.resolve();

    }

    return new Promise(
      (resolve, reject) => {

        const script =
          document.createElement('script');

        script.src =
          'https://accounts.google.com/gsi/client';

        script.async = true;

        script.defer = true;

        script.onload = () => {

          if (
            window.google?.accounts?.id
          ) {

            resolve();

          } else {

            reject(
              new Error(
                'No se cargó Google Identity Services.'
              )
            );

          }

        };

        script.onerror = () => {

          reject(
            new Error(
              'No se pudo descargar Google Identity Services.'
            )
          );

        };

        document.head.appendChild(script);

      }
    );
  }

  // ======================================
  // 14. DIBUJAR BOTÓN GOOGLE
  // DISEÑO BLANCO TIPO CÁPSULA
  // ======================================

  private async inicializarGoogle(
    clientId: string
  ): Promise<void> {

    try {

      await this.cargarLibreriaGoogle();

      const sdk =
        window.google?.accounts?.id;

      const destino =
        this.googleButton?.nativeElement;

      if (
        !sdk ||
        !destino
      ) {

        throw new Error(
          'No se encontró el contenedor de Google.'
        );

      }

      // ======================================
      // CONFIGURAR AUTENTICACIÓN GOOGLE
      // ======================================

      sdk.initialize({

        client_id: clientId,

        auto_select: false,

        callback: respuesta => {

          if (!respuesta.credential) {

            this.googleError =
              'Google no devolvió credenciales.';

            this.actualizarVista();

            return;
          }

          this.googleIdToken =
            respuesta.credential;

          this.ejecutarLoginGoogle();

        }

      });

      // Limpiar botón anterior

      destino.replaceChildren();

      // ======================================
      // NUEVO DISEÑO GOOGLE
      // ======================================

      sdk.renderButton(destino, {

        // Botón con icono y texto
        type: 'standard',

        // Fondo blanco
        theme: 'outline',

        // Forma de cápsula
        shape: 'pill',

        // Tamaño del botón
        size: 'large',

        // Texto de Google
        text: 'continue_with',

        // Logotipo a la izquierda
        logo_alignment: 'left',

        // Ancho del botón
        width: 230,

        // Idioma español
        locale: 'es'

      });

      this.googleDisponible = true;

      this.googleError = '';

      this.actualizarVista();

    } catch {

      this.googleError =
        'No se pudo cargar Google. Comprueba la conexión y el Client ID.';

      this.actualizarVista();

    }
  }

  // ======================================
  // 15. SEGUNDO FACTOR DE GOOGLE
  // ======================================

  verificarSegundoFactorGoogle(): void {

    if (
      !/^\d{6}$/.test(
        this.codigoGoogle.trim()
      )
    ) {

      this.googleError =
        'Ingresa los 6 dígitos de tu autenticador.';

      this.actualizarVista();

      return;
    }

    this.ejecutarLoginGoogle(
      this.codigoGoogle.trim()
    );
  }

  // ======================================
  // 16. INICIAR SESIÓN CON GOOGLE
  // POST /auth/google
  // ======================================

  private ejecutarLoginGoogle(
    codigo?: string
  ): void {

    if (
      this.googleCargando ||
      !this.googleIdToken
    ) {
      return;
    }

    this.googleCargando = true;

    this.googleError = '';

    this.actualizarVista();

    this.auth.iniciarConGoogle(

      this.googleIdToken,

      this.rolGoogle,

      codigo

    )
      .pipe(

        timeout(15000),

        finalize(() => {

          this.googleCargando = false;

          this.actualizarVista();

        })

      )
      .subscribe({

        next: respuesta => {

          this.googleIdToken = '';

          this.googleSegundoFactor = false;

          this.codigoGoogle = '';

          // Guardar JWT como login normal

          this.guardarSesion(respuesta);

        },

        error: (err: unknown) => {

          if (
            err instanceof HttpErrorResponse
          ) {

            const datos =
              this.obtenerDatosError(err);

            // Google requiere segundo factor

            if (
              datos.requiereSegundoFactor === true
            ) {

              this.googleSegundoFactor = true;

              this.googleError =
                'Confirma el acceso con tu código de seguridad.';

            } else if (
              err.status === 503
            ) {

              this.googleError =
                'Google no está habilitado en el backend.';

            } else {

              this.googleError =
                this.obtenerMensaje(datos) ||
                'No fue posible iniciar sesión con Google.';

            }

          } else {

            this.googleError =
              this.esTimeout(err)
                ? 'Google tardó demasiado en responder.'
                : 'No fue posible iniciar sesión con Google.';

          }

          this.actualizarVista();

        }

      });
  }

  // ======================================
  // 17. INTERPRETAR ERRORES DEL BACKEND
  // ======================================

  private obtenerDatosError(
    err: HttpErrorResponse
  ): ErrorBackend {

    let datos: unknown = err.error;

    if (
      typeof datos === 'string'
    ) {

      try {

        datos = JSON.parse(datos);

      } catch {

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
  // 18. OBTENER MENSAJE DEL BACKEND
  // ======================================

  private obtenerMensaje(
    datos: ErrorBackend
  ): string {

    return (
      datos.message ||
      datos.mensaje ||
      datos.detalle ||
      datos.error ||
      ''
    );
  }

  // ======================================
  // 19. MENSAJE DE ERROR GENERAL
  // ======================================

  private mensajeError(
    err: unknown,
    mensajePorDefecto: string
  ): string {

    if (this.esTimeout(err)) {

      return (
        'El servidor tardó demasiado en responder.'
      );

    }

    if (
      !(err instanceof HttpErrorResponse)
    ) {

      return mensajePorDefecto;

    }

    if (err.status === 0) {

      return (
        'No se pudo conectar con Spring Boot.'
      );

    }

    return (
      this.obtenerMensaje(
        this.obtenerDatosError(err)
      ) ||
      mensajePorDefecto
    );
  }

  // ======================================
  // 20. DETECTAR TIMEOUT
  // ======================================

  private esTimeout(
    err: unknown
  ): boolean {

    return (
      err instanceof Error &&
      err.name === 'TimeoutError'
    );
  }

  // ======================================
  // 21. ACTUALIZAR VISTA
  // ======================================

  private actualizarVista(): void {

    this.cdr.markForCheck();
  }
}
