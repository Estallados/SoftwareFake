import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { EventoAuditoria } from '../../../models/eventoAuditoriaDto';
import { AdminService, FiltrosAuditoria, valorDe } from '../../../services/admin.service';
@Component({
  selector: 'app-admin-audit',
  standalone: true,
  templateUrl: './audit.html',
})
export class Audit implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly TAMANIO = 20;
  readonly headers = ['Fecha y hora', 'Usuario', 'Rol', 'Acción', 'Entidad', 'Resultado', 'Detalle'];
  auditEvents = signal<EventoAuditoria[]>([]);
  loading = signal(true);
  errorCarga = signal(false);
  pagina = signal(0);
  totalPaginas = signal(1);
  totalElementos = signal(0);
  // Filtros: se aplican al pulsar "Filtrar" para no consultar en cada tecla
  accion = signal('');
  idActor = signal('');
  desde = signal('');
  hasta = signal('');
  /** Acciones distintas de la página actual, para sugerirlas en el filtro. */
  accionesVistas = computed(() => [...new Set(this.auditEvents().map((e) => this.accionDe(e)).filter(Boolean))].sort());
  // ---- Detalle de un evento ----
  eventoDetalle = signal<EventoAuditoria | null>(null);
  cargandoDetalle = signal(false);
  errorDetalle = signal(false);
  detalleAbierto = signal(false);
  camposDetalle = computed(() =>
    Object.entries((this.eventoDetalle() ?? {}) as unknown as Record<string, unknown>).map(([clave, valor]) => ({
      clave: this.humanizar(clave),
      valor: valor !== null && typeof valor === 'object' ? JSON.stringify(valor) : String(valor ?? '—'),
    })),
  );
  paginasVisibles = computed(() => {
    const total = this.totalPaginas();
    const inicio = Math.max(0, Math.min(this.pagina() - 2, total - 5));
    return Array.from({ length: Math.min(total, inicio + 5) - inicio }, (_, i) => inicio + i);
  });
  ngOnInit(): void {
    this.cargar();
  }
  cargar(): void {
    this.loading.set(true);
    this.errorCarga.set(false);
    const actor = Number(this.idActor());
    const filtros: FiltrosAuditoria = {
      accion: this.accion().trim() || undefined,
      idActor: this.idActor().trim() && Number.isInteger(actor) && actor > 0 ? actor : undefined,
      desde: this.desde() ? `${this.desde()}T00:00:00` : undefined,
      hasta: this.hasta() ? `${this.hasta()}T23:59:59` : undefined,
    };
    this.admin.auditoriaPagina(this.pagina(), this.TAMANIO, filtros).subscribe({
      next: (p) => {
        this.auditEvents.set(p.contenido);
        this.totalPaginas.set(Math.max(1, p.totalPaginas));
        this.totalElementos.set(p.totalElementos);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error al cargar la auditoría:', error);
        this.auditEvents.set([]);
        this.errorCarga.set(true);
        this.loading.set(false);
      },
    });
  }
  filtrar(): void {
    this.pagina.set(0);
    this.cargar();
  }
  limpiar(): void {
    this.accion.set('');
    this.idActor.set('');
    this.desde.set('');
    this.hasta.set('');
    this.filtrar();
  }
  irAPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas() && pagina !== this.pagina()) {
      this.pagina.set(pagina);
      this.cargar();
    }
  }
  verDetalle(e: EventoAuditoria): void {
    const id = Number(valorDe(e, 'idEvento', 'id'));
    this.detalleAbierto.set(true);
    this.errorDetalle.set(false);
    if (!Number.isInteger(id) || id <= 0) {
      this.eventoDetalle.set(e); // sin id no se puede pedir el detalle: se muestra lo que ya hay
      this.cargandoDetalle.set(false);
      return;
    }
    this.eventoDetalle.set(null);
    this.cargandoDetalle.set(true);
    this.admin.evento(id).subscribe({
      next: (ev) => {
        this.eventoDetalle.set(ev);
        this.cargandoDetalle.set(false);
      },
      error: (error) => {
        console.error('Error al cargar el evento de auditoría:', error);
        this.errorDetalle.set(true);
        this.cargandoDetalle.set(false);
      },
    });
  }
  @HostListener('document:keydown.escape')
  cerrarDetalle(): void {
    this.detalleAbierto.set(false);
  }
  private humanizar(clave: string): string {
    const texto = clave.replace(/([A-Z])/g, ' $1').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
  // ---- Lectura de campos (los DTO pueden nombrarlos distinto) ----
  fecha(e: EventoAuditoria): string { return valorDe(e, 'fecha', 'fechaHora', 'fechaEvento', 'fechaCreacion'); }
  usuario(e: EventoAuditoria): string { return valorDe(e, 'nombreActor', 'actor', 'nombreUsuario', 'correoActor', 'idActor'); }
  rol(e: EventoAuditoria): string { return valorDe(e, 'rolActor', 'rol'); }
  accionDe(e: EventoAuditoria): string { return valorDe(e, 'accion'); }
  detalle(e: EventoAuditoria): string { return valorDe(e, 'detalle', 'descripcion', 'motivo'); }
  resultado(e: EventoAuditoria): string { return valorDe(e, 'resultado', 'estado') || '—'; }
  entidad(e: EventoAuditoria): string {
    const tipo = valorDe(e, 'entidad', 'tipoEntidad');
    const id = valorDe(e, 'idEntidad', 'idObjeto');
    return id ? `${tipo} #${id}` : tipo;
  }
  badgeClass(e: EventoAuditoria): string {
    const r = this.resultado(e).toUpperCase();
    const falla = r.includes('ERR') || r.includes('FALL') || r.includes('RECHAZ') || r.includes('DENEG');
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + (falla ? 'bg-red-50 text-red-700' : 'bg-emerald-50 text-emerald-700');
  }
}
