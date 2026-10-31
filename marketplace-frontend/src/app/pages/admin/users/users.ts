import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Usuario } from '../../../models/usuarioDto';
import { AdminService } from '../../../services/admin.service';

type TipoAccion = 'restringir' | 'activar';

const ACCIONES = {
  restringir: {
    titulo: 'Restringir usuario',
    texto: '¿Estás seguro de que deseas restringir esta cuenta?',
    etiqueta: 'Motivo de la restricción',
    confirmar: 'Confirmar restricción',
    necesitaMotivo: true,
  },
  activar: {
    titulo: 'Activar usuario',
    texto: '¿Deseas activar nuevamente esta cuenta?',
    etiqueta: '',
    confirmar: 'Confirmar activación',
    necesitaMotivo: false,
  },
};

@Component({
  selector: 'app-admin-users',
  standalone: true,
  templateUrl: './users.html',
  imports: [DatePipe],
})
export class Users implements OnInit {
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly adminService = inject(AdminService);

  // ==================== DATOS ====================
  readonly encabezados = [
    'Nombre',
    'Correo electrónico',
    'Rol',
    'Estado',
    'Fecha de registro',
    'Acciones',
  ];
  usuarios = signal<Usuario[]>([]);
  loading = signal(true);
  errorCarga = signal(false);

  // ==================== FILTROS ====================
  busqueda = signal('');
  filtroRol = signal('Todos');
  filtroEstado = signal('Todos');

  readonly visibles = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    const rol = this.filtroRol();
    const estado = this.filtroEstado();

    return this.usuarios().filter(
      (u) =>
        `${u.nombre ?? ''} ${u.correo ?? ''}`.toLowerCase().includes(termino) &&
        (rol === 'Todos' || this.tipoRol(u.rol) === rol) &&
        (estado === 'Todos' || this.tipoEstado(u.estado) === estado),
    );
  });

  onBusquedaChange(event: Event): void {
    this.busqueda.set((event.target as HTMLInputElement).value);
  }

  onRolChange(event: Event): void {
    this.filtroRol.set((event.target as HTMLSelectElement).value);
  }

  onEstadoChange(event: Event): void {
    this.filtroEstado.set((event.target as HTMLSelectElement).value);
  }

  limpiarFiltros(): void {
    this.busqueda.set('');
    this.filtroRol.set('Todos');
    this.filtroEstado.set('Todos');
  }

  // ==================== INICIALIZACIÓN ====================
  ngOnInit(): void {
    this.cargarUsuarios();
  }

  reintentar(): void {
    this.cargarUsuarios();
  }

  /** Si `silencioso` es true no muestra el esqueleto de carga (se usa al refrescar tras una acción). */
  private cargarUsuarios(silencioso = false): void {
    if (!silencioso) {
      this.loading.set(true);
    }
    this.errorCarga.set(false);

    this.adminService.usuarios().subscribe({
      next: (respuesta: Usuario[]) => {
        console.log('Usuarios recibidos:', respuesta);
        this.usuarios.set(respuesta ?? []);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar usuarios:', error);
        this.usuarios.set([]);
        this.errorCarga.set(true);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
    });
  }

  // ==================== ROL Y ESTADO ====================
  /** Normaliza sin depender de cómo escribe el backend el enum (Huésped, HUESPED, ...). */
  private normalizar(valor: unknown): string {
    return String(valor ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase();
  }

  private tipoRol(rol: unknown): 'ADMIN' | 'ANFITRION' | 'HUESPED' | '' {
    const r = this.normalizar(rol);
    if (r.includes('ADMIN')) return 'ADMIN';
    if (r.includes('ANFITRION')) return 'ANFITRION';
    if (r.includes('HUESPED')) return 'HUESPED';
    return '';
  }

  private tipoEstado(estado: unknown): 'ACTIVO' | 'RESTRINGIDO' | 'INACTIVO' | '' {
    const e = this.normalizar(estado);
    if (e.includes('RESTRING')) return 'RESTRINGIDO';
    if (e.includes('INACT')) return 'INACTIVO';
    if (e.includes('ACTIV')) return 'ACTIVO';
    return '';
  }

  esActivo(usuario: Usuario): boolean {
    return this.tipoEstado(usuario.estado) === 'ACTIVO';
  }

  claseEstado(usuario: Usuario): string {
    const estado = this.tipoEstado(usuario.estado);
    const tono =
      estado === 'ACTIVO'
        ? 'bg-emerald-50 text-emerald-700'
        : estado === 'RESTRINGIDO'
          ? 'bg-red-50 text-red-700'
          : 'bg-slate-100 text-slate-600';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tono;
  }

  // ==================== NAVEGACIÓN ====================
  abrirUsuario(id: number | undefined): void {
    if (id == null) {
      console.error('El usuario no tiene un ID válido.');
      return;
    }
    this.router.navigate(['/admin/usuarios', id]);
  }

  // ==================== MODAL DE CONFIRMACIÓN ====================
  accion = signal<{ tipo: TipoAccion; id: number } | null>(null);
  motivo = signal('');
  guardando = signal(false);
  errorAccion = signal('');

  readonly modal = computed(() => {
    const a = this.accion();
    return a ? ACCIONES[a.tipo] : null;
  });

  pedirAccion(usuario: Usuario): void {
    this.motivo.set('');
    this.errorAccion.set('');
    this.accion.set({
      tipo: this.esActivo(usuario) ? 'restringir' : 'activar',
      id: usuario.idUsuario,
    });
  }

  onMotivoChange(event: Event): void {
    this.motivo.set((event.target as HTMLTextAreaElement).value);
  }

  cerrarAccion(): void {
    if (this.guardando()) {
      return;
    }
    this.accion.set(null);
  }

  confirmarAccion(): void {
    const a = this.accion();
    if (!a || this.guardando()) {
      return;
    }

    const motivo = this.motivo().trim();
    const peticion =
      a.tipo === 'restringir'
        ? this.adminService.restringir(a.id, motivo)
        : this.adminService.liberar(a.id, motivo || undefined);

    this.guardando.set(true);
    this.errorAccion.set('');

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.accion.set(null);
        // Recarga la lista para mostrar el estado real guardado por el backend.
        this.cargarUsuarios(true);
      },
      error: (error) => {
        console.error('Error al actualizar el usuario:', error);
        this.errorAccion.set(error?.error?.mensaje ?? 'No se pudo completar la acción.');
        this.guardando.set(false);
        this.cdr.detectChanges();
      },
    });
  }
}
