import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { photos } from '../../../services/mockData';
import { Alojamiento } from '../../../models/alojamientoDto';
import { AlojamientoService } from '../../../services/alojamiento.service';
import { FavoritoService } from '../../../services/favorito.service';
import {SessionService} from '../../../services/session.service';
interface TipoAlojamiento {
  name: string;
  count: number;
  image: string;
}
@Component({
  selector: 'app-guest-home',
  standalone: true,
  templateUrl: './home.html',
  imports: [CurrencyPipe, DecimalPipe],
})
export class Home implements OnInit {
  private router = inject(Router);
  protected readonly alojamientoService = inject(AlojamientoService);
  protected readonly favoritoService = inject(FavoritoService);
  protected readonly session = inject(SessionService);
  loading = signal(true);
  loadingTipos = signal(true);
  stays = signal<Alojamiento[]>([]);
  readonly photos = photos;
  // Las imágenes son fijas y no dependen del tipo de alojamiento.
  private readonly imagenesTipos: string[] = [
    photos.loft,
    photos.casa,
    photos.finca,
    photos.cabana,
    photos.interior,
  ];
  // Tipos obtenidos dinámicamente del backend.
  types: TipoAlojamiento[] = [];
  readonly searchFields = [
    { icon: 'pin', label: '¿A dónde?', value: 'Ciudad o destino' },
    { icon: 'calendar', label: 'Entrada', value: '15 jul 2025' },
    { icon: 'calendar', label: 'Salida', value: '19 jul 2025' },
    { icon: 'users', label: 'Huéspedes', value: '2 huéspedes' },
  ];
  ngOnInit(): void {
    this.favoritoService.cargarSiCorresponde();
    // Cargar alojamientos destacados.
    this.alojamientoService.disponibles(4).subscribe({
      next: (respuesta) => {
        this.stays.set(respuesta);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando alojamientos:', err);
        this.loading.set(false);
      },
    });
    // Cargar los cinco tipos con más alojamientos.
    this.cargarTipos();
  }
  /**
   * Obtiene las cantidades por tipo, ordena los resultados
   * de mayor a menor y muestra únicamente los cinco primeros.
   *
   * Las imágenes se asignan por posición, no por tipo.
   */
  private cargarTipos(): void {
    this.loadingTipos.set(true);
    this.alojamientoService.cantidadPorTipo().subscribe({
      next: (datos) => {
        this.types = Object.entries(datos)
          .map(([name, count]) => ({
            name,
            count,
            image: '',
          }))
          .sort(
            (a, b) =>
              b.count - a.count || a.name.localeCompare(b.name, 'es'),
          )
          .slice(0, 5)
          .map((tipo, index) => ({
            ...tipo,
            image: this.imagenesTipos[index],
          }));
        this.loadingTipos.set(false);
      },
      error: (error) => {
        console.error('Error al obtener las cantidades por tipo:', error);
        this.loadingTipos.set(false);
      },
    });
  }
  goSearch(): void {
    this.router.navigateByUrl('/explorar');
  }
  goHost(): void {
    this.router.navigateByUrl('/host/dashboard');
  }
  openStay(id: number | undefined): void {
    if (id == null) {
      console.error('El alojamiento no tiene un ID válido.');
      return;
    }
    this.router.navigate(['/alojamiento', id]);
  }
}
