import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

export type UserRole = 'Huésped' | 'Anfitrión' | 'Administrador';
export type UserStatus = 'Activo' | 'Restringido' | 'Inactivo';

export interface AdminUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: UserRole;
  status: UserStatus;
  registered: string;
}

export interface HostListing {
  name: string;
  status: 'Activo' | 'Pausado';
}

type UserActionKind = 'restrictUser' | 'activateUser';

const ACTIONS = {
  restrictUser: {
    title: 'Restringir usuario',
    text: '¿Estás seguro de que deseas restringir esta cuenta?',
    label: 'Motivo de la restricción',
    confirm: 'Confirmar restricción',
    needsReason: true,
  },
  activateUser: {
    title: 'Activar usuario',
    text: '¿Deseas activar nuevamente esta cuenta?',
    label: '',
    confirm: 'Confirmar activación',
    needsReason: false,
  },
};

@Component({
  selector: 'app-admin-user-detail',
  standalone: true,
  templateUrl: './user-detail.html',
})
export class UserDetail {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /** Parámetro :id de la ruta admin/usuarios/:id */
  readonly id = this.route.snapshot.paramMap.get('id');

  // TODO: cargar el usuario desde el backend usando `id`
  user = signal<AdminUser>({
    id: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    role: 'Huésped',
    status: 'Activo',
    registered: '',
  });
  // Solo para anfitriones
  hostListings = signal<HostListing[]>([]);
  // Solo para huéspedes
  totalReservations = signal(0);
  completedStays = signal(0);

  info = computed(() => {
    const u = this.user();
    return [
      { label: 'Nombre', value: u.firstName },
      { label: 'Apellido', value: u.lastName },
      { label: 'Correo electrónico', value: u.email },
      { label: 'Teléfono', value: u.phone },
      { label: 'Rol', value: u.role },
      { label: 'Estado de la cuenta', value: u.status },
      { label: 'Fecha de registro', value: u.registered },
      { label: 'Identificador interno', value: u.id },
    ];
  });

  sectionTitle = computed(() => {
    const r = this.user().role;
    return r === 'Anfitrión'
      ? 'Alojamientos publicados'
      : r === 'Huésped'
        ? 'Resumen de reservas'
        : 'Cuenta administrativa';
  });

  statusClass = computed(() => {
    const s = this.user().status;
    const tone =
      s === 'Activo'
        ? 'bg-emerald-50 text-emerald-700'
        : s === 'Restringido'
          ? 'bg-red-50 text-red-700'
          : 'bg-slate-100 text-slate-600';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  });

  // ---- Modal de confirmación ----
  action = signal<UserActionKind | null>(null);
  reason = signal('');
  modal = computed(() => {
    const a = this.action();
    return a ? ACTIONS[a] : null;
  });

  back() {
    this.router.navigateByUrl('/admin/usuarios');
  }

  askAction() {
    this.reason.set('');
    this.action.set(this.user().status === 'Activo' ? 'restrictUser' : 'activateUser');
  }

  closeAction() {
    this.action.set(null);
  }

  confirmAction() {
    const a = this.action();
    if (!a) return;
    const next: UserStatus = a === 'restrictUser' ? 'Restringido' : 'Activo';
    this.user.update((u) => ({ ...u, status: next }));
    this.action.set(null);
    // TODO: guardar en el backend (motivo: this.reason()), registrar auditoría y avisar
  }
}
