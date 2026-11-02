import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Resena } from '../../../models/resenaDto';
import {ResenaService} from '../../../services/reesna.service';

@Component({
  selector: 'app-guest-reviews',
  standalone: true,
  templateUrl: './reviews.html',
  imports: [DatePipe],
})
export class Reviews implements OnInit {
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly resenaService = inject(ResenaService);

  // ==================== DATOS ====================
  reviews = signal<Resena[]>([]);
  loading = signal(true);
  errorCarga = signal(false);

  readonly starList = [1, 2, 3, 4, 5];

  // ==================== INICIALIZACIÓN ====================
  ngOnInit(): void {
    this.cargarResenas();
  }

  reintentar(): void {
    this.cargarResenas();
  }

  private cargarResenas(): void {
    this.loading.set(true);
    this.errorCarga.set(false);

    this.resenaService.mias().subscribe({
      next: (respuesta: Resena[]) => {
        console.log('Reseñas recibidas:', respuesta);
        this.reviews.set(respuesta ?? []);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar las reseñas:', error);
        this.reviews.set([]);
        this.errorCarga.set(true);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
    });
  }

  // ==================== PRESENTACIÓN ====================
  /** Las reseñas ocultas por un administrador siguen siendo visibles para su autor, con una marca. */
  estaOculta(resena: Resena): boolean {
    return resena.estado === 'OCULTA';
  }

  tituloAlojamiento(resena: Resena): string {
    return resena.tituloAlojamiento ?? '';
  }

  irAReservas(): void {
    this.router.navigateByUrl('/reservas');
  }
}
