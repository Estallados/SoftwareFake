import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, map } from 'rxjs';
import { Alojamiento } from '../../../models/alojamientoDto';
import { HistorialEstadoAlojamiento } from '../../../models/historialEstadoAlojamientoDto';
import {
  ACCIONES_ALOJAMIENTO as ACCIONES, AccionAlojamiento, AdminService, accionesDe, claseBadge, conEstado, estadoDe,
  etiquetaEstado, valorDe,
} from '../../../services/admin.service';
import { AlojamientoService } from '../../../services/alojamiento.service';
@Component({
  selector: 'app-admin-listing-detail',
  standalone: true,
  templateUrl: './listing-detail.html',
  imports: [CurrencyPipe, DatePipe, DecimalPipe],
})
export class ListingDetail implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly admin = inject(AdminService);
  private readonly alojamientoService = inject(AlojamientoService);
  /** Parámetro :id de la ruta admin/alojamientos/:id */
  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly acciones = ACCIONES;
  readonly valorDe = valorDe;
  listing = signal<Alojamiento | null>(null);
  historial = signal<HistorialEstadoAlojamiento[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  estado = computed(() => estadoDe(this.listing()));
  etiqueta = computed(() => etiquetaEstado(this.estado()));
  badgeClass = computed(() => claseBadge(this.estado()));
  disponibles = computed(() => accionesDe(this.estado()));
  imagenes = computed(() => (this.listing()?.imagenes ?? []).slice(0, 3));
  info = computed(() => {
    const l = this.listing();
    if (!l) {
      return [];
    }
    return [
      { label: 'Tipo', value: l.tipo },
      { label: 'Ciudad', value: l.ciudad },
      { label: 'Ubicación', value: valorDe(l, 'direccion', 'ubicacion') },
      { label: 'Capacidad', value: `${l.capacidad} huéspedes` },
      { label: 'Habitaciones', value: l.habitaciones != null ? String(l.habitaciones) : '' },
      { label: 'Camas', value: l.camas != null ? String(l.camas) : '' },
      { label: 'Baños', value: l.banos != null ? String(l.banos) : '' },
    ].filter((fila) => !!fila.value);
  });
  // ---- Modal de moderación ----
  accion = signal<AccionAlojamiento | null>(null);
  enviando = signal(false);
  errorAccion = signal<string | null>(null);
  motivo = signal('');
  config = computed(() => {
    const a = this.accion();
    return a ? ACCIONES[a] : null;
  });
  puedeConfirmar = computed(() => {
    const c = this.config();
    return !!c && !this.enviando() && (!c.requiereMotivo || this.motivo().trim().length > 0);
  });
  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cancelarAccion();
  }
  ngOnInit(): void {
    this.cargar();
  }
  cargar(): void {
    if (!Number.isInteger(this.id) || this.id <= 0) {
      this.errorCarga.set(true);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.errorCarga.set(false);
    this.alojamientoService
      .obtener(this.id)
      .pipe(
        // Si el detalle público no sirve para este estado, se busca en el listado de admin.
        catchError(() =>
          this.admin.todosLosAlojamientos().pipe(
            map(({ alojamientos }) => {
              const a = alojamientos.find((x) => x.idAlojamiento === this.id);
              if (!a) {
                throw new Error('Alojamiento no encontrado');
              }
              return a;
            }),
          ),
        ),
      )
      .subscribe({
        next: (a) => {
          this.listing.set(a);
          this.loading.set(false);
          this.cargarHistorial();
        },
        error: (error) => {
          console.error('Error al cargar el alojamiento:', error);
          this.errorCarga.set(true);
          this.loading.set(false);
        },
      });
  }
  private cargarHistorial(): void {
    this.alojamientoService.historial(this.id).subscribe({
      next: (h) => this.historial.set(h ?? []),
      error: () => this.historial.set([]), // el historial es informativo: no bloquea la página
    });
  }
  back(): void {
    this.router.navigateByUrl('/admin/alojamientos');
  }
  pedirAccion(tipo: AccionAlojamiento): void {
    this.errorAccion.set(null);
    this.motivo.set('');
    this.accion.set(tipo);
  }
  cancelarAccion(): void {
    if (!this.enviando()) {
      this.accion.set(null);
    }
  }
  confirmarAccion(): void {
    const tipo = this.accion();
    const actual = this.listing();
    if (!tipo || !actual || !this.puedeConfirmar()) {
      return;
    }
    const motivo = this.motivo().trim();
    this.enviando.set(true);
    this.errorAccion.set(null);
    this.admin.ejecutarAccion(tipo, this.id, motivo).subscribe({
      next: (actualizado) => {
        this.listing.set(conEstado({ ...actual, ...actualizado }, ACCIONES[tipo].resultado));
        this.alojamientoService.limpiarCache(); // Inicio y Explorar deben ver el cambio
        this.cargarHistorial();
        this.enviando.set(false);
        this.accion.set(null);
      },
      error: (error) => {
        this.errorAccion.set(this.admin.mensajeDeError(error, 'No se pudo completar la acción.'));
        this.enviando.set(false);
      },
    });
  }
}
