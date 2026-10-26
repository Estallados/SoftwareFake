import { Routes } from '@angular/router';

import { Home } from './pages/guest/home/home';
import { Explore } from './pages/guest/explore/explore';
import { MapView } from './pages/guest/map-view/map-view';
import { Detail } from './pages/guest/detail/detail';
import { Favorites } from './pages/guest/favorites/favorites';
import { Messages } from './pages/guest/messages/messages';
import { ProfileView } from './pages/guest/profile-view/profile-view';
import { Notifications as GuestNotifications } from './pages/guest/notifications/notifications';
import { Reservations as GuestReservations } from './pages/guest/reservations/reservations';
import { ReservationDetail as GuestReservationDetail } from './pages/guest/reservation-detail/reservation-detail';
import { Reviews as GuestReviews } from './pages/guest/reviews/reviews';

import { HostArea } from './pages/host/host-area/host-area';
import { CreateListing } from './pages/host/create-listing/create-listing';
import { EditListing } from './pages/host/edit-listing/edit-listing';
import { ListingDetails } from './pages/host/listing-details/listing-details';
import { Availability } from './pages/host/availability/availability';
import { Dashboard as HostDashboard } from './pages/host/dashboard/dashboard';
import { Listings as HostListings } from './pages/host/listings/listings';
import { Notifications as HostNotifications } from './pages/host/notifications/notifications';
import { Reservations as HostReservations } from './pages/host/reservations/reservations';
import { ReservationDetail as HostReservationDetail } from './pages/host/reservation-detail/reservation-detail';
import { Reviews as HostReviews } from './pages/host/reviews/reviews';

import { AdminArea } from './pages/admin/admin-area/admin-area';
import { Audit } from './pages/admin/audit/audit';
import { Reports } from './pages/admin/reports/reports';
import { Users } from './pages/admin/users/users';
import { UserDetail } from './pages/admin/user-detail/user-detail';
import { ListingDetail } from './pages/admin/listing-detail/listing-detail';
import { Dashboard as AdminDashboard } from './pages/admin/dashboard/dashboard';
import { Listings as AdminListings } from './pages/admin/listings/listings';
import { Reservations as AdminReservations } from './pages/admin/reservations/reservations';
import { Reviews as AdminReviews } from './pages/admin/reviews/reviews';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full',
  },

  {
    path: 'home',
    component: Home,
  },
  {
    path: 'explorar',
    component: Explore,
  },
  {
    path: 'mapa/:id',
    component: MapView,
  },
  {
    path: 'alojamiento/:nombre',
    component: Detail,
  },
  {
    path: 'favoritos',
    component: Favorites,
  },
  {
    path: 'mensajes',
    component: Messages,
  },
  {
    path: 'notificaciones',
    component: GuestNotifications,
  },
  {
    path: 'perfil',
    component: ProfileView,
  },
  {
    path: 'reservas',
    component: GuestReservations,
  },
  {
    path: 'reservas/:id',
    component: GuestReservationDetail,
  },
  {
    path: 'resenas',
    component: GuestReviews,
  },

  {
    path: 'host',
    component: HostArea,
  },
  {
    path: 'host/dashboard',
    component: HostDashboard,
  },
  {
    path: 'host/alojamientos',
    component: HostListings,
  },
  {
    path: 'host/alojamientos/crear',
    component: CreateListing,
  },
  {
    path: 'host/alojamientos/:id',
    component: ListingDetails,
  },
  {
    path: 'host/alojamientos/:id/editar',
    component: EditListing,
  },
  {
    path: 'host/alojamientos/:id/disponibilidad',
    component: Availability,
  },
  {
    path: 'host/reservas',
    component: HostReservations,
  },
  {
    path: 'host/reservas/:id',
    component: HostReservationDetail,
  },
  {
    path: 'host/resenas',
    component: HostReviews,
  },
  {
    path: 'host/notificaciones',
    component: HostNotifications,
  },

  {
    path: 'admin',
    component: AdminArea,
  },
  {
    path: 'admin/dashboard',
    component: AdminDashboard,
  },
  {
    path: 'admin/usuarios',
    component: Users,
  },
  {
    path: 'admin/usuarios/:id',
    component: UserDetail,
  },
  {
    path: 'admin/alojamientos',
    component: AdminListings,
  },
  {
    path: 'admin/alojamientos/:id',
    component: ListingDetail,
  },
  {
    path: 'admin/reservas',
    component: AdminReservations,
  },
  {
    path: 'admin/resenas',
    component: AdminReviews,
  },
  {
    path: 'admin/reportes',
    component: Reports,
  },
  {
    path: 'admin/auditoria',
    component: Audit,
  },

  {
    path: '**',
    redirectTo: 'home',
  },
];
