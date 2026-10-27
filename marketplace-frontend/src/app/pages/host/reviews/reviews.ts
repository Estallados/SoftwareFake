import { Component, computed, signal } from '@angular/core';

export interface HostReview {
  id: number;
  accommodation: string;
  guest: string;
  published: string;
  rating: number;
  comment: string;
  status: 'Visible' | 'Oculta';
}

@Component({
  selector: 'app-host-reviews',
  standalone: true,
  templateUrl: './reviews.html',
})
export class Reviews {
  // TODO: reemplazar por las reseñas de los alojamientos del anfitrión (backend)
  reviews = signal<HostReview[]>([]);
  filter = signal('Todos');

  readonly starList = [1, 2, 3, 4, 5];

  /** Solo reseñas visibles */
  available = computed(() => this.reviews().filter((r) => r.status === 'Visible'));

  /** Alojamientos que aparecen en el filtro */
  accommodations = computed(() => Array.from(new Set(this.available().map((r) => r.accommodation))));

  filtered = computed(() =>
    this.filter() === 'Todos'
      ? this.available()
      : this.available().filter((r) => r.accommodation === this.filter()),
  );

  average = computed(() => {
    const list = this.available();
    if (!list.length) return '0,0';
    const avg = list.reduce((sum, r) => sum + r.rating, 0) / list.length;
    return avg.toFixed(1).replace('.', ',');
  });
}
