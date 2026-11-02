
import { Routes } from '@angular/router';

// ======================================
// GUARD DE AUTENTICACIÓN Y ROLES
// ======================================

import { roleGuard } from './guards/role.guard';

// ======================================
// AUTENTICACIÓN
// ======================================

import { Register } from './register/register';
import { Login } from './login/login';

// ======================================
// HUÉSPED
// ======================================

import { Home } from './pages/guest/home/home';
import { Explore } from './pages/guest/explore/explore';
import { MapView } from './pages/guest/map-view/map-view';
import { Detail } from './pages/guest/detail/detail';
import { Favorites } from './pages/guest/favorites/favorites';
import { Messages } from './pages/guest/messages/messages';
import { ProfileView } from './pages/guest/profile-view/profile-view';

import {
  Notifications as GuestNotifications
} from './pages/guest/notifications/notifications';

import {
  Reservations as GuestReservations
} from './pages/guest/reservations/reservations';

import {
  ReservationDetail as GuestReservationDetail
} from './pages/guest/reservation-detail/reservation-detail';

import {
  Reviews as GuestReviews
} from './pages/guest/reviews/reviews';

// ======================================
// ANFITRIÓN
// ======================================

// CREAR ALOJAMIENTO
import {
  CreateListing
} from './pages/host/create-listing/create-listing';

// ACTUALIZAR ALOJAMIENTO
import {
  Update
} from './pages/host/update/update';

// VER DETALLES DEL ALOJAMIENTO
import {
  Details
} from './pages/host/details/details';

// ADMINISTRAR DISPONIBILIDAD
import {
  Availability
} from './pages/host/availability/availability';

// DASHBOARD DEL ANFITRIÓN
import {
  Dashboard as HostDashboard
} from './pages/host/dashboard/dashboard';

// LISTADO DE ALOJAMIENTOS
import {
  Listings as HostListings
} from './pages/host/listings/listings';

// RESERVAS DEL ANFITRIÓN
import {
  Reservations as HostReservations
} from './pages/host/reservations/reservations';

import {
  ReservationDetail as HostReservationDetail
} from './pages/host/reservation-detail/reservation-detail';

// RESEÑAS DEL ANFITRIÓN
import {
  Reviews as HostReviews
} from './pages/host/reviews/reviews';

// ======================================
// ADMINISTRADOR
// ======================================

import {
  AdminArea
} from './pages/admin/admin-area/admin-area';

import {
  Audit
} from './pages/admin/audit/audit';

import {
  Reports
} from './pages/admin/reports/reports';

import {
  Users
} from './pages/admin/users/users';

import {
  UserDetail
} from './pages/admin/user-detail/user-detail';

import {
  ListingDetail
} from './pages/admin/listing-detail/listing-detail';

import {
  Dashboard as AdminDashboard
} from './pages/admin/dashboard/dashboard';

import {
  Listings as AdminListings
} from './pages/admin/listings/listings';

import {
  Reservations as AdminReservations
} from './pages/admin/reservations/reservations';

import {
  Reviews as AdminReviews
} from './pages/admin/reviews/reviews';

// ======================================
// CONFIGURACIÓN GENERAL DE RUTAS
// ======================================

export const routes: Routes = [

  // ====================================
  // REDIRECCIÓN INICIAL
  // ====================================

  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },

  // ====================================
  // RUTAS PÚBLICAS
  // ====================================

  {
    path: 'home',
    component: Home
  },

  {
    path: 'explorar',
    component: Explore
  },

  {
    path: 'mapa/:id',
    component: MapView
  },

  {
    path: 'alojamiento/:id',
    component: Detail
  },

  // ====================================
  // LOGIN Y REGISTRO
  // ====================================

  {
    path: 'login',
    component: Login
  },

  {
    path: 'registro',
    component: Register
  },

  // ====================================
  // PERFIL DE USUARIO
  // ====================================

  {
    path: 'perfil',
    component: ProfileView,
    canActivate: [roleGuard]
  },

  // ====================================
  // NOTIFICACIONES
  // ====================================

  {
    path: 'notificaciones',
    component: GuestNotifications,
    canActivate: [roleGuard]
  },

  // ====================================
  // RUTAS EXCLUSIVAS DE HUÉSPED
  // ====================================

  {
    path: 'favoritos',
    component: Favorites,
    canActivate: [roleGuard],
    data: {
      roles: ['Huésped']
    }
  },

  {
    path: 'mensajes',
    component: Messages,
    canActivate: [roleGuard],
    data: {
      roles: ['Huésped']
    }
  },

  {
    path: 'reservas',
    component: GuestReservations,
    canActivate: [roleGuard],
    data: {
      roles: ['Huésped']
    }
  },

  {
    path: 'reservas/:id',
    component: GuestReservationDetail,
    canActivate: [roleGuard],
    data: {
      roles: ['Huésped']
    }
  },

  {
    path: 'resenas',
    component: GuestReviews,
    canActivate: [roleGuard],
    data: {
      roles: ['Huésped']
    }
  },

  // ====================================
  // RUTAS EXCLUSIVAS DE ANFITRIÓN
  // ====================================

  // DASHBOARD
  {
    path: 'host/dashboard',
    component: HostDashboard,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // LISTADO DE ALOJAMIENTOS
  {
    path: 'host/alojamientos',
    component: HostListings,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // ====================================
  // CREAR ALOJAMIENTO
  // ====================================

  {
    path: 'host/alojamientos/crear',
    component: CreateListing,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // ====================================
  // EDITAR ALOJAMIENTO
  // CARPETA: host/update
  // ====================================

  {
    path: 'host/alojamientos/:id/editar',
    component: Update,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // ====================================
  // DISPONIBILIDAD DEL ALOJAMIENTO
  // CARPETA: host/availability
  // ====================================

  {
    path: 'host/alojamientos/:id/disponibilidad',
    component: Availability,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // ====================================
  // VER DETALLES DEL ALOJAMIENTO
  // CARPETA: host/details
  // ====================================

  {
    path: 'host/alojamientos/:id',
    component: Details,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // ====================================
  // RESERVAS DEL ANFITRIÓN
  // ====================================

  {
    path: 'host/reservas',
    component: HostReservations,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  {
    path: 'host/reservas/:id',
    component: HostReservationDetail,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // ====================================
  // RESEÑAS DEL ANFITRIÓN
  // ====================================

  {
    path: 'host/resenas',
    component: HostReviews,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },

  // ====================================
  // RUTAS EXCLUSIVAS DE ADMINISTRADOR
  // ====================================

  // ÁREA DE ADMINISTRACIÓN
  {
    path: 'admin',
    component: AdminArea,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // DASHBOARD ADMINISTRADOR
  {
    path: 'admin/dashboard',
    component: AdminDashboard,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // USUARIOS
  {
    path: 'admin/usuarios',
    component: Users,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  {
    path: 'admin/usuarios/:id',
    component: UserDetail,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // ALOJAMIENTOS
  {
    path: 'admin/alojamientos',
    component: AdminListings,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  {
    path: 'admin/alojamientos/:id',
    component: ListingDetail,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // RESERVAS
  {
    path: 'admin/reservas',
    component: AdminReservations,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // RESEÑAS
  {
    path: 'admin/resenas',
    component: AdminReviews,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // REPORTES
  {
    path: 'admin/reportes',
    component: Reports,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // AUDITORÍA
  {
    path: 'admin/auditoria',
    component: Audit,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },

  // ====================================
  // RUTA NO ENCONTRADA
  // ====================================

  {
    path: '**',
    redirectTo: 'home'
  }

];
