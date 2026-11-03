import { CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Reserva } from '../../../models/reservaDto';
import { MensajeriaService } from '../../../services/mensajeria.service';
import { ReservaService } from '../../../services/reserva.service';
import {ResenaService} from '../../../services/reesna.service';
@Component({
  selector: 'app-guest-reservation-detail',
  standalone: true,
  templateUrl: './reservation-detail.html',
  imports: [CurrencyPipe],
})
export class ReservationDetail implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly reservaService = inject(ReservaService);
  private readonly resenaService = inject(ResenaService);
  private readonly mensajeriaService = inject(MensajeriaService);
  // ==================== Reserva ====================
  reserva: Reserva | null = null;
  loading = true;
  errorCarga = false;
  mensajeError = '';
  aviso = '';
  private idReserva = 0;
  /** ?accion=cancelar|resenar (viene de los botones del listado) abre el modal al cargar. */
  private accionPendiente: string | null = null;
  private temporizadorAviso?: ReturnType<typeof setTimeout>;
  // ==================== Estado de la reserva ====================
  /** Fecha de hoy (local) como YYYY-MM-DD, comparable con fechaEntrada. */
  private get hoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  private get yaInicio(): boolean {
    return !!this.reserva && this.reserva.fechaEntrada <= this.hoy;
  }
  get etiqueta(): string {
    const r = this.reserva;
    if (!r) return '';
    if (r.estado === 'CANCELADA') return 'Cancelada';
    if (r.completada) return 'Finalizada';
    if (r.estado === 'EN_REVISION') return 'En revisión';
    if (r.estado === 'PENDIENTE_PAGO') return 'Pendiente de pago';
    return this.yaInicio ? 'Activa' : 'Confirmada';
  }
  get claseEtiqueta(): string {
    const e = this.etiqueta;
    const tono =
      e === 'Cancelada'
        ? 'bg-red-50 text-red-700'
        : e === 'Finalizada'
          ? 'bg-slate-100 text-slate-600'
          : e === 'Activa'
            ? 'bg-sky-50 text-sky-700'
            : e === 'Confirmada'
              ? 'bg-emerald-50 text-emerald-700'
              : 'bg-amber-50 text-amber-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tono;
  }
  /** Solo se puede cancelar una reserva confirmada que aún no empieza. */
  get puedeCancelar(): boolean {
    const r = this.reserva;
    return !!r && r.estado === 'CONFIRMADA' && !r.completada && !this.yaInicio;
  }
  /** Se puede reseñar cuando la reserva ya finalizó y aún no tiene reseña. */
  get puedeResenar(): boolean {
    const r = this.reserva;
    return !!r && !!r.completada && r.estado !== 'CANCELADA' && !r.tieneResena;
  }
  get tieneResena(): boolean {
    return !!this.reserva?.tieneResena;
  }
  get finalizada(): boolean {
    return !!this.reserva?.completada && this.reserva?.estado !== 'CANCELADA';
  }
  // ==================== Información visual ====================
  get informacion(): { label: string; value: string | number }[] {
    const r = this.reserva;
    if (!r) {
      return [];
    }
    return [
      { label: 'Reserva', value: `#${r.idReserva}` },
      { label: 'Estado actual', value: this.etiqueta },
      { label: 'Fecha de creación', value: this.fechaCorta(r.fechaCreacion) },
      { label: 'Entrada', value: this.fechaCorta(r.fechaEntrada) },
      { label: 'Salida', value: this.fechaCorta(r.fechaSalida) },
      { label: 'Número de noches', value: r.noches },
      { label: 'Huéspedes', value: r.huespedes },
    ];
  }
  /** El desglose viene congelado como texto: si es JSON se muestra como filas, si no, línea por línea. */
  get lineasDesglose(): { label: string; value: string }[] {
    const texto = this.reserva?.desglose;
    if (!texto) {
      return [];
    }
    try {
      const objeto = JSON.parse(texto);
      if (objeto && typeof objeto === 'object' && !Array.isArray(objeto)) {
        return Object.entries(objeto)
          .filter(([, valor]) => valor === null || typeof valor !== 'object')
          .map(([clave, valor]) => ({ label: this.nombreDeClave(clave), value: this.formatearValor(clave, valor) }));
      }
    } catch {
      // no es JSON: se muestra tal cual
    }
    return texto
      .split(/\r?\n/)
      .map((linea) => linea.trim())
      .filter(Boolean)
      .map((linea) => ({ label: linea, value: '' }));
  }
  private nombreDeClave(clave: string): string {
    const texto = clave.replace(/([A-Z])/g, ' $1').replace(/[_-]+/g, ' ').trim().toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
  private formatearValor(clave: string, valor: unknown): string {
    if (typeof valor === 'number') {
      const noEsDinero = /noche|porcentaje|huesped|cantidad/i.test(clave);
      return noEsDinero
        ? String(valor)
        : new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(valor);
    }
    return String(valor ?? '');
  }
  /** Acepta YYYY-MM-DD (sin desfase de zona horaria) o una fecha-hora ISO. */
  private fechaCorta(valor: string | undefined): string {
    if (!valor) {
      return '';
    }
    const soloFecha = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valor);
    const fecha = soloFecha ? new Date(+soloFecha[1], +soloFecha[2] - 1, +soloFecha[3]) : new Date(valor);
    return Number.isNaN(fecha.getTime())
      ? ''
      : fecha.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }
  fechaCancelacion(): string {
    return this.fechaCorta(this.reserva?.cancelacion?.fecha);
  }
  // ==================== Inicialización ====================
  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);
    if (!idParam || !Number.isInteger(id) || id <= 0) {
      this.errorCarga = true;
      this.mensajeError = 'El identificador de la reserva no es válido.';
      this.loading = false;
      return;
    }
    this.idReserva = id;
    this.accionPendiente = this.route.snapshot.queryParamMap.get('accion');
    this.cargarReserva();
  }
  reintentar(): void {
    this.cargarReserva();
  }
  /** Si `silencioso` es true no muestra el indicador de carga (se usa al refrescar tras una acción). */
  private cargarReserva(silencioso = false): void {
    if (!silencioso) {
      this.loading = true;
    }
    this.errorCarga = false;
    this.reservaService.obtener(this.idReserva).subscribe({
      next: (respuesta: Reserva) => {
        console.log('Reserva recibida:', respuesta);
        this.reserva = respuesta;
        this.loading = false;
        this.aplicarAccionPendiente();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar la reserva:', error);
        if (!silencioso) {
          this.errorCarga = true;
          this.mensajeError = this.reservaService.mensajeDeError(error, 'No se pudo cargar la reserva.');
          this.loading = false;
        }
        this.cdr.detectChanges();
      },
    });
  }
  private aplicarAccionPendiente(): void {
    const accion = this.accionPendiente;
    if (!accion) {
      return;
    }
    this.accionPendiente = null;
    if (accion === 'cancelar' && this.puedeCancelar) {
      this.abrirCancelacion();
    } else if (accion === 'resenar' && this.puedeResenar) {
      this.abrirResena();
    }
    // Limpia ?accion= para que no se reabra al refrescar la página.
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { accion: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
  private mostrarAviso(texto: string): void {
    clearTimeout(this.temporizadorAviso);
    this.aviso = texto;
    this.temporizadorAviso = setTimeout(() => {
      this.aviso = '';
      this.cdr.detectChanges();
    }, 5000);
  }
  // ==================== Navegación ====================
  volver(): void {
    this.router.navigateByUrl('/reservas');
  }
  verResenas(): void {
    this.router.navigateByUrl('/resenas');
  }
  /** Inicia (o retoma) la conversación con el anfitrión del alojamiento y la abre en Mensajes. */
  contactando = false;
  errorContacto = '';
  contactarAnfitrion(): void {
    const idAlojamiento = this.reserva?.idAlojamiento;
    if (idAlojamiento == null || this.contactando) {
      return;
    }
    this.contactando = true;
    this.errorContacto = '';
    this.mensajeriaService.iniciar(idAlojamiento).subscribe({
      next: (conversacion) => {
        this.contactando = false;
        this.router.navigate(['/mensajes'], { queryParams: { conversacion: conversacion.id } });
      },
      error: (error) => {
        console.error('Error al iniciar la conversación:', error);
        this.errorContacto = this.mensajeriaService.mensajeDeError(error, 'No se pudo abrir la conversación.');
        this.contactando = false;
        this.cdr.detectChanges();
      },
    });
  }
  // ==================== Cancelación ====================
  mostrarCancelacion = false;
  /** Solo tras un error de red: permite reintentar la MISMA cancelación con la misma clave. */
  private intentoCancelacion?: { motivo: string; clave: string };
  motivoCancelacion = '';
  cancelando = false;
  errorCancelacion = '';
  abrirCancelacion(): void {
    this.motivoCancelacion = '';
    this.errorCancelacion = '';
    this.mostrarCancelacion = true;
  }
  cerrarCancelacion(): void {
    if (this.cancelando) {
      return;
    }
    this.mostrarCancelacion = false;
  }
  onMotivoCancelacionChange(event: Event): void {
    this.motivoCancelacion = (event.target as HTMLTextAreaElement).value;
  }
  confirmarCancelacion(): void {
    const motivo = this.motivoCancelacion.trim();
    if (!motivo || this.cancelando) {
      return;
    }
    this.cancelando = true;
    this.errorCancelacion = '';
    const clave =
      this.intentoCancelacion?.motivo === motivo
        ? this.intentoCancelacion.clave
        : this.reservaService.nuevaClaveIdempotencia();
    this.reservaService.cancelar(this.idReserva, motivo, clave).subscribe({
      next: () => {
        this.intentoCancelacion = undefined;
        this.cancelando = false;
        this.mostrarCancelacion = false;
        this.mostrarAviso('Tu reserva fue cancelada.');
        this.cargarReserva(true); // trae el estado real y los datos del reembolso
      },
      error: (error) => {
        console.error('Error al cancelar la reserva:', error);
        this.intentoCancelacion = error?.status === 0 ? { motivo, clave } : undefined;
        this.errorCancelacion = this.reservaService.mensajeDeError(error, 'No se pudo cancelar la reserva.');
        this.cancelando = false;
        this.cdr.detectChanges();
      },
    });
  }
  // ==================== Reseña ====================
  readonly starList = [1, 2, 3, 4, 5];
  readonly MAX_COMENTARIO = 2000;
  mostrarResena = false;
  calificacion = 0;
  calificacionHover = 0;
  comentario = '';
  enviandoResena = false;
  errorResena = '';
  abrirResena(): void {
    this.calificacion = 0;
    this.calificacionHover = 0;
    this.comentario = '';
    this.errorResena = '';
    this.mostrarResena = true;
  }
  cerrarResena(): void {
    if (this.enviandoResena) {
      return;
    }
    this.mostrarResena = false;
  }
  elegirCalificacion(valor: number): void {
    this.calificacion = valor;
  }
  onComentarioChange(event: Event): void {
    this.comentario = (event.target as HTMLTextAreaElement).value;
  }
  enviarResena(): void {
    if (this.enviandoResena) {
      return;
    }
    if (this.calificacion < 1 || this.calificacion > 5) {
      this.errorResena = 'Elige una calificación de 1 a 5 estrellas.';
      return;
    }
    const comentario = this.comentario.trim();
    if (comentario.length > this.MAX_COMENTARIO) {
      this.errorResena = `El comentario no puede superar ${this.MAX_COMENTARIO} caracteres.`;
      return;
    }
    this.enviandoResena = true;
    this.errorResena = '';
    this.resenaService
      .crear({ idReserva: this.idReserva, calificacion: this.calificacion, comentario: comentario || undefined })
      .subscribe({
        next: () => {
          this.enviandoResena = false;
          this.mostrarResena = false;
          this.mostrarAviso('¡Gracias! Tu reseña fue publicada.');
          this.cargarReserva(true); // actualiza tieneResena
        },
        error: (error) => {
          console.error('Error al publicar la reseña:', error);
          this.errorResena = this.reservaService.mensajeDeError(error, 'No se pudo publicar la reseña.');
          this.enviandoResena = false;
          this.cdr.detectChanges();
        },
      });
  }
}
