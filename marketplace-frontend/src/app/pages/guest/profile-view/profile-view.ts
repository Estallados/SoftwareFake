
import {
  Component,
  computed,
  inject,
  signal
} from '@angular/core';

import { Router } from '@angular/router';
import { isValidEmail } from '../../../utils/validators';

export interface Profile {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  registered: string;
  active: boolean;
}

type EditableKey =
  | 'firstName'
  | 'lastName'
  | 'email'
  | 'phone';

@Component({
  selector: 'app-profile-view',
  standalone: true,
  templateUrl: './profile-view.html'
})
export class ProfileView {

  private readonly router = inject(Router);

  // ======================================
  // DATOS DE LA SESIÓN
  // ======================================

  private readonly correoSesion =
    sessionStorage.getItem('correo') ?? '';

  private readonly rolSesion =
    sessionStorage.getItem('rol') ?? '';

  readonly role = this.normalizarRol(this.rolSesion);

  // ======================================
  // PERFIL DEL USUARIO
  // ======================================

  profile = signal<Profile>({
    firstName: '',
    lastName: '',
    email: this.correoSesion,
    phone: '',
    registered: '',
    active: true
  });

  editing = signal(false);
  confirmAccount = signal(false);

  form = signal<Profile>({
    ...this.profile()
  });

  mensaje = signal('');
  error = signal('');

  // Indica que todavía no se ha consultado
  // el estado real de la cuenta al backend.
  estadoConsultado = signal(false);

  // ======================================
  // CAMPOS EDITABLES
  // ======================================

  readonly editFields: {
    label: string;
    key: EditableKey;
  }[] = [
    { label: 'Nombre', key: 'firstName' },
    { label: 'Apellido', key: 'lastName' },
    { label: 'Correo electrónico', key: 'email' },
    { label: 'Teléfono', key: 'phone' }
  ];

  // ======================================
  // VALIDAR FORMULARIO
  // ======================================

  valid = computed(() => {

    const f = this.form();

    return (
      !!f.firstName.trim() &&
      !!f.lastName.trim() &&
      isValidEmail(f.email)
    );
  });

  // ======================================
  // INICIALES DEL USUARIO
  // ======================================

  initials = computed(() => {

    const p = this.profile();

    const iniciales =
      p.firstName.trim().charAt(0) +
      p.lastName.trim().charAt(0);

    if (iniciales.trim()) {
      return iniciales.toUpperCase();
    }

    const usuario = p.email.split('@')[0];

    return usuario.charAt(0).toUpperCase() || 'U';
  });

  // ======================================
  // NOMBRE VISIBLE
  // ======================================

  nombreVisible = computed(() => {

    const p = this.profile();

    const nombreCompleto =
      `${p.firstName} ${p.lastName}`.trim();

    if (nombreCompleto) {
      return nombreCompleto;
    }

    return p.email.split('@')[0] || 'Usuario';
  });

  // ======================================
  // DETALLES DEL PERFIL
  // ======================================

  details = computed(() => {

    const p = this.profile();

    return [
      {
        label: 'Nombre',
        value: p.firstName || 'Sin información'
      },
      {
        label: 'Apellido',
        value: p.lastName || 'Sin información'
      },
      {
        label: 'Correo electrónico',
        value: p.email || 'Sin información'
      },
      {
        label: 'Teléfono',
        value: p.phone || 'Sin información'
      },
      {
        label: 'Rol',
        value: this.role
      },
      {
        label: 'Fecha de registro',
        value: p.registered || 'Sin información'
      }
    ];
  });

  // ======================================
  // NORMALIZAR ROL
  // ======================================

  private normalizarRol(valor: string): string {

    const rol = valor
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    switch (rol) {

      case 'HUESPED':
      case 'ROLE_HUESPED':
        return 'Huésped';

      case 'ANFITRION':
      case 'ROLE_ANFITRION':
        return 'Anfitrión';

      case 'ADMIN':
      case 'ROLE_ADMIN':
      case 'ADMINISTRADOR':
      case 'ROLE_ADMINISTRADOR':
        return 'Administrador';

      default:
        return 'Sin rol asignado';
    }
  }

  // ======================================
  // EDITAR PERFIL
  // ======================================

  startEditing(): void {

    this.form.set({
      ...this.profile()
    });

    this.error.set('');
    this.mensaje.set('');
    this.editing.set(true);
  }

  // ======================================
  // ACTUALIZAR CAMPO
  // ======================================

  setField(
    key: EditableKey,
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    this.form.update(actual => ({
      ...actual,
      [key]: input.value
    }));
  }

  // ======================================
  // CANCELAR EDICIÓN
  // ======================================

  cancelEdit(): void {

    this.form.set({
      ...this.profile()
    });

    this.editing.set(false);
    this.error.set('');
    this.mensaje.set('');
  }

  // ======================================
  // GUARDAR CAMBIOS
  // ======================================

  save(): void {

    if (!this.valid()) {

      this.error.set(
        'Completa los campos obligatorios y utiliza un correo válido.'
      );

      return;
    }

    // IMPORTANTE:
    // Todavía no hay un endpoint de actualización
    // de perfil conectado a este componente.
    // No modificamos los datos autenticados
    // ni mostramos un éxito falso.

    this.error.set(
      'Para guardar los cambios de forma permanente ' +
      'debemos conectar la actualización del perfil ' +
      'con Spring Boot.'
    );
  }

  // ======================================
  // ACTIVAR / DESACTIVAR CUENTA
  // ======================================

  toggleAccount(): void {

    this.confirmAccount.set(false);

    // No se modifica el estado de la cuenta
    // sin confirmación del backend.

    this.error.set(
      'Esta acción requiere conectar el endpoint ' +
      'de activación o desactivación de cuenta.'
    );
  }

  // ======================================
  // VOLVER AL LOGIN
  // ======================================

  irAlLogin(): void {

    this.router.navigateByUrl('/login');
  }
}
