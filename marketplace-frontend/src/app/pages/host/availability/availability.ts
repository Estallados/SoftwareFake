import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

export type ListingStatus = 'Activo' | 'Pausado' | 'Bloqueado' | 'Pendiente' | 'Rechazado';

export interface AvailabilityListing {
  id: string;
  name: string;
  image: string;
  city: string;
  type: string;
  status: ListingStatus;
}

export interface AvailabilityReservation {
  code: string;
  start: string; // AAAA-MM-DD
  end: string; // AAAA-MM-DD
  guests: number;
  status: 'Confirmada' | 'Cancelada' | 'Finalizada';
}

export interface CalendarMonth {
  label: string;
  prefix: string; // AAAA-MM
  days: number;
  offset: number; // casillas vacías antes del día 1 (Lun = 0)
}

export interface CalendarDay {
  key: string;
  day: number;
  reserved: boolean;
  blocked: boolean;
  past: boolean;
  selected: boolean;
  status: string;
}

@Component({
  selector: 'app-host-availability',
  standalone: true,
  templateUrl: './availability.html',
})
export class Availability {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  /** Parámetro :id de la ruta host/alojamientos/:id/disponibilidad */
  readonly id = this.route.snapshot.paramMap.get('id');

  // TODO: cargar desde el backend usando `id`
  listing = signal<AvailabilityListing>({
    id: '',
    name: '',
    image: '',
    city: '',
    type: '',
    status: 'Activo',
  });
  reservation = signal<AvailabilityReservation | null>(null);
  blockedDates = signal<string[]>([]);

  /** Fecha mínima seleccionable (antes de esta fecha los días son "no disponibles").
   *  TODO: usar la fecha actual real */
  readonly minDate = '2025-07-05';

  readonly months: CalendarMonth[] = [
    { label: 'Julio 2025', prefix: '2025-07', days: 31, offset: 1 },
    { label: 'Agosto 2025', prefix: '2025-08', days: 31, offset: 4 },
  ];
  readonly weekDays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
  readonly legend = [
    { color: 'bg-emerald-100 ring-emerald-200', label: 'Disponible' },
    { color: 'bg-red-100 ring-red-200', label: 'Reservado' },
    { color: 'bg-slate-400 ring-slate-400', label: 'Bloqueado' },
    { color: 'bg-slate-100 ring-slate-200', label: 'No seleccionable' },
  ];

  monthIndex = signal(0);
  selectedDate = signal('');
  blockOpen = signal(false);
  releaseOpen = signal(false);
  range = { start: '', end: '', reason: '' };
  error = signal('');

  month = computed(() => this.months[this.monthIndex()]);
  emptyCells = computed(() => Array.from({ length: this.month().offset }, (_, i) => i));

  // ---- Fechas ----
  private dateKeys(start: string, end: string): string[] {
    if (!start || !end) return [];
    const result: string[] = [];
    const current = new Date(`${start}T12:00:00`);
    const limit = new Date(`${end}T12:00:00`);
    while (current <= limit && result.length < 62) {
      result.push(current.toISOString().slice(0, 10));
      current.setDate(current.getDate() + 1);
    }
    return result;
  }

  reservedDates = computed(() => {
    const r = this.reservation();
    return r && r.status === 'Confirmada' ? this.dateKeys(r.start, r.end) : [];
  });

  calendar = computed<CalendarDay[]>(() => {
    const m = this.month();
    const reservedSet = new Set(this.reservedDates());
    const blockedSet = new Set(this.blockedDates());
    return Array.from({ length: m.days }, (_, i) => {
      const day = i + 1;
      const key = `${m.prefix}-${String(day).padStart(2, '0')}`;
      const reserved = reservedSet.has(key);
      const blocked = blockedSet.has(key);
      const past = key < this.minDate;
      return {
        key,
        day,
        reserved,
        blocked,
        past,
        selected: this.selectedDate() === key,
        status: reserved
          ? 'Reservado'
          : blocked
            ? 'Bloqueado'
            : past
              ? 'No disponible'
              : 'Disponible',
      };
    });
  });

  selectedReserved = computed(() => this.reservedDates().includes(this.selectedDate()));
  selectedBlocked = computed(() => this.blockedDates().includes(this.selectedDate()));
  selectedPast = computed(() => !!this.selectedDate() && this.selectedDate() < this.minDate);
  isBlockedListing = computed(() => this.listing().status === 'Bloqueado');

