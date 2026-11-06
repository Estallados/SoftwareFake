
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './services/auth.interceptor';

// ======================================
// CONFIGURACIÓN GLOBAL DE ANGULAR
// ======================================

export const appConfig: ApplicationConfig = {

  providers: [

    // ======================================
    // CONFIGURACIÓN DE RUTAS
    // ======================================

    provideRouter(routes),

    // ======================================
    // CLIENTE HTTP CON INTERCEPTOR JWT
    // ======================================

    provideHttpClient(
      withInterceptors([authInterceptor])
    )

  ]

};
