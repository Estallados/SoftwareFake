import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { photos } from '../../../services/mockData';

export interface Stay {
  name: string;
  image: string;
  tag: string;
  city: string;
  type: string;
  rating: number | string;
  reviews: number;
  guests: number;
  price: number | string;
}

export interface Review {
  id: number;
  accommodation: string;
  reservation: string;
  guest: string;
  rating: number;
  comment: string;
  published: string;
  status: 'Visible' | 'Oculta';
}

@Component({
  selector: 'app-detail',
  standalone: true,
  templateUrl: './detail.html',
})
export class Detail {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly photos = photos;

  /** Parámetro :id de la ruta alojamiento/:id */
  readonly id = this.route.snapshot.paramMap.get('id');

  // TODO: cargar el alojamiento y las reseñas desde el backend usando `id`
  stay: Stay = {
    name: '',
    image: '',
    tag: '',
    city: '',
    type: '',
    rating: '',
    reviews: 0,
    guests: 0,
    price: '',
  };
  reviews = signal<Review[]>([]);
  favorite = signal(false);

  visibleReviews = computed(() =>
    this.reviews()
      .filter((r) => r.accommodation === this.stay.name && r.status === 'Visible')
      .slice(0, 4),
  );

  readonly highlights = [
    { icon: 'home', title: 'Un espacio solo para ti', text: 'Tendrás toda la casa durante tu estancia.' },
    { icon: 'pin', title: 'Ubicación privilegiada', text: 'Cerca de naturaleza, restaurantes y puntos de interés.' },
    { icon: 'star', title: 'Experiencia destacada', text: 'Una de las propiedades mejor calificadas en Medellín.' },
  ];

  readonly amenities = [
    'WiFi de alta velocidad', 'Cocina equipada', 'Piscina privada',
    'Parqueadero', 'TV', 'Mascotas permitidas',
  ];

  readonly distribution = [
    { stars: 5, width: 82, count: 105 },
    { stars: 4, width: 13, count: 17 },
    { stars: 3, width: 4, count: 5 },
    { stars: 2, width: 1, count: 1 },
    { stars: 1, width: 0, count: 0 },
  ];

  // ---- Mapa ilustrativo ----
  readonly horizontalLines = [18, 38, 58, 78];
  readonly verticalLines = [15, 35, 55, 75];
  private readonly markerPositions: Record<string, [number, number]> = {
    'Casa Bosque Verde': [58, 42],
    'Finca El Mirador': [36, 58],
    'Cabaña Montaña Viva': [67, 31],
    'Loft Urbano LR': [48, 65],
  };
  get marker(): [number, number] {
    return this.markerPositions[this.stay.name] ?? [50, 50];
  }

  readonly starList = [1, 2, 3, 4, 5];

  // ---- Acciones ----
  toggleFavorite() {
    this.favorite.update((v) => !v);
  }

  reserve() {
    // TODO: abrir el modal de cotización / consultar disponibilidad
  }

  openMap() {
    this.router.navigateByUrl('/mapa');
  }

  contactHost() {
    this.router.navigateByUrl('/mensajes');
  }

  initials(name: string) {
    return name
      .split(' ')
      .map((w) => w[0])
      .join('');
  }
}
