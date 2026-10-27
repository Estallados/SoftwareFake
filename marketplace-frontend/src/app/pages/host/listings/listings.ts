import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

export type ListingStatus = 'Activo' | 'Pausado' | 'Bloqueado' | 'Pendiente' | 'Rechazado';
export type ApprovalStatus = 'Aprobado' | 'Pendiente de aprobación' | 'Rechazado';

export interface HostListing {
  id: string;
  name: string;
  image: string;
  city: string;
  type: string;
  capacity: number;
  rooms: number;
  baths: number;
  price: string;
  rating: number | string;
  reviews: number;
  reservations: number;
  status: ListingStatus;
  approvalStatus?: ApprovalStatus;
  rejectionReason?: string;
  submitted?: string;
}

@Component({
  selector: 'app-host-listings',
  standalone: true,
  templateUrl: './listings.html',
})
export class Listings {
  private router = inject(Router);

  // TODO: reemplazar por los alojamientos del anfitrión (backend)
  listings = signal<HostListing[]>([]);

  total = computed(() => this.listings().length);
  active = computed(() => this.listings().filter((l) => l.status === 'Activo').length);
  paused = computed(() => this.listings().filter((l) => l.status === 'Pausado').length);
  blocked = computed(() => this.listings().filter((l) => l.status === 'Bloqueado').length);

  create() {
    this.router.navigateByUrl('/host/alojamientos/crear');
  }

  details(id: string) {
    this.router.navigate(['/host/alojamientos', id]);
  }

  edit(id: string) {
    this.router.navigate(['/host/alojamientos', id, 'editar']);
  }

  availability(id: string) {
    this.router.navigate(['/host/alojamientos', id, 'disponibilidad']);
  }

  badgeClass(l: HostListing) {
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
  }
}
