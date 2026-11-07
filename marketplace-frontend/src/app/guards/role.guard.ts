
import { inject } from '@angular/core';

import {
  CanActivateFn,
  Router
} from '@angular/router';

import {
  RolUsuario,
  SessionService
} from '../services/session.service';

export const roleGuard: CanActivateFn = (route) => {

  const sessionService = inject(SessionService);
  const router = inject(Router);

  // Verificar que el usuario haya iniciado sesión.
  if (!sessionService.autenticado()) {
    return router.createUrlTree(['/login']);
  }

  // Obtener los roles permitidos para la ruta.
  const rolesPermitidos =
    route.data['roles'] as RolUsuario[] | undefined;

  // Si la ruta solo necesita autenticación.
  if (!rolesPermitidos || rolesPermitidos.length === 0) {
    return true;
  }

  // Verificar si el rol tiene permiso.
  if (sessionService.tieneRol(...rolesPermitidos)) {
    return true;
  }

  // Si no tiene permisos, regresar al inicio.
  return router.createUrlTree(['/home']);
};
