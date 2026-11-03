import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
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
  // ---- Barra de búsqueda: al buscar se abre Explorar con estos filtros ----
  readonly hoy = this.alojamientoService.fechaHoy();
  ciudad = signal('');
  entrada = signal('');
  salida = signal('');
  huespedes = signal('');
  errorBusqueda = signal<string | null>(null);
  /** Pregunta antes de cerrar la sesión de un huésped que quiere hacerse anfitrión. */
  confirmarAnfitrion = signal(false);
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
  buscar(): void {
    const campos = { ciudad: this.ciudad(), entrada: this.entrada(), salida: this.salida(), huespedes: this.huespedes() };
    const error = this.alojamientoService.validarBusqueda(campos);
    this.errorBusqueda.set(error);
    if (error) {
      return;
    }
    this.router.navigate(['/explorar'], { queryParams: this.alojamientoService.paramsBusqueda(campos) });
  }
  /** Botón "Publicar alojamiento": depende de la sesión. */
  publicar(): void {
    if (this.session.tieneRol('Anfitrión')) {
      this.router.navigateByUrl('/host/alojamientos');
    } else if (this.session.tieneRol('Huésped')) {
      this.confirmarAnfitrion.set(true);
    } else if (this.session.tieneRol('Administrador')) {
      this.router.navigateByUrl('/admin/dashboard');
    } else {
      this.irARegistroAnfitrion();
    }
  }
  @HostListener('document:keydown.escape')
  cancelarCambio(): void {
    this.confirmarAnfitrion.set(false);
  }
  /** Acepta cerrar la sesión de huésped y lleva al registro de anfitrión. */
  aceptarCambio(): void {
    this.confirmarAnfitrion.set(false);
    this.session.cerrarSesion();
    this.irARegistroAnfitrion();
  }
  private irARegistroAnfitrion(): void {
    this.router.navigate(['/registro'], { queryParams: { rol: 'anfitrion' } });
  }
  openStay(id: number | undefined): void {
    if (id == null) {
      console.error('El alojamiento no tiene un ID válido.');
      return;
    }
    this.router.navigate(['/alojamiento', id]);
  }
}
