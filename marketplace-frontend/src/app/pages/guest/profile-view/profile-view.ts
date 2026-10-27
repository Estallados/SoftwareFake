import { Component, computed, signal } from '@angular/core';
import {isValidEmail} from '../../../utils/validators';

export interface Profile {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  registered: string;
  active: boolean;
}

type EditableKey = 'firstName' | 'lastName' | 'email' | 'phone';

@Component({
  selector: 'app-profile-view',
  standalone: true,
  templateUrl: './profile-view.html',
})
export class ProfileView {
  readonly role = 'Huésped';

  // TODO: cargar el perfil desde el backend
  profile = signal<Profile>({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    registered: '',
    active: true,
  });

  editing = signal(false);
  confirmAccount = signal(false);
  form = signal<Profile>({ ...this.profile() });

  readonly editFields: { label: string; key: EditableKey }[] = [
    { label: 'Nombre', key: 'firstName' },
    { label: 'Apellido', key: 'lastName' },
    { label: 'Correo electrónico', key: 'email' },
    { label: 'Teléfono', key: 'phone' },
  ];

  valid = computed(() => {
    const f = this.form();
    return !!f.firstName.trim() && !!f.lastName.trim() && isValidEmail(f.email);
  });

  initials = computed(
    () => this.profile().firstName.charAt(0) + this.profile().lastName.charAt(0),
  );

  details = computed(() => {
    const p = this.profile();
    return [
      { label: 'Nombre', value: p.firstName },
      { label: 'Apellido', value: p.lastName },
      { label: 'Correo electrónico', value: p.email },
      { label: 'Teléfono', value: p.phone },
      { label: 'Rol', value: this.role },
      { label: 'Fecha de registro', value: p.registered },
    ];
  });

  startEditing() {
    this.form.set({ ...this.profile() });
    this.editing.set(true);
  }

  setField(key: EditableKey, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.form.update((f) => ({ ...f, [key]: value }));
  }

  cancelEdit() {
    this.form.set({ ...this.profile() });
    this.editing.set(false);
  }

  save() {
    if (!this.valid()) return;
    this.profile.set({ ...this.form() });
    this.editing.set(false);
    // TODO: guardar en el backend y avisar "Tu perfil se actualizó correctamente"
  }

  toggleAccount() {
    this.profile.update((p) => ({ ...p, active: !p.active }));
    this.confirmAccount.set(false);
    // TODO: guardar en el backend y avisar del cambio de estado
  }
}
