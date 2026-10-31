import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Alojamiento } from '../../../models/alojamientoDto';
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
  selector: 'app-admin-user-detail',
  standalone: true,
  templateUrl: './user-detail.html',
})
export class UserDetail implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly adminService = inject(AdminService);

  // ==================== Usuario ====================
  usuario: Usuario | null = null;
  loading = true;
  errorCarga = false;
  mensajeError = '';

  // Solo para anfitriones
  alojamientos: Alojamiento[] = [];
  // Solo para huéspedes
  totalReservas = 0;
  estanciasFinalizadas = 0;

  private idUsuario = 0;

  // ==================== Rol y estado ====================
  /** Normaliza sin depender de cómo escribe el backend el enum (Huésped, HUESPED, ...). */
  private normalizar(valor: unknown): string {
    return String(valor ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase();
  }

  get rol(): 'ADMIN' | 'ANFITRION' | 'HUESPED' | '' {
    const r = this.normalizar(this.usuario?.rol);
    if (r.includes('ADMIN')) return 'ADMIN';
    if (r.includes('ANFITRION')) return 'ANFITRION';
    if (r.includes('HUESPED')) return 'HUESPED';
    return '';
  }

  get estado(): 'ACTIVO' | 'RESTRINGIDO' | 'INACTIVO' | '' {
    const e = this.normalizar(this.usuario?.estado);
    if (e.includes('RESTRING')) return 'RESTRINGIDO';
    if (e.includes('INACT')) return 'INACTIVO';
    if (e.includes('ACTIV')) return 'ACTIVO';
    return '';
  }

  get activo(): boolean {
    return this.estado === 'ACTIVO';
  }

  get claseEstado(): string {
    const tono =
      this.estado === 'ACTIVO'
        ? 'bg-emerald-50 text-emerald-700'
        : this.estado === 'RESTRINGIDO'
          ? 'bg-red-50 text-red-700'
          : 'bg-slate-100 text-slate-600';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tono;
  }

  claseAlojamiento(estado: unknown): string {
    const e = this.normalizar(estado);
    return e.includes('BLOQUE')
      ? 'bg-red-50 text-red-700'
      : e.includes('PAUSAD') || e.includes('PENDIENTE')
        ? 'bg-amber-50 text-amber-700'
        : e.includes('ACTIV')
          ? 'bg-emerald-50 text-emerald-700'
          : 'bg-slate-100 text-slate-600';
  }

  // ==================== Información visual ====================
  get informacion(): { label: string; value: string | number }[] {
    const u = this.usuario;
    if (!u) {
      return [];
    }
    return [
      { label: 'Nombre', value: u.nombre },
      { label: 'Correo electrónico', value: u.correo },
      { label: 'Rol', value: u.rol },
      { label: 'Estado de la cuenta', value: u.estado },
      {
        label: 'Fecha de registro',
        value: u.fechaRegistro ? new Date(u.fechaRegistro).toLocaleDateString('es-CO') : '',
      },
      { label: 'Identificador interno', value: u.idUsuario },
    ];
  }

  get tituloSeccion(): string {
    return this.rol === 'ANFITRION'
      ? 'Alojamientos publicados'
      : this.rol === 'HUESPED'
        ? 'Resumen de reservas'
        : 'Cuenta administrativa';
  }

  // ==================== Inicialización ====================
  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);

    if (!idParam || !Number.isInteger(id) || id <= 0) {
      this.errorCarga = true;
      this.mensajeError = 'El identificador del usuario no es válido.';
      this.loading = false;
      return;
    }

    this.idUsuario = id;
    this.cargarUsuario();
  }

  reintentar(): void {
    this.cargarUsuario();
  }

  /** Si `silencioso` es true no muestra el indicador de carga (se usa al refrescar tras una acción). */
  private cargarUsuario(silencioso = false): void {
    if (!silencioso) {
      this.loading = true;
    }
    this.errorCarga = false;

    // El backend no tiene GET /admin/usuarios/{id}: se busca dentro de la lista.
    this.adminService.usuarios().subscribe({
      next: (lista: Usuario[]) => {
        console.log('Usuarios recibidos:', lista);
        const encontrado = (lista ?? []).find((u) => u.idUsuario === this.idUsuario);

        if (!encontrado) {
          this.errorCarga = true;
          this.mensajeError = 'Usuario no encontrado.';
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }

        this.usuario = encontrado;
        this.loading = false;

        if (this.rol === 'ANFITRION') {
          this.cargarAlojamientos();
        } else if (this.rol === 'HUESPED') {
          this.cargarReservas();
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar el usuario:', error);
        this.errorCarga = true;
        this.mensajeError = error?.error?.mensaje ?? 'No se pudo cargar el usuario.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private cargarAlojamientos(): void {
    this.adminService.alojamientosDeUsuario(this.idUsuario).subscribe({
      next: (lista: Alojamiento[]) => {
        this.alojamientos = lista ?? [];
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar los alojamientos del usuario:', error);
        this.alojamientos = [];
        this.cdr.detectChanges();
      },
    });
  }

  private cargarReservas(): void {
    this.adminService.reservasDeUsuario(this.idUsuario).subscribe({
      next: (lista) => {
        this.totalReservas = lista?.length ?? 0;
        this.estanciasFinalizadas = (lista ?? []).filter((r) => r.completada).length;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar las reservas del usuario:', error);
        this.totalReservas = 0;
        this.estanciasFinalizadas = 0;
        this.cdr.detectChanges();
      },
    });
  }

  // ==================== Navegación ====================
  volver(): void {
    this.router.navigateByUrl('/admin/usuarios');
  }

  // ==================== Modal de confirmación ====================
  accion: TipoAccion | null = null;
  motivo = '';
  guardando = false;
  errorAccion = '';

  get modal() {
    return this.accion ? ACCIONES[this.accion] : null;
  }

  pedirAccion(): void {
    this.motivo = '';
    this.errorAccion = '';
    this.accion = this.activo ? 'restringir' : 'activar';
  }

  onMotivoChange(event: Event): void {
    this.motivo = (event.target as HTMLTextAreaElement).value;
  }

  cerrarAccion(): void {
    if (this.guardando) {
      return;
    }
    this.accion = null;
  }

  confirmarAccion(): void {
    if (!this.accion || this.guardando) {
      return;
    }

    const motivo = this.motivo.trim();
    const peticion =
      this.accion === 'restringir'
        ? this.adminService.restringir(this.idUsuario, motivo)
        : this.adminService.liberar(this.idUsuario, motivo || undefined);

    this.guardando = true;
    this.errorAccion = '';

    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.accion = null;
        // Recarga para mostrar el estado real guardado por el backend.
        this.cargarUsuario(true);
      },
      error: (error) => {
        console.error('Error al actualizar el usuario:', error);
        this.errorAccion = error?.error?.mensaje ?? 'No se pudo completar la acción.';
        this.guardando = false;
        this.cdr.detectChanges();
      },
    });
  }
}
