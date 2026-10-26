import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';

export interface Stay {
  name: string;
  city: string;
}

@Component({
  selector: 'app-map-view',
  standalone: true,
  templateUrl: './map-view.html',
})
export class MapView {
  private location = inject(Location);

  // TODO: cargar el alojamiento desde el backend
  stay: Stay = { name: '', city: '' };

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

  back() {
    this.location.back();
  }
}
