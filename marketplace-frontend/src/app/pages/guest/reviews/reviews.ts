import { Component, signal } from '@angular/core';

export interface Review {
  id: number;
  accommodation: string;
  reservation: string;
  rating: number;
  comment: string;
  published: string;
}

@Component({
  selector: 'app-guest-reviews',
  standalone: true,
  templateUrl: './reviews.html',
})
export class Reviews {
  // TODO: reemplazar por las reseñas del usuario (backend)
  reviews = signal<Review[]>([]);

  readonly starList = [1, 2, 3, 4, 5];
}
