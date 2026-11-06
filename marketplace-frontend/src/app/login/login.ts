
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  ViewChild,
  inject
} from '@angular/core';

import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, switchMap, tap, timeout } from 'rxjs';

import {
  Auth,
  LoginResponse,
  RegistroRol
} from '../services/auth';

import { SessionService } from '../services/session.service';

// ======================================
// RESPUESTA DE AUTENTICACIÓN
// ======================================
// El backend (8) todavía usa TOTP.
// Cuando se implemente 2FA por correo,
// deberá responder inicialmente con:
// {
//   requiereSegundoFactor: true,
//   metodoSegundoFactor: 'CORREO',
//   message: 'Código enviado al correo.'
// }
// Sin entregar todavía el JWT.

interface RespuestaAutenticacion {
  token?: string;
  rol?: string;
  correo?: string;
  message?: string;
  mensaje?: string;
  detalle?: string;
  error?: string;
  requiereVerificacion?: boolean;
  requiereSegundoFactor?: boolean;
  requiereCodigoCorreo?: boolean;
  metodoSegundoFactor?: string;
  tipoSegundoFactor?: string;
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
      initialize(options: {
        client_id: string;
        callback: (
          response: CredencialGoogle
        ) => void;
        auto_select?: boolean;
      }): void;

      renderButton(
        element: HTMLElement,
        options: {
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
    FormsModule,
    RouterLink
  ],
  templateUrl: './login.html'
})
export class Login implements AfterViewInit {

  // ======================================
  // SERVICIOS
  // ======================================

  private readonly auth = inject(Auth);

  private readonly sesion = inject(SessionService);

  private readonly router = inject(Router);

  private readonly cdr = inject(ChangeDetectorRef);

  private readonly destroyRef = inject(DestroyRef);

  // ======================================
  // BOTÓN GOOGLE
  // ======================================

  @ViewChild('googleButton')
  googleButton?: ElementRef<HTMLDivElement>;

  // ======================================
  // LOGIN TRADICIONAL
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

  // Código de verificación del registro

  requiereVerificacion = false;

  // Código por correo en cada inicio de sesión
  // Pendiente de implementar en el backend

  requiereSegundoFactor = false;

  // ======================================
  // GOOGLE: FLUJO INDEPENDIENTE
  // ======================================

  rolGoogle: RegistroRol = 'HUESPED';

  googleDisponible = false;

  googleCargando = false;

  googleError = '';

  private googleIdToken = '';

  // ======================================
  // 1. INICIAR SESIÓN
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
        'Completa correctamente todos los campos.';

      this.actualizarVista();

