import { Component, signal } from '@angular/core';

export interface AdminReview {
  id: number;
  guest: string;
  accommodation: string;
  reservation: string;
  rating: number;
  published: string;
  comment: string;
  status: 'Visible' | 'Oculta';
}

@Component({
  selector: 'app-admin-reviews',
  standalone: true,
  templateUrl: './reviews.html',
})
export class Reviews {
  // TODO: reemplazar por las reseñas del backend
  reviews = signal<AdminReview[]>([]);
  reviewToHide = signal<number | null>(null);

  readonly headers = [
    'Huésped',
    'Alojamiento',
    'Reserva',
    'Calificación',
    'Fecha',
    'Comentario',
    'Estado',
    'Acción',
  ];
  readonly starList = [1, 2, 3, 4, 5];

  show(id: number) {
    this.reviews.update((list) =>
      list.map((r) => (r.id === id ? { ...r, status: 'Visible' as const } : r)),
    );
    // TODO: guardar en el backend y avisar "La reseña vuelve a estar visible."
  }

  confirmHide() {
    const id = this.reviewToHide();
    if (id === null) return;
    this.reviews.update((list) =>
      list.map((r) => (r.id === id ? { ...r, status: 'Oculta' as const } : r)),
    );
    this.reviewToHide.set(null);
    // TODO: guardar en el backend, registrar auditoría "Ocultó reseña" y avisar al usuario
  }
}
