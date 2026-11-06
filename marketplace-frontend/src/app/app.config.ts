import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import { routes } from './app.routes';
import {authInterceptor} from './services/auth.interceptor';

/**
 * Configuración de la aplicación: el cliente HTTP con el interceptor que agrega el token JWT y las
 * rutas.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
    provideRouter(routes),
    provideHttpClient(),
  ],
};
