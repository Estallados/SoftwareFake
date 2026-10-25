import { Component, input, output } from '@angular/core';
import { photos } from '../../../services/mockData';

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
  selector: 'app-guest-home',
  standalone: true,
  templateUrl: './home.html',
})
export class Home {
  // Entradas (props de React)
  stays = input.required<Stay[]>();
  favorites = input<string[]>([]);

  // Salidas (callbacks de React)
  search = output<void>();
  open = output<string>();
  host = output<void>();
  favorite = output<string>();

  readonly photos = photos;

  // Campos de la barra de búsqueda
  readonly searchFields = [
    { icon: 'pin', label: '¿A dónde?', value: 'Ciudad o destino' },
    { icon: 'calendar', label: 'Entrada', value: '15 jul 2025' },
    { icon: 'calendar', label: 'Salida', value: '19 jul 2025' },
    { icon: 'users', label: 'Huéspedes', value: '2 huéspedes' },
  ];

  // Tipos de alojamiento
  readonly types = [
    { name: 'Apartamento', count: '128 lugares', image: photos.loft },
    { name: 'Casa', count: '94 lugares', image: photos.casa },
    { name: 'Finca', count: '57 lugares', image: photos.finca },
    { name: 'Cabaña', count: '72 lugares', image: photos.cabana },
    { name: 'Habitación', count: '110 lugares', image: photos.interior },
  ];

  // Footer
  readonly footerGroups = [
    { title: 'Explora', links: ['Alojamientos', 'Destinos', 'Tipos de propiedad'] },
    { title: 'Soporte', links: ['Centro de ayuda', 'Cancelaciones', 'Términos y condiciones'] },
    { title: 'Anfitriones', links: ['Publicar alojamiento', 'Administrar propiedades', 'Disponibilidad'] },
  ];

  isFavorite(name: string): boolean {
    return this.favorites().includes(name);
  }

  favClass(name: string): string {
    return (
      'absolute right-3 top-3 grid size-9 place-items-center rounded-full bg-white/90 transition ' +
      (this.isFavorite(name) ? 'text-red-500' : 'text-slate-800 hover:text-red-500')
    );
  }

  amenities(stay: Stay): string {
    return (stay.amenities ?? ['WiFi', 'Cocina']).slice(0, 2).join(' · ');
  }

  toggleFavorite(event: Event, name: string) {
    event.stopPropagation(); // evita abrir la tarjeta
    this.favorite.emit(name);
  }
}
