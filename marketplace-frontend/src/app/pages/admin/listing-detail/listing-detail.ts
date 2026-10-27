import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { photos } from '../../../services/mockData';

export type ListingStatus = 'Activo' | 'Pausado' | 'Bloqueado' | 'Pendiente' | 'Rechazado';
export type ApprovalStatus = 'Aprobado' | 'Pendiente de aprobación' | 'Rechazado';

export interface AdminListing {
  id: string;
  name: string;
  image: string;
  description: string;
  type: string;
  city: string;
  location: string;
  capacity: number;
  rooms: number;
  baths: number;
  services: string[];
  price: string;
  rating: number | string;
  reviews: number;
  reservations: number;
  host: string;
  hostEmail: string;
  hostId: string;
  hostStatus: string;
  status: ListingStatus;
  approvalStatus?: ApprovalStatus;
  rejectionReason?: string;
  submitted?: string;
  published: string;
  updated: string;
}

type ListingActionKind = 'approveListing' | 'rejectListing' | 'blockListing' | 'activateListing';

const ACTIONS = {
  approveListing: {
    title: 'Aprobar alojamiento',
    text: '¿Confirmas que deseas aprobar esta publicación?',
    label: '',
    confirm: 'Confirmar aprobación',
    needsReason: false,
  },
  rejectListing: {
    title: 'Rechazar alojamiento',
    text: 'La publicación no aparecerá en los resultados públicos.',
    label: 'Motivo del rechazo',
    confirm: 'Confirmar rechazo',
    needsReason: true,
  },
  blockListing: {
    title: 'Bloquear publicación',
    text: 'Esta acción impedirá que el alojamiento aparezca en las búsquedas mientras esté bloqueado.',
    label: 'Motivo del bloqueo',
    confirm: 'Confirmar bloqueo',
    needsReason: true,
  },
  activateListing: {
    title: 'Activar publicación',
    text: '¿Deseas habilitar nuevamente esta publicación?',
    label: '',
    confirm: 'Confirmar activación',
    needsReason: false,
  },
};

@Component({
  selector: 'app-admin-listing-detail',
  standalone: true,
  templateUrl: './listing-detail.html',
})
export class ListingDetail {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly photos = photos;

  /** Parámetro :id de la ruta admin/alojamientos/:id */
  readonly id = this.route.snapshot.paramMap.get('id');

  // TODO: cargar el alojamiento desde el backend usando `id`
  listing = signal<AdminListing>({
    id: '',
    name: '',
    image: '',
    description: '',
    type: '',
    city: '',
    location: '',
    capacity: 0,
    rooms: 0,
    baths: 0,
    services: [],
    price: '',
    rating: '',
    reviews: 0,
    reservations: 0,
    host: '',
    hostEmail: '',
    hostId: '',
    hostStatus: '',
    status: 'Activo',
    published: '',
    updated: '',
  });

  info = computed(() => {
    const l = this.listing();
    return [
      { label: 'Tipo', value: l.type },
      { label: 'Ciudad', value: l.city },
      { label: 'Ubicación', value: l.location },
      { label: 'Capacidad', value: `${l.capacity} huéspedes` },
      { label: 'Habitaciones', value: String(l.rooms) },
      { label: 'Baños', value: String(l.baths) },
    ];
  });

  isPendingApproval = computed(() => this.listing().approvalStatus === 'Pendiente de aprobación');

  badgeClass = computed(() => {
    const s = this.listing().status;
    const tone =
      s === 'Activo'
        ? 'bg-emerald-50 text-emerald-700'
        : s === 'Bloqueado'
          ? 'bg-red-50 text-red-700'
          : s === 'Pausado'
            ? 'bg-slate-100 text-slate-600'
            : 'bg-amber-50 text-amber-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  });

  // ---- Modal de confirmación ----
  action = signal<ListingActionKind | null>(null);
  reason = signal('');
  modal = computed(() => {
    const a = this.action();
    return a ? ACTIONS[a] : null;
  });

  back() {
    this.router.navigateByUrl('/admin/alojamientos');
  }

  askApproval(approved: boolean) {
    this.reason.set('');
    this.action.set(approved ? 'approveListing' : 'rejectListing');
  }

  askAction() {
    this.reason.set('');
    this.action.set(this.listing().status === 'Bloqueado' ? 'activateListing' : 'blockListing');
  }

  closeAction() {
    this.action.set(null);
  }

  confirmAction() {
    const a = this.action();
    if (!a) return;
    const today = new Date().toLocaleDateString('es-CO', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });

    if (a === 'approveListing' || a === 'rejectListing') {
      const approved = a === 'approveListing';
      this.listing.update((l) => ({
        ...l,
        status: (approved ? 'Activo' : 'Pendiente') as ListingStatus,
        approvalStatus: (approved ? 'Aprobado' : 'Rechazado') as ApprovalStatus,
        rejectionReason: approved ? undefined : this.reason(),
        updated: today,
        published: approved ? today : l.published,
      }));
    } else {
      const next: ListingStatus = a === 'blockListing' ? 'Bloqueado' : 'Activo';
      this.listing.update((l) => ({ ...l, status: next, updated: today }));
    }
    this.action.set(null);
    // TODO: guardar en el backend, registrar auditoría y avisar
  }
}
