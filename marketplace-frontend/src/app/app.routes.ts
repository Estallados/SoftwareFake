
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

import {
  CreateListing
} from './pages/host/create-listing/create-listing';

import {
  EditListing
} from './pages/host/edit-listing/edit-listing';

import {
  ListingDetails
} from './pages/host/listing-details/listing-details';

import {
  Availability
} from './pages/host/availability/availability';

import {
  Dashboard as HostDashboard
} from './pages/host/dashboard/dashboard';

import {
  Listings as HostListings
} from './pages/host/listings/listings';

import {
  Reservations as HostReservations
} from './pages/host/reservations/reservations';

import {
  ReservationDetail as HostReservationDetail
} from './pages/host/reservation-detail/reservation-detail';

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
// CONFIGURACIÓN DE RUTAS
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
  // No necesitan iniciar sesión
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
  // PERFIL
  // Disponible para cualquier usuario
  // autenticado
  // ====================================

  {
    path: 'perfil',
    component: ProfileView,
    canActivate: [roleGuard]
  },

  // ====================================
  // NOTIFICACIONES
  // Disponible para usuarios autenticados
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

  {
    path: 'host/dashboard',
    component: HostDashboard,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },
  {
    path: 'host/alojamientos',
    component: HostListings,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },
  {
    path: 'host/alojamientos/crear',
    component: CreateListing,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },
  {
    path: 'host/alojamientos/:id',
    component: ListingDetails,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },
  {
    path: 'host/alojamientos/:id/editar',
    component: EditListing,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },
  {
    path: 'host/alojamientos/:id/disponibilidad',
    component: Availability,
    canActivate: [roleGuard],
    data: {
      roles: ['Anfitrión']
    }
  },
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

  {
    path: 'admin',
    component: AdminArea,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },
  {
    path: 'admin/dashboard',
    component: AdminDashboard,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },
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
  {
    path: 'admin/reservas',
    component: AdminReservations,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },
  {
    path: 'admin/resenas',
    component: AdminReviews,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },
  {
    path: 'admin/reportes',
    component: Reports,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },
  {
    path: 'admin/auditoria',
    component: Audit,
    canActivate: [roleGuard],
    data: {
      roles: ['Administrador']
    }
  },


  {
    path: '**',
    redirectTo: 'home'
  }

];
