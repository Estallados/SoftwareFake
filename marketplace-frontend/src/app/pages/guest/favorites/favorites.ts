import { Component, inject, signal } from '@angular/core';
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
  selector: 'app-favorites',
  standalone: true,
  templateUrl: './favorites.html',
})
export class Favorites {
  private router = inject(Router);

  // TODO: reemplazar por los favoritos del usuario (backend)
  favoriteStays = signal<Stay[]>([]);

  openStay(name: string) {
    this.router.navigate(['/alojamiento', name]);
  }

  remove(event: Event, name: string) {
    event.stopPropagation();
    this.favoriteStays.update((list) => list.filter((s) => s.name !== name));
  }

  explore() {
    this.router.navigateByUrl('/explorar');
  }

  amenities(stay: Stay) {
    return (stay.amenities ?? ['WiFi', 'Cocina']).slice(0, 2).join(' · ');
  }
}
