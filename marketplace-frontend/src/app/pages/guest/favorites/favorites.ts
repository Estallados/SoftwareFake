import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Alojamiento } from '../../../models/alojamientoDto';
import { AlojamientoService } from '../../../services/alojamiento.service';
import { FavoritoService } from '../../../services/favorito.service';
import { SessionService } from '../../../services/session.service';

@Component({
  selector: 'app-favorites',
  standalone: true,
  templateUrl: './favorites.html',
  imports: [CurrencyPipe, DecimalPipe],
})
export class Favorites implements OnInit {
  private readonly router = inject(Router);
  private readonly session = inject(SessionService);
  protected readonly alojamientoService = inject(AlojamientoService);
  protected readonly favoritoService = inject(FavoritoService);

  private readonly stays = signal<Alojamiento[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  requiereSesion = signal(false);

  /** Solo se muestran los que siguen marcados: al quitar uno desaparece al instante. */
  readonly favoriteStays = computed(() =>
    this.stays().filter((s) => this.favoritoService.esFavorito(s.idAlojamiento)),
  );

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.errorCarga.set(false);
    if (!this.session.autenticado()) {
      this.requiereSesion.set(true);
      this.loading.set(false);
      return;
    }
    this.requiereSesion.set(false);
    this.loading.set(true);
    this.favoritoService.listar().subscribe({
      next: (lista) => {
        this.stays.set(lista);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error al cargar favoritos:', error);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }

  openStay(id: number | undefined): void {
    if (id == null) {
      console.error('El alojamiento no tiene un ID válido.');
      return;
    }
    this.router.navigate(['/alojamiento', id]);
  }

  explore(): void {
    this.router.navigateByUrl('/explorar');
  }
}