  selectedLabel = computed(() =>
    this.selectedReserved()
      ? 'Reservada'
      : this.selectedBlocked()
        ? 'Bloqueada'
        : this.selectedPast()
          ? 'No seleccionable'
          : 'Disponible',
  );

  selectedBadgeClass = computed(() => {
    const tone = this.selectedReserved()
      ? 'bg-red-50 text-red-700'
      : this.selectedBlocked() || this.selectedPast()
        ? 'bg-slate-100 text-slate-600'
        : 'bg-emerald-50 text-emerald-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  });

  listingBadgeClass = computed(() => {
    const s = this.listing().status;
    const tone =
      s === 'Activo'
        ? 'bg-emerald-50 text-emerald-700'
        : s === 'Bloqueado'
          ? 'bg-red-50 text-red-700'
          : 'bg-amber-50 text-amber-700';
    return 'inline-flex rounded-full px-3 py-1 text-xs font-bold ' + tone;
  });

  dayClass(d: CalendarDay) {
    const state = d.reserved
      ? 'border-red-200 bg-red-50 text-red-800'
      : d.blocked
        ? 'border-slate-400 bg-slate-200 text-slate-700'
        : d.past
          ? 'border-slate-100 bg-slate-50 text-slate-300'
          : 'border-emerald-100 bg-emerald-50 text-emerald-800 hover:border-emerald-400';
    return (
      'min-h-20 rounded-xl border p-3 text-left text-sm transition ' +
      (d.selected ? 'ring-2 ring-emerald-700 ring-offset-2 ' : '') +
      state
    );
  }

  // ---- Navegación del calendario ----
  prevMonth() {
    this.monthIndex.update((i) => Math.max(0, i - 1));
  }

  nextMonth() {
    this.monthIndex.update((i) => Math.min(this.months.length - 1, i + 1));
  }

  // ---- Bloquear fechas ----
  openBlock() {
    this.error.set('');
    this.blockOpen.set(true);
  }

  setRange(field: 'start' | 'end' | 'reason', event: Event) {
    this.range = { ...this.range, [field]: (event.target as HTMLInputElement).value };
  }

  confirmBlock() {
    this.error.set('');
    const { start, end } = this.range;
    if (!start || !end) {
      this.error.set('Selecciona una fecha inicial y una fecha final.');
      return;
    }
    if (start > end) {
      this.error.set('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }
    const requested = this.dateKeys(start, end);
    if (!requested.length || requested.some((d) => this.reservedDates().includes(d))) {
      this.error.set('El período incluye fechas reservadas y no puede bloquearse.');
      return;
    }
    if (requested.some((d) => d < this.minDate)) {
      this.error.set('El período incluye fechas pasadas o no seleccionables.');
      return;
    }
    this.blockedDates.update((list) => Array.from(new Set([...list, ...requested])));
    this.blockOpen.set(false);
    this.selectedDate.set(start);
    this.range = { start: '', end: '', reason: '' };
    // TODO: guardar en el backend y avisar "Las fechas se bloquearon correctamente"
  }

  // ---- Liberar fechas (todo el rango bloqueado contiguo) ----
  releaseSelectedRange() {
    if (!this.selectedBlocked() || this.selectedReserved()) return;
    const blocked = new Set(this.blockedDates());
    const connected = new Set([this.selectedDate()]);
    const walk = (direction: number) => {
      const cursor = new Date(`${this.selectedDate()}T12:00:00`);
      for (;;) {
        cursor.setDate(cursor.getDate() + direction);
        const key = cursor.toISOString().slice(0, 10);
        if (!blocked.has(key)) break;
        connected.add(key);
      }
    };
    walk(-1);
    walk(1);
    this.blockedDates.update((list) => list.filter((d) => !connected.has(d)));
    this.releaseOpen.set(false);
    // TODO: guardar en el backend y avisar "Las fechas vuelven a estar disponibles"
  }

  // ---- Navegación ----
  viewReservation(code: string) {
    this.router.navigate(['/host/reservas', code]);
  }

  back() {
    this.router.navigateByUrl('/host/alojamientos');
  }

  details() {
    this.router.navigate(['/host/alojamientos', this.id]);
  }

  edit() {
    this.router.navigate(['/host/alojamientos', this.id, 'editar']);
  }
}