      return;
    }

    this.error = '';

    this.mensaje = '';

    // ======================================
    // VERIFICACIÓN DEL REGISTRO
    // ======================================

    if (this.requiereVerificacion) {

      this.verificarCorreo();

      return;
    }

    // ======================================
    // CÓDIGO DEL LOGIN POR CORREO
    // ======================================

    if (this.requiereSegundoFactor) {

      if (!this.codigoValido()) {

        this.error =
          'Introduce el código de 6 dígitos enviado a tu correo.';

        this.actualizarVista();

        return;
      }

      this.ejecutarLogin(this.codigo);

      return;
    }

    // ======================================
    // PRIMER PASO: CREDENCIALES
    // ======================================

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
        ? {
          codigo: codigo.trim()
        }
        : {})

    })
      .pipe(

        timeout(15000),

        finalize(() => {

          this.cargando = false;

          this.actualizarVista();

        }),

        takeUntilDestroyed(this.destroyRef)

      )
      .subscribe({

        next: respuesta => {

          this.procesarRespuestaLogin(respuesta);

        },

        error: err => {

          this.procesarErrorLogin(err);

        }

      });

  }

  // ======================================
  // 3. PROCESAR RESPUESTA DEL LOGIN
  // ======================================

  private procesarRespuestaLogin(
    respuesta: LoginResponse
  ): void {

    const datos =
      respuesta as RespuestaAutenticacion;

    // ======================================
    // EL SERVIDOR SOLICITA SEGUNDO FACTOR
    // ======================================

    if (
      datos.requiereSegundoFactor ||
      datos.requiereCodigoCorreo
    ) {

      this.procesarSolicitudSegundoFactor(datos);

      return;
    }

    // ======================================
    // LOGIN COMPLETADO
    // ======================================

    this.guardarSesion(respuesta);

  }

  // ======================================
  // 4. PROCESAR ERRORES DE LOGIN
  // ======================================

  private procesarErrorLogin(
    err: unknown
  ): void {

    if (!(err instanceof HttpErrorResponse)) {

      this.error = this.esTimeout(err)
        ? 'El servidor tardó demasiado en responder.'
        : 'Ocurrió un error inesperado al iniciar sesión.';

      this.actualizarVista();

      return;
    }

    const datos =
      this.obtenerDatosError(err);

    const texto = this.normalizarTexto(
      this.obtenerMensaje(datos)
    );

    // ======================================
    // CORREO NO VERIFICADO
    // ======================================

    const correoPendiente =

      datos.requiereVerificacion === true ||

      (
        err.status === 403 &&

        (
          texto.includes('correo sin verificar') ||
          texto.includes('correo no verificado') ||
          texto.includes('verificar tu correo') ||
          texto.includes('verifica tu correo')
        )
      );

    if (correoPendiente) {

      this.requiereVerificacion = true;

      this.requiereSegundoFactor = false;

      this.codigo = '';

      this.error = '';

      this.mensaje =
        'Tu correo aún no está verificado. ' +
        'Introduce el código recibido al registrarte.';

    } else if (
      datos.requiereSegundoFactor ||
      datos.requiereCodigoCorreo
    ) {

      this.procesarSolicitudSegundoFactor(datos);

      return;

    } else if (err.status === 0) {

      this.error =
        'No se pudo conectar con Spring Boot. ' +
        'Comprueba el puerto 8081.';

    } else {

      this.error =
        this.obtenerMensaje(datos) ||
        'No fue posible iniciar sesión. Revisa tus credenciales.';

    }

    this.actualizarVista();

  }

  // ======================================
  // 5. IDENTIFICAR TIPO DE SEGUNDO FACTOR
  // ======================================

  private procesarSolicitudSegundoFactor(
    datos: RespuestaAutenticacion
  ): void {

    const mensajeBackend =
      this.obtenerMensaje(datos);

    const texto = this.normalizarTexto(
      mensajeBackend
    );

    const metodo = this.normalizarTexto(
      datos.metodoSegundoFactor ||
      datos.tipoSegundoFactor ||
      ''
    );

    // ======================================
    // DETECTAR AUTENTICADOR TOTP ANTIGUO
    // ======================================

    const esTotp =

      metodo === 'totp' ||

      metodo === 'autenticador' ||

      texto.includes('aplicacion autenticadora') ||

      texto.includes('google authenticator') ||

      texto.includes('totp');

    // ======================================
    // DETECTAR CÓDIGO ENVIADO AL CORREO
    // ======================================

    const esCorreo =

      datos.requiereCodigoCorreo === true ||

      metodo === 'correo' ||

      metodo === 'email' ||

      (
        texto.includes('codigo') &&

        (
          texto.includes('correo') ||
          texto.includes('email')
        )
      );

    // ======================================
    // EVITAR CONFUNDIR TOTP CON CORREO
    // ======================================

    if (esTotp || !esCorreo) {

      this.requiereSegundoFactor = false;

      this.codigo = '';

      this.mensaje = '';

      this.error = esTotp

        ? 'El servidor todavía solicita un código ' +
        'de una aplicación autenticadora. ' +
        'Para utilizar un código por correo en ' +
        'cada inicio de sesión, primero debemos ' +
        'actualizar Spring Boot.'

        : 'El servidor solicitó una verificación ' +
        'adicional, pero no confirmó que el código ' +
        'se haya enviado al correo. ' +
        'Revisa la respuesta del backend.';

      this.actualizarVista();

      return;
    }

    // ======================================
    // MOSTRAR FORMULARIO DE CÓDIGO POR CORREO
    // ======================================

    const estabaVerificando =
      this.requiereSegundoFactor;

    this.requiereSegundoFactor = true;

    this.requiereVerificacion = false;

    this.codigo = '';

    // ======================================
    // CÓDIGO INCORRECTO
    // ======================================

    if (estabaVerificando) {

      this.error =
        mensajeBackend ||
        'El código es incorrecto o ha vencido. ' +
        'Inténtalo de nuevo.';

      this.mensaje = '';

    } else {

      // ======================================
      // PRIMER CÓDIGO SOLICITADO
      // ======================================

      this.error = '';

      this.mensaje =
        mensajeBackend ||
        'Revisa tu correo y escribe el código ' +
        'de seis dígitos para continuar.';

    }

    this.actualizarVista();

  }

  // ======================================
  // 6. VERIFICAR CORREO DEL REGISTRO
  // POST /auth/verificar
  // ======================================

  verificarCorreo(): void {

    if (this.cargando) {
      return;
    }

    if (!this.codigoValido()) {

      this.error =
        'Introduce el código de 6 dígitos de tu registro.';

      this.actualizarVista();

      return;
    }

    const correo =
      this.correo.trim();

    const contrasenia =
      this.contrasenia;

    const codigo =
      this.codigo;

    let verificado = false;

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

        tap(() => {

          verificado = true;

          this.requiereVerificacion = false;

          this.codigo = '';

          this.mensaje =
            'Correo verificado. Comprobando el acceso...';

          this.actualizarVista();

        }),

        // Iniciar sesión nuevamente.
        // El backend deberá exigir el OTP de login
        // antes de entregar el JWT.

        switchMap(() =>

          this.auth.login({

            correo,

            contrasenia

          }).pipe(

            timeout(15000)

          )

        ),

        finalize(() => {

          this.cargando = false;

          this.actualizarVista();

        }),

        takeUntilDestroyed(this.destroyRef)

      )
      .subscribe({

        next: respuesta => {

          this.procesarRespuestaLogin(respuesta);

        },

        error: err => {

          if (verificado) {

            this.procesarErrorLogin(err);

          } else {

            this.error = this.mensajeError(
              err,
              'El código de registro es incorrecto o venció.'
            );

            this.actualizarVista();

          }

        }

      });

  }

  // ======================================
  // 7. REENVIAR CÓDIGO DEL REGISTRO
  // POST /auth/reenviarcodigo
  // ======================================
  // No reenvía el código del inicio de sesión.

  reenviarCodigo(): void {

    if (
      this.cargando ||
      this.reenviando
    ) {
      return;
    }

    if (!this.correo.trim()) {

      this.error =
        'Primero escribe tu correo electrónico.';

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

        }),

        takeUntilDestroyed(this.destroyRef)

      )
      .subscribe({

        next: respuesta => {

          this.requiereVerificacion = true;

          this.requiereSegundoFactor = false;

          this.codigo = '';

          this.mensaje =
            respuesta.message ||
            'Revisa tu correo electrónico.';

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
  // 10. CAMBIAR CÓDIGO
  // ======================================

  cambiarCodigo(valor: string): void {

    this.codigo = (valor || '')
      .replace(/\D/g, '')
      .slice(0, 6);

    this.error = '';

  }

  // ======================================
  // 11. CANCELAR SEGUNDO FACTOR
  // ======================================

  cancelarSegundoFactor(): void {

    this.requiereSegundoFactor = false;

    this.codigo = '';

    this.error = '';

    this.mensaje = '';

    this.actualizarVista();

  }

  // ======================================
  // 12. REINICIAR VERIFICACIÓN
  // ======================================

  private reiniciarVerificacion(): void {

    this.requiereVerificacion = false;

    this.requiereSegundoFactor = false;

    this.codigo = '';

    this.error = '';

    this.mensaje = '';

  }

  // ======================================
  // 13. VALIDAR CÓDIGO
  // ======================================

  private codigoValido(): boolean {

    return /^\d{6}$/.test(
      this.codigo.trim()
    );

  }

  // ======================================
  // 14. GUARDAR SESIÓN JWT
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
        'El servidor no devolvió los datos ' +
        'necesarios de la sesión.';

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
            'El rol recibido por el servidor no es válido.';

          this.actualizarVista();

      }

    } catch {

      this.error =
        'No se pudo establecer la sesión ' +
        'con el JWT recibido.';

      this.actualizarVista();

    }

  }

  // ======================================
  // 15. INICIALIZAR GOOGLE
  // ======================================

  ngAfterViewInit(): void {

    this.auth.configuracionGoogle()
      .pipe(

        timeout(15000),

        takeUntilDestroyed(this.destroyRef)

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

        error: err => {

          this.googleError = this.mensajeError(
            err,
            'No se pudo obtener la configuración de Google.'
          );

          this.actualizarVista();

        }

      });

  }

  // ======================================
  // 16. CAMBIAR ROL DE GOOGLE
  // ======================================

  cambiarRolGoogle(valor: string): void {

    if (
      valor === 'HUESPED' ||
      valor === 'ANFITRION'
    ) {

      this.rolGoogle = valor;

    }

  }

  // ======================================
  // 17. CARGAR GOOGLE IDENTITY SERVICES
  // ======================================

  private cargarLibreriaGoogle(): Promise<void> {

    if (window.google?.accounts?.id) {

      return Promise.resolve();

    }

    return new Promise((resolve, reject) => {

      const previo =
        document.querySelector<HTMLScriptElement>(
          'script[src="https://accounts.google.com/gsi/client"]'
        );

      if (previo) {

        if (window.google?.accounts?.id) {

          resolve();

        } else {

          previo.addEventListener(
            'load',
            () => resolve(),
            { once: true }
          );

          previo.addEventListener(
            'error',
            () => reject(
              new Error('No se pudo cargar Google.')
            ),
            { once: true }
          );

        }

        return;
      }

      const script =
        document.createElement('script');

      script.src =
        'https://accounts.google.com/gsi/client';

      script.async = true;

      script.defer = true;

      script.onload = () =>

        window.google?.accounts?.id

          ? resolve()

          : reject(
            new Error(
              'Google Identity Services no está disponible.'
            )
          );

      script.onerror = () =>

        reject(
          new Error(
            'No se pudo descargar Google Identity Services.'
          )
        );

      document.head.appendChild(script);

    });

  }

  // ======================================
  // 18. DIBUJAR BOTÓN GOOGLE
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

      if (!sdk || !destino) {

        throw new Error(
          'No se encontró el botón de Google.'
        );

      }

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
      // BOTÓN BLANCO TIPO CÁPSULA
      // ======================================

      sdk.renderButton(destino, {

        type: 'standard',

        theme: 'outline',

        shape: 'pill',

        size: 'large',

        text: 'continue_with',

        logo_alignment: 'left',

        width: 230,

        locale: 'es'

      });

      this.googleDisponible = true;

      this.googleError = '';

      this.actualizarVista();

    } catch {

      this.googleError =
        'No fue posible cargar Google. ' +
        'Revisa tu conexión y el Client ID.';

      this.actualizarVista();

    }

  }

  // ======================================
  // 19. INICIAR SESIÓN CON GOOGLE
  // POST /auth/google
  // ======================================

  private ejecutarLoginGoogle(): void {

    if (
      !this.googleIdToken ||
      this.googleCargando
    ) {
      return;
    }

    this.googleCargando = true;

    this.googleError = '';

    this.actualizarVista();

    this.auth.iniciarConGoogle(
      this.googleIdToken,
      this.rolGoogle
    )
      .pipe(

        timeout(15000),

        finalize(() => {

          this.googleCargando = false;

          this.googleIdToken = '';

          this.actualizarVista();

        }),

        takeUntilDestroyed(this.destroyRef)

      )
      .subscribe({

        next: respuesta => {

          this.guardarSesion(respuesta);

        },

        error: err => {

          if (err instanceof HttpErrorResponse) {

            const datos =
              this.obtenerDatosError(err);

            this.googleError =
              datos.requiereSegundoFactor

                ? 'Este servidor todavía solicita ' +
                'un autenticador en algunas cuentas Google. ' +
                'Para mantener Google independiente, ' +
                'debemos ajustar esa regla en Spring Boot.'

                : this.obtenerMensaje(datos) ||
                'No fue posible iniciar sesión con Google.';

          } else {

            this.googleError =
              this.mensajeError(
                err,
                'No fue posible iniciar sesión con Google.'
              );

          }

          this.actualizarVista();

        }

      });

  }

  // ======================================
  // 20. INTERPRETAR RESPUESTAS DEL BACKEND
  // ======================================

  private obtenerDatosError(
    err: HttpErrorResponse
  ): RespuestaAutenticacion {

    let datos: unknown =
      err.error;

    if (typeof datos === 'string') {

      try {

        datos = JSON.parse(datos);

      } catch {

        return {
          message: String(datos)
        };

      }

    }

    return (
      datos !== null &&
      typeof datos === 'object' &&
      !Array.isArray(datos)
    )

      ? datos as RespuestaAutenticacion

      : {};

  }

  // ======================================
  // 21. OBTENER MENSAJE
  // ======================================

  private obtenerMensaje(
    datos: RespuestaAutenticacion
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
  // 22. NORMALIZAR MENSAJES
  // ======================================

  private normalizarTexto(
    texto: string
  ): string {

    return texto
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

  }

  // ======================================
  // 23. MENSAJES DE ERROR
  // ======================================

  private mensajeError(
    err: unknown,
    defecto: string
  ): string {

    if (this.esTimeout(err)) {

      return 'El servidor tardó demasiado en responder.';

    }

    if (!(err instanceof HttpErrorResponse)) {

      return defecto;

    }

    if (err.status === 0) {

      return 'No se pudo conectar con Spring Boot.';

    }

    return (
      this.obtenerMensaje(
        this.obtenerDatosError(err)
      ) ||
      defecto
    );

  }

  // ======================================
  // 24. DETECTAR TIMEOUT
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
  // 25. ACTUALIZAR VISTA
  // ======================================

  private actualizarVista(): void {

    this.cdr.markForCheck();

  }

}
