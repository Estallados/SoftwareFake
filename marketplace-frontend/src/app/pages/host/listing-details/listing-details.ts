import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

export type ListingStatus = 'Activo' | 'Pausado' | 'Bloqueado' | 'Pendiente' | 'Rechazado';
export type ApprovalStatus = 'Aprobado' | 'Pendiente de aprobación' | 'Rechazado';

export interface HostListing {
  id: string;
  name: string;
  image: string;
  photos?: string[];
  city: string;
  type: string;
  location: string;
  description: string;
  capacity: number;
  rooms: number;
  baths: number;
  amenities?: string[];
  price: string;
  additionalCosts?: string;
  rules?: string;
  smokingRestrictions?: string;
  partyRestrictions?: string;
  checkin?: string;
  checkout?: string;
  cancellation?: string;
  status: ListingStatus;
  approvalStatus?: ApprovalStatus;
  published: string;
  updated: string;
  rating: number | string;
  reviews: number;
  reservations: number;
}

@Component({
  selector: 'app-host-listing-details',
  standalone: true,
  templateUrl: './listing-details.html',
})
export class ListingDetails {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /** Parámetro :id de la ruta host/alojamientos/:id */
  readonly id = this.route.snapshot.paramMap.get('id');

  // TODO: cargar el alojamiento desde el backend usando `id`
  listing = signal<HostListing>({
    id: '',
    name: '',
    image: '',
    city: '',
    type: '',
    location: '',
    description: '',
    capacity: 0,
    rooms: 0,
    baths: 0,
    price: '',
    status: 'Activo',
    published: '',
    updated: '',
    rating: '',
    reviews: 0,
    reservations: 0,
  });

  gallery = computed(() => {
    const l = this.listing();
    return l.photos?.length ? l.photos : [l.image];
  });

  /** Las 4 miniaturas de la galería (repite la primera si faltan fotos) */
  thumbs = computed(() => {
    const g = this.gallery();
    return [g[1] ?? g[0], g[2] ?? g[0], g[0], g[1] ?? g[0]];
  });

  general = computed(() => {
    const l = this.listing();
    return [
      { label: 'Tipo', value: l.type },
      { label: 'Ciudad', value: l.city },
      { label: 'Ubicación', value: l.location },
      { label: 'Capacidad máxima', value: `${l.capacity} huéspedes` },
      { label: 'Habitaciones', value: String(l.rooms) },
      { label: 'Baños', value: String(l.baths) },
    ];
  });

  conditions = computed(() => {
    const l = this.listing();
    return [
      { label: 'Precio por noche', value: l.price },
      { label: 'Costos adicionales', value: l.additionalCosts ?? 'No configurados' },
      { label: 'Reglas', value: l.rules ?? 'No configuradas' },
      { label: 'Fumadores', value: l.smokingRestrictions ?? 'No configurado' },
      { label: 'Fiestas y eventos', value: l.partyRestrictions ?? 'No configurado' },
      { label: 'Hora de entrada', value: l.checkin ?? '15:00' },
      { label: 'Hora de salida', value: l.checkout ?? '11:00' },
      { label: 'Política de cancelación', value: l.cancellation ?? 'Flexible' },
    ];
  });

  activity = computed(() => {
    const l = this.listing();
    return [
      { label: 'Estado actual', value: l.status },
      { label: 'Publicación', value: l.published },
      { label: 'Última actualización', value: l.updated },
      { label: 'Calificación', value: `★ ${l.rating}` },
      { label: 'Reseñas', value: String(l.reviews) },
      { label: 'Reservas', value: String(l.reservations) },
    ];
  });

  badgeClass = computed(() => {
    const l = this.listing();
    let tone: string;
    if (l.approvalStatus) {
      tone =
        l.approvalStatus === 'Aprobado'
          ? 'bg-emerald-50 text-emerald-700'
          : l.approvalStatus === 'Rechazado'
            ? 'bg-red-50 text-red-700'
            : 'bg-amber-50 text-amber-700';
    } else {
      tone =
        l.status === 'Activo'
          ? 'bg-emerald-50 text-emerald-700'
          : l.status === 'Bloqueado'
            ? 'bg-red-50 text-red-700'
            : l.status === 'Pausado'
              ? 'bg-slate-100 text-slate-600'
              : 'bg-amber-50 text-amber-700';
    }
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  });

  back() {
    this.router.navigateByUrl('/host/alojamientos');
  }

  edit() {
    this.router.navigate(['/host/alojamientos', this.id, 'editar']);
  }

  availability() {
    this.router.navigate(['/host/alojamientos', this.id, 'disponibilidad']);
  }
}
