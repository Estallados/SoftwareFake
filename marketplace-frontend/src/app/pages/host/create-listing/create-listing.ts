import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

export interface ListingDraft {
  name: string;
  city: string;
  description: string;
  location: string;
  type: string;
  capacity: string;
  rooms: string;
  baths: string;
  price: string;
  amenities: string[];
}

@Component({
  selector: 'app-host-create-listing',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './create-listing.html',
})
export class CreateListing {
  private router = inject(Router);

  readonly services = [
    'WiFi',
    'Cocina',
    'Piscina',
    'Parqueadero',
    'Aire acondicionado',
    'Mascotas',
    'TV',
  ];

  draft: ListingDraft = {
    name: '',
    city: '',
    description: '',
    location: '',
    type: '',
    capacity: '',
    rooms: '',
    baths: '',
    price: '',
    amenities: [],
  };

  toggleService(service: string) {
    const list = this.draft.amenities;
    this.draft.amenities = list.includes(service)
      ? list.filter((s) => s !== service)
      : [...list, service];
  }

  saveDraft() {
    // TODO: guardar el borrador en el backend
  }

  submit() {
    // TODO: enviar a revisión en el backend y avisar "Tu alojamiento fue enviado para revisión"
    this.router.navigateByUrl('/host/alojamientos');
  }
}
