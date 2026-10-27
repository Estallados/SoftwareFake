import { Component, signal } from '@angular/core';

export interface AuditEvent {
  date: string;
  user: string;
  role: string;
  action: string;
  entity: string;
  result: string;
}

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  templateUrl: './audit.html',
})
export class Audit {
  // TODO: reemplazar por los eventos de auditoría del backend
  auditEvents = signal<AuditEvent[]>([]);

  readonly headers = ['Fecha y hora', 'Usuario', 'Rol', 'Acción', 'Entidad', 'Resultado'];
}
