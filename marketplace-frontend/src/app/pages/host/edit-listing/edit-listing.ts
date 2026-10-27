import { NgTemplateOutlet } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { photos } from '../../../services/mockData';

export type ListingStatus = 'Activo' | 'Pausado' | 'Bloqueado' | 'Pendiente' | 'Rechazado';
export type ApprovalStatus = 'Aprobado' | 'Pendiente de aprobación' | 'Rechazado';

export interface EditableListing {
  id: string;
  name: string;
  type: string;
  city: string;
  location: string;
  description: string;
  capacity: number;
  rooms: number;
  baths: number;
  price: string;
  additionalCosts: string;
  amenities: string[];
  rules: string;
  petConditions: string;
  smokingRestrictions: string;
  partyRestrictions: string;
  checkin: string;
  checkout: string;
  image: string;
  photos: string[];
  status: ListingStatus;
  approvalStatus?: ApprovalStatus;
  rejectionReason?: string;
  submitted?: string;
  updated: string;
}

interface FieldConfig {
  key: string;
  label: string;
  area?: boolean;
  type?: 'text' | 'number' | 'time';
}

@Component({
  selector: 'app-host-edit-listing',
  standalone: true,
  imports: [FormsModule, NgTemplateOutlet],
  templateUrl: './edit-listing.html',
})
export class EditListing {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /** Parámetro :id de la ruta host/alojamientos/:id/editar */
  readonly id = this.route.snapshot.paramMap.get('id');

  readonly services = [
    'WiFi', 'Cocina', 'Piscina', 'Parqueadero', 'Aire acondicionado', 'Televisión', 'Mascotas permitidas',
  ];

  // ---- Configuración de los campos (una sola plantilla los dibuja) ----
  readonly basicFields: FieldConfig[] = [
    { key: 'name', label: 'Nombre del alojamiento' },
    { key: 'type', label: 'Tipo de alojamiento' },
    { key: 'city', label: 'Ciudad' },
    { key: 'location', label: 'Ubicación textual' },
  ];
  readonly capacityFields: FieldConfig[] = [
    { key: 'capacity', label: 'Huéspedes', type: 'number' },
    { key: 'rooms', label: 'Habitaciones', type: 'number' },
    { key: 'baths', label: 'Baños', type: 'number' },
  ];
  readonly priceFields: FieldConfig[] = [
    { key: 'price', label: 'Precio por noche' },
    { key: 'additionalCosts', label: 'Costos adicionales' },
  ];
  readonly ruleFields: FieldConfig[] = [
    { key: 'rules', label: 'Reglas generales', area: true },
    { key: 'petConditions', label: 'Condiciones para mascotas', area: true },
    { key: 'smokingRestrictions', label: 'Restricciones para fumadores', area: true },
    { key: 'partyRestrictions', label: 'Restricciones de fiestas', area: true },
    { key: 'checkin', label: 'Hora de entrada', type: 'time' },
    { key: 'checkout', label: 'Hora de salida', type: 'time' },
  ];
  readonly descriptionField: FieldConfig = { key: 'description', label: 'Descripción', area: true };

  // TODO: cargar el alojamiento desde el backend usando `id` y llamar a load()
  form: EditableListing = this.empty();
  private snapshot = JSON.stringify(this.form);
  private original: EditableListing = this.form;

  errors: Record<string, string> = {};
  confirmExit = false;
  photoToRemove: string | null = null;
  private pendingExit: (() => void) | null = null;

  private empty(): EditableListing {
    return {
      id: '', name: '', type: '', city: '', location: '', description: '',
      capacity: 0, rooms: 0, baths: 0, price: '', additionalCosts: '',
      amenities: [], rules: '', petConditions: '', smokingRestrictions: '',
      partyRestrictions: '', checkin: '', checkout: '', image: '', photos: [],
      status: 'Activo', updated: '',
    };
  }

  /** Llama a este método cuando tengas el alojamiento del backend */
  load(listing: EditableListing) {
    this.original = listing;
    this.form = structuredClone(listing);
    this.snapshot = JSON.stringify(this.form);
  }

  get dirty() {
    return JSON.stringify(this.form) !== this.snapshot;
  }

  // ---- Acceso a campos por clave ----
  val(key: string) {
    return (this.form as unknown as Record<string, unknown>)[key] as string | number;
  }

