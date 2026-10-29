import {Component, inject, input, OnInit, signal} from '@angular/core';
import { Router } from '@angular/router';
import { photos} from '../../../services/mockData';
import {Alojamiento} from '../../../models/alojamientoDto';
import {AlojamientoService} from '../../../services/alojamiento.service';

@Component({
  selector: 'app-guest-home',
  standalone: true,
  templateUrl: './home.html',
})
export class Home implements OnInit{
  private router = inject(Router);
  private alojamientoService = inject(AlojamientoService);
  loading = signal(true);


  stays = signal<Alojamiento[]>([]);


  favorites = signal<string[]>([]);

  ngOnInit() {
    this.alojamientoService.disponibles(4).subscribe({
      next: (respuesta) => {
        this.stays.set(respuesta);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando alojamientos:', err);
        this.loading.set(false);
      }
    });
  }

  readonly photos = photos;

  readonly searchFields = [
    { icon: 'pin', label: '¿A dónde?', value: 'Ciudad o destino' },
    { icon: 'calendar', label: 'Entrada', value: '15 jul 2025' },
    { icon: 'calendar', label: 'Salida', value: '19 jul 2025' },
    { icon: 'users', label: 'Huéspedes', value: '2 huéspedes' },
  ];

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

  goSearch() {
    this.router.navigateByUrl('/explorar');
  }

  goHost() {
    this.router.navigateByUrl('/host/dashboard');
  }

  openStay(id: number) {
    this.router.navigate(['/alojamiento', id]);
  }

  isFavorite(name: string): boolean {
    return this.favorites().includes(name);
  }

  toggleFavorite(event: Event, name: string) {
    event.stopPropagation();
    this.favorites.update((f) =>
      f.includes(name) ? f.filter((x) => x !== name) : [...f, name],
    );
  }

  favClass(name: string): string {
    return (
      'absolute right-3 top-3 grid size-9 place-items-center rounded-full bg-white/90 transition ' +
      (this.isFavorite(name) ? 'text-red-500' : 'text-slate-800 hover:text-red-500')
    );
  }

  amenities(stay: Alojamiento): string {
    return (stay.amenidades ?? ['WiFi', 'Cocina']).slice(0, 2).join(' · ');
  }
}
