import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

export interface Stay {
  name: string;
  image: string;
  tag: string;
  city: string;
  type: string;
  rating: number | string;
  guests: number;
  price: number | string;
  amenities?: string[];
}

@Component({
  selector: 'app-explore',
  standalone: true,
  templateUrl: './explore.html',
})
export class Explore {
  private router = inject(Router);

  // TODO: reemplazar por los datos del backend
  stays = signal<Stay[]>([]);
  favorites = signal<string[]>([]);
  filterType = signal('Todos');

  readonly types = ['Todos', 'Casa', 'Finca', 'Cabaña', 'Apartamento'];
  readonly services = ['WiFi', 'Piscina', 'Parqueadero', 'Cocina', 'Mascotas permitidas'];
  readonly sortOptions = ['Recomendados', 'Precio menor', 'Precio mayor', 'Mejor calificados'];

  readonly searchFields = [
    { icon: 'pin', label: '¿A dónde?', value: 'Ciudad o destino' },
    { icon: 'calendar', label: 'Entrada', value: '15 jul 2025' },
    { icon: 'calendar', label: 'Salida', value: '19 jul 2025' },
  ];

  visible = computed(() =>
    this.filterType() === 'Todos'
      ? this.stays()
      : this.stays().filter((s) => s.type === this.filterType()),
  );

  openStay(name: string) {
    this.router.navigate(['/alojamiento', name]);
  }

  isFavorite(name: string) {
    return this.favorites().includes(name);
  }

  toggleFavorite(event: Event, name: string) {
    event.stopPropagation();
    this.favorites.update((f) => (f.includes(name) ? f.filter((x) => x !== name) : [...f, name]));
  }

  favClass(name: string) {
    return (
      'absolute right-3 top-3 grid size-9 place-items-center rounded-full bg-white/90 transition ' +
      (this.isFavorite(name) ? 'text-red-500' : 'text-slate-800 hover:text-red-500')
    );
  }

  amenities(stay: Stay) {
    return (stay.amenities ?? ['WiFi', 'Cocina']).slice(0, 2).join(' · ');
  }
}
