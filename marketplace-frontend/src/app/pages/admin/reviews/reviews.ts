import { DatePipe } from '@angular/common';
import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { Resena } from '../../../models/resenaDto';
import { AdminService, valorDe } from '../../../services/admin.service';
@Component({
  selector: 'app-admin-reviews',
  standalone: true,
  templateUrl: './reviews.html',
  imports: [DatePipe],
})
export class Reviews implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly TAMANIO = 10;
  readonly headers = ['Huésped', 'Alojamiento', 'Reserva', 'Calificación', 'Fecha', 'Comentario', 'Estado', 'Acción'];
  readonly starList = [1, 2, 3, 4, 5];
  readonly filtros = [
    { valor: 'Todas', texto: 'Todas las reseñas' },
    { valor: 'Visible', texto: 'Visibles' },
    { valor: 'Oculta', texto: 'Ocultas' },
  ];
  reviews = signal<Resena[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  search = signal('');
  estadoFiltro = signal('Todas');
  pagina = signal(0);
  /** Id de la reseña que se está volviendo a mostrar. */
  mostrando = signal<number | null>(null);
  errorFila = signal<string | null>(null);
  filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const filtro = this.estadoFiltro();
    return this.reviews().filter(
      (r) =>
        `${this.huesped(r)} ${this.alojamiento(r)} ${this.comentario(r)}`.toLowerCase().includes(term) &&
        (filtro === 'Todas' || (filtro === 'Oculta') === this.oculta(r)),
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
  // ---- Modal: ocultar reseña ----
  aOcultar = signal<Resena | null>(null);
  motivo = signal('');
  enviando = signal(false);
  errorAccion = signal<string | null>(null);
  puedeConfirmar = computed(() => !this.enviando() && this.motivo().trim().length > 0);
  ngOnInit(): void {
    this.cargar();
  }
  cargar(): void {
    this.loading.set(true);
    this.errorCarga.set(false);
    this.admin.todasLasResenas().subscribe({
      next: ({ resenas }) => {
        this.reviews.set(resenas);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error al cargar las reseñas:', error);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }
  // ---- Lectura de campos (los DTO pueden nombrarlos distinto) ----
  idDe(r: Resena): number { return Number(valorDe(r, 'idResena', 'id')); }
  huesped(r: Resena): string { return valorDe(r, 'nombreHuesped', 'nombreAutor', 'autor', 'huesped'); }
  alojamiento(r: Resena): string { return valorDe(r, 'tituloAlojamiento', 'alojamiento', 'titulo'); }
  reserva(r: Resena): string { const id = valorDe(r, 'idReserva', 'reserva'); return id ? `#${id}` : ''; }
  fecha(r: Resena): string { return valorDe(r, 'fecha', 'fechaCreacion', 'fechaPublicacion'); }
  comentario(r: Resena): string { return valorDe(r, 'comentario', 'texto', 'contenido'); }
  oculta(r: Resena): boolean { return valorDe(r, 'estado').toUpperCase().startsWith('OCULT'); }
  // ---- Filtros y paginación ----
  buscar(valor: string): void { this.search.set(valor); this.pagina.set(0); }
  filtrarEstado(valor: string): void { this.estadoFiltro.set(valor); this.pagina.set(0); }
  irAPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas()) {
      this.pagina.set(pagina);
    }
  }
  // ---- Moderación ----
  pedirOcultar(r: Resena): void {
    this.errorAccion.set(null);
    this.motivo.set('');
    this.aOcultar.set(r);
  }
  cancelarOcultar(): void {
    if (!this.enviando()) {
      this.aOcultar.set(null);
    }
  }
  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cancelarOcultar();
  }
  confirmarOcultar(): void {
    const r = this.aOcultar();
    if (!r || !this.puedeConfirmar()) {
      return;
    }
    this.enviando.set(true);
    this.errorAccion.set(null);
    this.admin.ocultarResena(this.idDe(r), this.motivo().trim()).subscribe({
      next: (actualizada) => {
        this.reemplazar(r, actualizada);
        this.enviando.set(false);
        this.aOcultar.set(null);
      },
      error: (error) => {
        this.errorAccion.set(this.admin.mensajeDeError(error, 'No se pudo ocultar la reseña.'));
        this.enviando.set(false);
      },
    });
  }
  mostrar(r: Resena): void {
    if (this.mostrando() !== null) {
      return;
    }
    const id = this.idDe(r);
    this.mostrando.set(id);
    this.errorFila.set(null);
    this.admin.mostrarResena(id).subscribe({
      next: (actualizada) => {
        this.reemplazar(r, actualizada);
        this.mostrando.set(null);
      },
      error: (error) => {
        this.errorFila.set(this.admin.mensajeDeError(error, 'No se pudo mostrar la reseña.'));
        this.mostrando.set(null);
      },
    });
  }
  private reemplazar(original: Resena, actualizada: Resena): void {
    const id = this.idDe(original);
    this.reviews.update((lista) => lista.map((x) => (this.idDe(x) === id ? { ...x, ...actualizada } : x)));
  }
}
