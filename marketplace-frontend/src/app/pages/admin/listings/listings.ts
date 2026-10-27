import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

export type ListingStatus = 'Activo' | 'Pausado' | 'Bloqueado' | 'Pendiente' | 'Rechazado';
export type ApprovalStatus = 'Aprobado' | 'Pendiente de aprobación' | 'Rechazado';

export interface AdminListing {
  id: string;
  name: string;
  image: string;
  host: string;
  city: string;
  type: string;
  price: string;
  rating: number | string;
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
  selector: 'app-admin-listings',
  standalone: true,
  templateUrl: './listings.html',
})
export class Listings {
  private router = inject(Router);

  // TODO: reemplazar por los alojamientos del backend
  listings = signal<AdminListing[]>([]);

  search = signal('');
  statusFilter = signal('Todos');
  typeFilter = signal('Todos');
  cityFilter = signal('Todas');

  readonly headers = [
    'Imagen y alojamiento', 'Anfitrión', 'Ciudad', 'Tipo', 'Precio / noche',
    'Fecha de envío', 'Estado', 'Calificación', 'Acciones',
  ];

  cities = computed(() => Array.from(new Set(this.listings().map((l) => l.city))));

  filtered = computed(() => {
    const term = this.search().toLowerCase();
    return this.listings().filter(
      (l) =>
        `${l.name} ${l.host}`.toLowerCase().includes(term) &&
        (this.statusFilter() === 'Todos' ||
          l.status === this.statusFilter() ||
          l.approvalStatus === this.statusFilter()) &&
        (this.typeFilter() === 'Todos' || l.type === this.typeFilter()) &&
        (this.cityFilter() === 'Todas' || l.city === this.cityFilter()),
    );
  });

  pendingCount = computed(
    () => this.listings().filter((l) => l.approvalStatus === 'Pendiente de aprobación').length,
  );
  approvedCount = computed(
    () => this.listings().filter((l) => !l.approvalStatus || l.approvalStatus === 'Aprobado').length,
  );
  rejectedCount = computed(
    () => this.listings().filter((l) => l.approvalStatus === 'Rechazado').length,
  );

  // ---- Modal de confirmación ----
  action = signal<{ kind: ListingActionKind; id: string } | null>(null);
  reason = signal('');
  modal = computed(() => {
    const a = this.action();
    return a ? ACTIONS[a.kind] : null;
  });

  open(id: string) {
    this.router.navigate(['/admin/alojamientos', id]);
  }

  askApproval(id: string, approved: boolean) {
    this.reason.set('');
    this.action.set({ kind: approved ? 'approveListing' : 'rejectListing', id });
  }

  askAction(l: AdminListing) {
    this.reason.set('');
    this.action.set({ kind: l.status === 'Bloqueado' ? 'activateListing' : 'blockListing', id: l.id });
  }

  closeAction() {
    this.action.set(null);
  }

  confirmAction() {
    const a = this.action();
    if (!a) return;
    const today = new Date().toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });

    if (a.kind === 'approveListing' || a.kind === 'rejectListing') {
      const approved = a.kind === 'approveListing';
      this.listings.update((list) =>
        list.map((l) =>
          l.id === a.id
            ? {
              ...l,
              status: (approved ? 'Activo' : 'Pendiente') as ListingStatus,
              approvalStatus: (approved ? 'Aprobado' : 'Rechazado') as ApprovalStatus,
              rejectionReason: approved ? undefined : this.reason(),
              updated: today,
              published: approved ? today : l.published,
            }
            : l,
        ),
      );
    } else {
      const next: ListingStatus = a.kind === 'blockListing' ? 'Bloqueado' : 'Activo';
      this.listings.update((list) =>
        list.map((l) => (l.id === a.id ? { ...l, status: next, updated: today } : l)),
      );
    }
    this.action.set(null);
    // TODO: guardar en el backend, registrar auditoría y avisar
  }

  badgeClass(l: AdminListing) {
    const tone =
      l.approvalStatus === 'Rechazado'
        ? 'bg-red-50 text-red-700'
        : l.approvalStatus === 'Pendiente de aprobación'
          ? 'bg-amber-50 text-amber-700'
          : l.status === 'Activo'
            ? 'bg-emerald-50 text-emerald-700'
            : l.status === 'Bloqueado'
              ? 'bg-red-50 text-red-700'
              : l.status === 'Pausado'
                ? 'bg-slate-100 text-slate-600'
                : 'bg-amber-50 text-amber-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }
}