  setVal(key: string, value: string | number) {
    (this.form as unknown as Record<string, unknown>)[key] = value;
  }

  // ---- Servicios ----
  hasService(s: string) {
    return this.form.amenities.includes(s);
  }

  toggleService(s: string) {
    this.form.amenities = this.hasService(s)
      ? this.form.amenities.filter((x) => x !== s)
      : [...this.form.amenities, s];
  }

  // ---- Fotografías ----
  setMain(photo: string) {
    this.form.image = photo;
  }

  addPhoto() {
    const candidates = [photos.casa, photos.finca, photos.cabana, photos.loft, photos.colonial, photos.interior];
    const next = candidates.find((p) => !this.form.photos.includes(p));
    if (next) this.form.photos = [...this.form.photos, next];
    // TODO: subir la imagen al backend y avisar "Imagen agregada" / "No hay más imágenes"
  }

  confirmRemovePhoto() {
    const target = this.photoToRemove;
    if (!target) return;
    const next = this.form.photos.filter((p) => p !== target);
    if (!next.length) {
      // TODO: avisar "El alojamiento debe conservar al menos una fotografía."
      this.photoToRemove = null;
      return;
    }
    this.form.photos = next;
    if (this.form.image === target) this.form.image = next[0];
    this.photoToRemove = null;
  }

  // ---- Validación y guardado ----
  save() {
    const f = this.form;
    const e: Record<string, string> = {};
    if (!f.name.trim()) e['name'] = 'El nombre es obligatorio.';
    if (!f.description.trim()) e['description'] = 'La descripción es obligatoria.';
    if (!f.city.trim()) e['city'] = 'La ciudad es obligatoria.';
    if (!f.location.trim()) e['location'] = 'La ubicación es obligatoria.';
    if (!f.price.trim()) e['price'] = 'El precio es obligatorio.';
    if (f.capacity < 1) e['capacity'] = 'La capacidad debe ser mayor que cero.';
    if (f.rooms < 1) e['rooms'] = 'Indica al menos una habitación.';
    if (f.baths < 1) e['baths'] = 'Indica al menos un baño.';
    this.errors = e;
    if (Object.keys(e).length) return;

    // Si estaba rechazado, se reenvía a revisión
    const resubmit = this.original.approvalStatus === 'Rechazado';
    if (resubmit) {
      this.form = {
        ...this.form,
        status: 'Pendiente',
        approvalStatus: 'Pendiente de aprobación',
        rejectionReason: undefined,
      };
    }
    // TODO: guardar en el backend y avisar:
    //   resubmit ? 'Tu alojamiento fue enviado nuevamente para revisión' : 'Los cambios se guardaron correctamente'
    this.snapshot = JSON.stringify(this.form);
    this.goDetails(true);
  }

  // ---- Salida con confirmación de cambios sin guardar ----
  leave(action: () => void) {
    if (this.dirty) {
      this.pendingExit = action;
      this.confirmExit = true;
    } else {
      action();
    }
  }

  exitWithoutSaving() {
    const action = this.pendingExit;
    this.confirmExit = false;
    this.pendingExit = null;
    action?.();
  }

  goDetails(force = false) {
    const go = () => this.router.navigate(['/host/alojamientos', this.id]);
    if (force) go();
    else this.leave(go);
  }

  cancel() {
    this.leave(() => this.router.navigate(['/host/alojamientos', this.id]));
  }

  inputClass(key: string) {
    return (
      'mt-2 w-full rounded-xl border p-3 text-sm outline-none focus:ring-2 ' +
      (this.errors[key]
        ? 'border-red-300 focus:ring-red-100'
        : 'border-slate-200 focus:border-emerald-500 focus:ring-emerald-100')
    );
  }

  serviceClass(s: string) {
    return (
      'cursor-pointer rounded-xl border px-4 py-3 text-sm font-semibold ' +
      (this.hasService(s) ? 'border-emerald-300 bg-emerald-50 text-emerald-800' : 'border-slate-200')
    );
  }

  statusBadgeClass() {
    const s = this.original.status;
    const tone =
      s === 'Activo'
        ? 'bg-emerald-50 text-emerald-700'
        : s === 'Bloqueado'
          ? 'bg-red-50 text-red-700'
          : s === 'Pausado'
            ? 'bg-slate-100 text-slate-600'
            : 'bg-amber-50 text-amber-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }
}
