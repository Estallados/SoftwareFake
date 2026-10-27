import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface PreviewTable {
  headers: string[];
  rows: string[][];
}

export interface ExportRecord {
  id: number;
  report: string;
  format: 'PDF' | 'Excel';
  date: string;
  filters: string;
}

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './reports.html',
})
export class Reports {
  readonly reportTypes = ['Usuarios', 'Publicaciones', 'Reservas', 'Ingresos estimados', 'Demanda'];
  readonly statuses = ['Todos', 'Activo', 'Confirmada', 'Pendiente', 'Finalizada', 'Cancelada'];
  readonly cities = ['Todas', 'Medellín', 'Bogotá', 'Cartagena', 'Cali', 'Santa Marta'];
  readonly propertyTypes = ['Todos', 'Casa', 'Apartamento', 'Finca', 'Cabaña'];

  // Filtros (two-way con [(ngModel)])
  report = signal('Reservas');
  from = signal('2026-01-01');
  to = signal('2026-10-09');
  status = signal('Todos');
  city = signal('Todas');
  propertyType = signal('Todos');
  generated = signal(false);

  // TODO: reemplazar las filas por los resultados del backend (las columnas ya están definidas)
  previews = signal<Record<string, PreviewTable>>({
    Usuarios: { headers: ['Usuario', 'Rol', 'Ciudad', 'Registro', 'Estado'], rows: [] },
    Publicaciones: { headers: ['Publicación', 'Ciudad', 'Tipo', 'Anfitrión', 'Estado'], rows: [] },
    Reservas: {
      headers: ['Reserva', 'Huésped', 'Alojamiento', 'Ciudad', 'Total', 'Estado'],
      rows: [],
    },
    'Ingresos estimados': {
      headers: ['Periodo', 'Reservas', 'Ingresos brutos', 'Comisión estimada'],
      rows: [],
    },
    Demanda: { headers: ['Posición', 'Ciudad', 'Reservas', 'Noches', 'Participación'], rows: [] },
  });
  history = signal<ExportRecord[]>([]);

  readonly historyHeaders = [
    'Reporte',
    'Formato',
    'Usuario solicitante',
    'Fecha y hora',
    'Filtros utilizados',
  ];

  preview = computed(() => this.previews()[this.report()]);

  statusDisabled = computed(
    () => this.report() === 'Demanda' || this.report() === 'Ingresos estimados',
  );
  propertyDisabled = computed(() => this.report() === 'Usuarios');

  filterSummary = computed(
    () =>
      `${this.from() || 'Sin fecha inicial'} a ${this.to() || 'Sin fecha final'} · Estado: ${this.status()} · Ciudad: ${this.city()} · Tipo: ${this.propertyType()}`,
  );

  generate() {
    this.generated.set(true);
    // TODO: pedir el reporte al backend y avisar "Reporte generado correctamente."
  }

  exportReport(format: 'PDF' | 'Excel') {
    const record: ExportRecord = {
      id: Date.now(),
      report: this.report(),
      format,
      date: new Date().toLocaleString('es-CO'),
      filters: this.filterSummary(),
    };
    this.history.update((h) => [record, ...h]);
    // TODO: descargar el archivo desde el backend y avisar "Reporte exportado en {format} correctamente."
  }

  /** Estilo de badge para las columnas de estado de la vista previa */
  badgeClass(value: string) {
    const tone =
      value === 'Pendiente'
        ? 'bg-amber-50 text-amber-700'
        : value === 'Finalizada'
          ? 'bg-slate-100 text-slate-600'
          : value === 'Cancelada'
            ? 'bg-red-50 text-red-700'
            : 'bg-emerald-50 text-emerald-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  }
}
