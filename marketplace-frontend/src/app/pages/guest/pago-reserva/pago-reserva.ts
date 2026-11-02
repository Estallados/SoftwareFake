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
/** Opción "tarjeta válida": se traduce al método de prueba aprobado según la marca escrita. */
const APROBADO = 'APROBADO';
/** Texto de cada motivo de rechazo, según el sufijo del método de prueba de Stripe. */
const MOTIVOS_RECHAZO: Record<string, string> = {
  InsufficientFunds: 'fondos insuficientes',
  LostCard: 'tarjeta reportada como perdida',
  StolenCard: 'tarjeta reportada como robada',
  ExpiredCard: 'tarjeta vencida',
  IncorrectCvc: 'CVC incorrecto',
  IncorrectNumber: 'número incorrecto',
  ProcessingError: 'error de procesamiento',
};
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
  // Datos escritos en modo SIMULADO: solo se validan aquí, nunca se envían ni se guardan.
  numeroSim = signal('');
  vencimientoSim = signal('');
  cvcSim = signal('');
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
  readonly digitos = computed(() => this.numeroSim().replace(/\D/g, ''));
  readonly marca = computed<'visa' | 'mastercard' | 'amex' | ''>(() => {
    const d = this.digitos();
    if (/^4/.test(d)) return 'visa';
    if (/^(5[1-5]|2(2[2-9]|[3-6]\d|7[01]|720))/.test(d)) return 'mastercard';
    if (/^3[47]/.test(d)) return 'amex';
    return '';
  });
  readonly numeroValido = computed(() => {
    const d = this.digitos();
    return d.length >= 13 && d.length <= 19 && this.luhn(d);
  });
  readonly vencimientoValido = computed(() => {
    const m = /^(\d{2})\/(\d{2})$/.exec(this.vencimientoSim());
    if (!m) return false;
    const mes = Number(m[1]);
    // La tarjeta vale hasta el último día del mes indicado
    return mes >= 1 && mes <= 12 && new Date(2000 + Number(m[2]), mes, 0, 23, 59, 59) >= new Date();
  });
  readonly cvcValido = computed(() => this.cvcSim().length === (this.marca() === 'amex' ? 4 : 3));
  readonly datosSimValidos = computed(() => this.numeroValido() && this.vencimientoValido() && this.cvcValido());
  readonly errorNumeroSim = computed(() =>
    this.digitos().length >= 13 && !this.numeroValido() ? 'El número de tarjeta no es válido.' : null,
  );
  readonly errorVencimientoSim = computed(() =>
    this.vencimientoSim().length === 5 && !this.vencimientoValido() ? 'La fecha no es válida o la tarjeta está vencida.' : null,
  );
  /** Resultado que se quiere simular: tarjeta válida o cada motivo de rechazo del backend. */
  readonly resultados = computed(() => {
    const metodos = this.config()?.metodosPrueba ?? [];
    const opciones: { valor: string; texto: string }[] = [];
    if (metodos.some((m) => !this.esRechazo(m))) {
      opciones.push({ valor: APROBADO, texto: 'Tarjeta válida · pago aprobado' });
    }
    for (const m of metodos.filter((x) => this.esRechazo(x))) {
      opciones.push({ valor: m.metodoPago, texto: `Tarjeta no válida · ${this.motivoRechazo(m)}` });
    }
    return opciones;
  });
  readonly modoStripe = computed(() => this.config()?.modo === 'STRIPE_TEST');
  readonly moneda = computed(() => (this.config()?.moneda ?? 'COP').toUpperCase());
  readonly puedePagar = computed(
    () =>
      !this.pagando() &&
      !!this.config() &&
      (this.modoStripe() ? this.tarjetaCompleta() : !!this.metodoPrueba() && this.datosSimValidos()),
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
          this.metodoPrueba.set(this.resultados()[0]?.valor ?? '');
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
  etiquetaMarca(): string {
    return { visa: 'Visa', mastercard: 'Mastercard', amex: 'American Express', '': '' }[this.marca()];
  }
  // ---- Campos del modo SIMULADO: se formatean mientras se escribe ----
  onNumero(input: HTMLInputElement): void {
    const bruto = input.value.replace(/\D/g, '');
    const amex = /^3[47]/.test(bruto);
    const d = bruto.slice(0, amex ? 15 : 19);
    const partes = amex ? [d.slice(0, 4), d.slice(4, 10), d.slice(10, 15)] : (d.match(/.{1,4}/g) ?? []);
    const formateado = partes.filter(Boolean).join(' ');
    input.value = formateado;
    this.numeroSim.set(formateado);
  }
  onVencimiento(input: HTMLInputElement): void {
    const d = input.value.replace(/\D/g, '').slice(0, 4);
    const formateado = d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
    input.value = formateado;
    this.vencimientoSim.set(formateado);
  }
  onCvc(input: HTMLInputElement): void {
    const d = input.value.replace(/\D/g, '').slice(0, 4);
    input.value = d;
    this.cvcSim.set(d);
  }
  private esRechazo(m: MetodoPagoPrueba): boolean {
    return /declin|fail|insufficient|stolen|lost|expired|incorrect|processing/i.test(m.metodoPago)
      || /rechaz|declin|fall/i.test(m.resultado);
  }
  private motivoRechazo(m: MetodoPagoPrueba): string {
    const sufijo = m.metodoPago.replace(/^pm_card_chargeDeclined/, '');
    if (MOTIVOS_RECHAZO[sufijo]) return MOTIVOS_RECHAZO[sufijo];
    return m.metodoPago === 'pm_card_chargeDeclined' ? 'tarjeta rechazada' : m.descripcion || m.metodoPago;
  }
  /** Método de prueba que se envía al backend según lo elegido y la marca escrita. */
  private metodoSimulado(): string {
    const elegido = this.metodoPrueba();
    if (elegido !== APROBADO) {
      return elegido;
    }
    const aprobados = (this.config()?.metodosPrueba ?? []).filter((m) => !this.esRechazo(m));
    const porMarca = aprobados.find((m) => m.metodoPago === `pm_card_${this.marca()}`);
    return (porMarca ?? aprobados.find((m) => m.metodoPago === 'pm_card_visa') ?? aprobados[0]).metodoPago;
  }
  private luhn(d: string): boolean {
    let suma = 0;
    let doble = false;
    for (let i = d.length - 1; i >= 0; i--) {
      let n = Number(d[i]);
      if (doble) {
        n *= 2;
        if (n > 9) n -= 9;
      }
      suma += n;
      doble = !doble;
    }
    return suma % 10 === 0;
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
      return this.metodoSimulado();
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
