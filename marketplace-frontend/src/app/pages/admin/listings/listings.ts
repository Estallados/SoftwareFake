import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Alojamiento } from '../../../models/alojamientoDto';
import {
  ACCIONES_ALOJAMIENTO as ACCIONES, AccionAlojamiento, AdminService, ESTADOS_ALOJAMIENTO, accionesDe, claseBadge,
  conEstado, estadoDe, etiquetaEstado,
} from '../../../services/admin.service';
import { AlojamientoService } from '../../../services/alojamiento.service';
@Component({
  selector: 'app-admin-listings',
  standalone: true,
  templateUrl: './listings.html',
  imports: [CurrencyPipe, DecimalPipe],
})
export class Listings implements OnInit {
  private readonly router = inject(Router);
  private readonly admin = inject(AdminService);
  private readonly alojamientoService = inject(AlojamientoService);
  private readonly TAMANIO = 10;
  readonly acciones = ACCIONES;
  readonly estados = ESTADOS_ALOJAMIENTO;
  readonly headers = [
    'Alojamiento', 'Anfitrión', 'Ciudad', 'Tipo', 'Precio / noche', 'Capacidad', 'Estado', 'Calificación', 'Acciones',
  ];
  listings = signal<Alojamiento[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  search = signal('');
  statusFilter = signal('Todos');
  typeFilter = signal('Todos');
  cityFilter = signal('Todas');
  pagina = signal(0);
  types = computed(() => this.unicos((l) => l.tipo));
  cities = computed(() => this.unicos((l) => l.ciudad));
  filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    return this.listings().filter(
      (l) =>
        `${l.titulo} ${l.nombreAnfitrion ?? ''} ${l.ciudad ?? ''}`.toLowerCase().includes(term) &&
        (this.statusFilter() === 'Todos' || estadoDe(l) === this.statusFilter()) &&
        (this.typeFilter() === 'Todos' || l.tipo === this.typeFilter()) &&
        (this.cityFilter() === 'Todas' || l.ciudad === this.cityFilter()),
    );
  });
  totalPaginas = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.TAMANIO)));
  paginaActual = computed(() => Math.min(this.pagina(), this.totalPaginas() - 1));
  visibles = computed(() => {
    const inicio = this.paginaActual() * this.TAMANIO;
    return this.filtered().slice(inicio, inicio + this.TAMANIO);
  });
  paginasVisibles = computed(() => {
    const total = this.totalPaginas();
    const inicio = Math.max(0, Math.min(this.paginaActual() - 2, total - 5));
    return Array.from({ length: Math.min(total, inicio + 5) - inicio }, (_, i) => inicio + i);
  });
  pendingCount = computed(() => this.contar('PENDIENTE_REVISION'));
  activeCount = computed(() => this.contar('ACTIVA'));
  blockedCount = computed(() => this.contar('BLOQUEADA'));
  // ---- Modal de moderación ----
  accion = signal<{ tipo: AccionAlojamiento; stay: Alojamiento } | null>(null);
  enviando = signal(false);
  errorAccion = signal<string | null>(null);
  motivo = signal('');
  config = computed(() => {
    const a = this.accion();
    return a ? ACCIONES[a.tipo] : null;
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
    this.loading.set(true);
    this.errorCarga.set(false);
    this.admin.todosLosAlojamientos().subscribe({
      next: ({ alojamientos }) => {
        this.listings.set(alojamientos);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error al cargar los alojamientos:', error);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }
  // ---- Filtros (cada cambio vuelve a la primera página) ----
  buscar(valor: string): void { this.search.set(valor); this.pagina.set(0); }
  filtrarEstado(valor: string): void { this.statusFilter.set(valor); this.pagina.set(0); }
  filtrarTipo(valor: string): void { this.typeFilter.set(valor); this.pagina.set(0); }
  filtrarCiudad(valor: string): void { this.cityFilter.set(valor); this.pagina.set(0); }
  irAPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas()) {
      this.pagina.set(pagina);
    }
  }
  open(id: number | undefined): void {
    if (id != null) {
      this.router.navigate(['/admin/alojamientos', id]);
    }
  }
  // ---- Presentación ----
  accionesDe(l: Alojamiento): AccionAlojamiento[] {
    return accionesDe(estadoDe(l));
  }
  etiqueta(l: Alojamiento): string {
    return etiquetaEstado(estadoDe(l));
  }
  badgeClass(l: Alojamiento): string {
    return claseBadge(estadoDe(l));
  }
  // ---- Moderación ----
  pedirAccion(tipo: AccionAlojamiento, stay: Alojamiento): void {
    this.errorAccion.set(null);
    this.motivo.set('');
    this.accion.set({ tipo, stay });
  }
  cancelarAccion(): void {
    if (!this.enviando()) {
      this.accion.set(null);
    }
  }
  confirmarAccion(): void {
    const a = this.accion();
    const id = a?.stay.idAlojamiento;
    if (!a || id == null || !this.puedeConfirmar()) {
      return;
    }
    const motivo = this.motivo().trim();
    this.enviando.set(true);
    this.errorAccion.set(null);
    this.admin.ejecutarAccion(a.tipo, id, motivo).subscribe({
      next: (actualizado) => {
        const nuevo = conEstado({ ...a.stay, ...actualizado }, ACCIONES[a.tipo].resultado);
        this.listings.update((lista) => lista.map((l) => (l.idAlojamiento === id ? nuevo : l)));
        this.alojamientoService.limpiarCache(); // Inicio y Explorar deben ver el cambio
        this.enviando.set(false);
        this.accion.set(null);
      },
      error: (error) => {
        this.errorAccion.set(this.admin.mensajeDeError(error, 'No se pudo completar la acción.'));
        this.enviando.set(false);
      },
    });
  }
  private contar(estado: string): number {
    return this.listings().filter((l) => estadoDe(l) === estado).length;
  }
  private unicos(valor: (l: Alojamiento) => string | undefined): string[] {
    return [...new Set(this.listings().map(valor).filter((v): v is string => !!v?.trim()))].sort((a, b) =>
      a.localeCompare(b, 'es'),
    );
  }
}
