import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component, ElementRef, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output, ViewChild,
  computed, inject, signal,
} from '@angular/core';
import type { Stripe, StripeCardCvcElement, StripeCardExpiryElement, StripeCardNumberElement } from '@stripe/stripe-js';
// "pure": Stripe.js solo se descarga cuando se llama a loadStripe (no en modo SIMULADO).
import { loadStripe } from '@stripe/stripe-js/pure';
import { Cotizacion } from '../../../models/cotizacionDto';
import { ConfiguracionPago, MetodoPagoPrueba } from '../../../models/pagoDto';
import { Reserva } from '../../../models/reservaDto';
import { PagoService } from '../../../services/pago.service';
import { ReservaService } from '../../../services/reserva.service';
type Campo = 'numero' | 'vencimiento' | 'cvc';
/** Modal de pago: muestra el desglose de la cotización y cobra con Stripe. */
@Component({
  selector: 'app-pago-reserva',
  standalone: true,
  templateUrl: './pago-reserva.html',
  imports: [CurrencyPipe, DatePipe],
})
export class PagoReserva implements OnInit, OnDestroy {
  @Input({ required: true }) cotizacion!: Cotizacion;
  @Input() titulo = '';
  @Output() reservado = new EventEmitter<Reserva>();
  @Output() cerrado = new EventEmitter<void>();
  /** Pide una cotización nueva (la actual venció, ya se usó o dejó de ser válida). */
  @Output() recotizar = new EventEmitter<void>();
  @ViewChild('numeroTarjeta', { static: true }) private numeroRef!: ElementRef<HTMLDivElement>;
  @ViewChild('vencimientoTarjeta', { static: true }) private vencimientoRef!: ElementRef<HTMLDivElement>;
  @ViewChild('cvcTarjeta', { static: true }) private cvcRef!: ElementRef<HTMLDivElement>;
  private readonly pagoService = inject(PagoService);
  private readonly reservaService = inject(ReservaService);
  config = signal<ConfiguracionPago | null>(null);
  cargandoConfig = signal(true);
  metodoPrueba = signal('');
  titular = signal('');
  pagando = signal(false);
  error = signal<string | null>(null);
  conflicto = signal(false);
  private campos = signal<Record<Campo, boolean>>({ numero: false, vencimiento: false, cvc: false });
  private errores = signal<Partial<Record<Campo, string>>>({});
  readonly tarjetaCompleta = computed(() => {
    const c = this.campos();
    return c.numero && c.vencimiento && c.cvc;
  });
  readonly errorTarjeta = computed(() => {
    const e = this.errores();
    return e.numero ?? e.vencimiento ?? e.cvc ?? null;
  });
  readonly modoStripe = computed(() => this.config()?.modo === 'STRIPE_TEST');
  readonly moneda = computed(() => (this.config()?.moneda ?? 'COP').toUpperCase());
  readonly puedePagar = computed(
    () =>
      !this.pagando() &&
      !!this.config() &&
      (this.modoStripe() ? this.tarjetaCompleta() : !!this.metodoPrueba()),
  );
  private stripe: Stripe | null = null;
  private numero: StripeCardNumberElement | null = null;
  private vencimiento: StripeCardExpiryElement | null = null;
  private cvc: StripeCardCvcElement | null = null;
  private destruido = false;
  /** Solo tras un error de red: permite reintentar la MISMA petición sin duplicar el cobro. */
  private intento?: { metodoPago: string; clave: string };
  ngOnInit(): void {
    this.pagoService.configuracion().subscribe({
      next: (config) => {
        this.config.set(config);
        this.cargandoConfig.set(false);
        if (config.modo === 'STRIPE_TEST') {
          void this.iniciarStripe(config.clavePublica);
        } else {
          this.metodoPrueba.set(config.metodosPrueba[0]?.metodoPago ?? '');
        }
      },
      error: (e: HttpErrorResponse) => {
        this.cargandoConfig.set(false);
        this.error.set(this.reservaService.mensajeDeError(e, 'No se pudo cargar la configuración de pagos.'));
      },
    });
  }
  ngOnDestroy(): void {
    this.destruido = true;
    this.numero?.destroy();
    this.vencimiento?.destroy();
    this.cvc?.destroy();
  }
  @HostListener('document:keydown.escape')
  cerrar(): void {
    if (!this.pagando()) {
      this.cerrado.emit();
    }
  }
  etiqueta(m: MetodoPagoPrueba): string {
    return m.resultado ? `${m.descripcion} · ${m.resultado}` : m.descripcion;
  }
  elegirMetodo(valor: string): void {
    this.metodoPrueba.set(valor);
    this.intento = undefined;
  }
  async pagar(): Promise<void> {
    if (!this.puedePagar()) {
      return;
    }
    this.error.set(null);
    this.conflicto.set(false);
    this.pagando.set(true);
    let metodoPago: string;
    try {
      metodoPago = this.intento?.metodoPago ?? (await this.obtenerMetodoPago());
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'No se pudo validar la tarjeta.');
      this.pagando.set(false);
      return;
    }
    const clave = this.intento?.clave ?? this.reservaService.nuevaClaveIdempotencia();
    this.reservaService.crear({ idCotizacion: this.cotizacion.idCotizacion, metodoPago }, clave).subscribe({
      next: (reserva) => {
        this.intento = undefined;
        this.pagando.set(false);
        this.reservado.emit(reserva);
      },
      error: (e: HttpErrorResponse) => {
        this.intento = e.status === 0 ? { metodoPago, clave } : undefined;
        this.conflicto.set(e.status === 409);
        this.error.set(
          e.status === 0
            ? 'No hay conexión con el servidor. Puedes reintentar: no se duplicará el cobro.'
            : this.reservaService.mensajeDeError(e, 'No se pudo completar el pago.'),
        );
        this.pagando.set(false);
      },
    });
  }
  private async obtenerMetodoPago(): Promise<string> {
    if (!this.modoStripe()) {
      return this.metodoPrueba();
    }
    if (!this.stripe || !this.numero) {
      throw new Error('El formulario de tarjeta no está listo.');
    }
    // Con campos separados, Stripe toma vencimiento y CVC de los elementos hermanos.
    const { paymentMethod, error } = await this.stripe.createPaymentMethod({
      type: 'card',
      card: this.numero,
      billing_details: { name: this.titular().trim() || undefined },
    });
    if (error || !paymentMethod) {
      throw new Error(error?.message ?? 'No se pudo validar la tarjeta.');
    }
    return paymentMethod.id;
  }
  private async iniciarStripe(clavePublica: string | null): Promise<void> {
    if (!clavePublica) {
      this.error.set('El servidor no entregó la clave publicable de Stripe.');
      return;
    }
    try {
      const stripe = await loadStripe(clavePublica);
      if (!stripe || this.destruido) {
        return;
      }
      this.stripe = stripe;
      const elements = stripe.elements({ locale: 'es' });
      const estilo = { style: { base: { fontSize: '15px', color: '#0f172a', '::placeholder': { color: '#94a3b8' } } } };
      this.numero = elements.create('cardNumber', { ...estilo, showIcon: true });
      this.vencimiento = elements.create('cardExpiry', estilo);
      this.cvc = elements.create('cardCvc', estilo);
      this.numero.mount(this.numeroRef.nativeElement);
      this.vencimiento.mount(this.vencimientoRef.nativeElement);
      this.cvc.mount(this.cvcRef.nativeElement);
      this.numero.on('change', (e) => this.actualizar('numero', e.complete, e.error?.message));
      this.vencimiento.on('change', (e) => this.actualizar('vencimiento', e.complete, e.error?.message));
      this.cvc.on('change', (e) => this.actualizar('cvc', e.complete, e.error?.message));
    } catch {
      this.error.set('No se pudo cargar el formulario de tarjeta.');
    }
  }
  private actualizar(campo: Campo, completo: boolean, error?: string): void {
    this.campos.update((c) => ({ ...c, [campo]: completo }));
    this.errores.update((e) => ({ ...e, [campo]: error }));
    this.intento = undefined;
  }
}
