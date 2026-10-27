import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

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
  selector: 'app-admin-users',
  standalone: true,
  templateUrl: './users.html',
})
export class Users {
  private router = inject(Router);

  // TODO: reemplazar por los usuarios del backend
  users = signal<AdminUser[]>([]);

  search = signal('');
  roleFilter = signal('Todos');
  statusFilter = signal('Todos');

  readonly headers = [
    'Nombre',
    'Correo electrónico',
    'Rol',
    'Estado',
    'Fecha de registro',
    'Acciones',
  ];

  filtered = computed(() => {
    const term = this.search().toLowerCase();
    return this.users().filter(
      (u) =>
        `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(term) &&
        (this.roleFilter() === 'Todos' || u.role === this.roleFilter()) &&
        (this.statusFilter() === 'Todos' || u.status === this.statusFilter()),
    );
  });

  // ---- Modal de confirmación ----
  action = signal<{ kind: UserActionKind; id: string } | null>(null);
  reason = signal('');
  modal = computed(() => {
    const a = this.action();
    return a ? ACTIONS[a.kind] : null;
  });

  open(id: string) {
    this.router.navigate(['/admin/usuarios', id]);
  }

  askAction(u: AdminUser) {
    this.reason.set('');
    this.action.set({ kind: u.status === 'Activo' ? 'restrictUser' : 'activateUser', id: u.id });
  }

  closeAction() {
    this.action.set(null);
  }

  confirmAction() {
    const a = this.action();
    if (!a) return;
    const next: UserStatus = a.kind === 'restrictUser' ? 'Restringido' : 'Activo';
    this.users.update((list) => list.map((u) => (u.id === a.id ? { ...u, status: next } : u)));
    this.action.set(null);
    // TODO: guardar en el backend (motivo: this.reason()), registrar auditoría y avisar
  }

  statusClass(s: UserStatus) {
    const tone =
      s === 'Activo'
        ? 'bg-emerald-50 text-emerald-700'
        : s === 'Restringido'
          ? 'bg-red-50 text-red-700'
          : 'bg-slate-100 text-slate-600';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }
}
