import { Component, signal } from '@angular/core';
import {Resena} from '../../../models/resenaDto';

@Component({
  selector: 'app-guest-reviews',
  standalone: true,
  templateUrl: './reviews.html',
})
export class Reviews {
  // TODO: reemplazar por las reseñas del usuario (backend)
  reviews = signal<Resena[]>([]);

  readonly starList = [1, 2, 3, 4, 5];
}
