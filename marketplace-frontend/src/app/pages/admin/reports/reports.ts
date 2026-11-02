import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AdminService, valorDe } from '../../../services/admin.service';
type Formato = 'pdf' | 'excel';
@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './reports.html',
})
export class Reports implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly TAMANIO = 20;
  /** valor = parámetro {tipo} del backend. */
  readonly reportTypes = [
    { valor: 'usuarios', texto: 'Usuarios' },
    { valor: 'alojamientos', texto: 'Publicaciones' },
    { valor: 'reservas', texto: 'Reservas' },
    { valor: 'ingresos', texto: 'Ingresos estimados' },
    { valor: 'demanda', texto: 'Demanda' },
  ];
  readonly historyHeaders = ['Reporte', 'Formato', 'Usuario solicitante', 'Fecha y hora'];
  report = signal('reservas');
  /** Formato que se está exportando (null si no hay exportación en curso). */
  exportando = signal<Formato | null>(null);
  mensaje = signal<{ ok: boolean; texto: string } | null>(null);
  history = signal<Record<string, unknown>[]>([]);
  loadingHistory = signal(true);
  errorHistory = signal(false);
  pagina = signal(0);
  totalPaginas = signal(1);
  totalElementos = signal(0);
  paginasVisibles = computed(() => {
    const total = this.totalPaginas();
    const inicio = Math.max(0, Math.min(this.pagina() - 2, total - 5));
    return Array.from({ length: Math.min(total, inicio + 5) - inicio }, (_, i) => inicio + i);
  });
  ngOnInit(): void {
    this.cargarHistorial();
  }
  cargarHistorial(): void {
    this.loadingHistory.set(true);
    this.errorHistory.set(false);
    this.admin.historialExportaciones(this.pagina(), this.TAMANIO).subscribe({
      next: (p) => {
        this.history.set(p.contenido);
        this.totalPaginas.set(Math.max(1, p.totalPaginas));
        this.totalElementos.set(p.totalElementos);
        this.loadingHistory.set(false);
      },
      error: (error) => {
        console.error('Error al cargar el historial de exportaciones:', error);
        this.history.set([]);
        this.errorHistory.set(true);
        this.loadingHistory.set(false);
      },
    });
  }
  irAPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas() && pagina !== this.pagina()) {
      this.pagina.set(pagina);
      this.cargarHistorial();
    }
  }
  exportReport(formato: Formato): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(formato);
    this.mensaje.set(null);
    this.admin.exportarReporte(this.report(), formato).subscribe({
      next: ({ blob, nombre }) => {
        if (!blob || blob.size === 0) {
          this.mensaje.set({ ok: false, texto: 'El servidor devolvió un archivo vacío.' });
          this.exportando.set(null);
          return;
        }
        this.descargar(blob, nombre);
        this.mensaje.set({ ok: true, texto: `Reporte exportado en ${formato === 'pdf' ? 'PDF' : 'Excel'} correctamente.` });
        this.exportando.set(null);
        this.pagina.set(0);
        this.cargarHistorial(); // la exportación quedó registrada
      },
      error: (error) => {
        this.mensaje.set({ ok: false, texto: this.admin.mensajeDeError(error, 'No se pudo exportar el reporte.') });
        this.exportando.set(null);
      },
    });
  }
  private descargar(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    document.body.appendChild(enlace); // Firefox exige que el enlace esté en el documento
    enlace.click();
    enlace.remove();
    // Se libera después: revocarlo de inmediato cancela la descarga en algunos navegadores
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  // ---- Lectura del historial (los DTO pueden nombrar los campos distinto) ----
  reporteDe(r: Record<string, unknown>): string {
    const valor = valorDe(r, 'tipo', 'reporte', 'nombreReporte', 'tipoReporte');
    return this.reportTypes.find((t) => t.valor === valor.toLowerCase())?.texto ?? valor;
  }
  formatoDe(r: Record<string, unknown>): string {
    const f = valorDe(r, 'formato').toLowerCase();
    return f.startsWith('pdf') ? 'PDF' : f.startsWith('exc') || f.startsWith('xls') ? 'Excel' : f.toUpperCase();
  }
  usuarioDe(r: Record<string, unknown>): string {
    return valorDe(r, 'nombreUsuario', 'usuario', 'nombreAdmin', 'nombre', 'correo', 'idUsuario');
  }
  fechaDe(r: Record<string, unknown>): string {
    return valorDe(r, 'fecha', 'fechaExportacion', 'fechaCreacion');
  }
}
