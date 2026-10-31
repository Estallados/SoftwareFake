import { Location } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import * as L from 'leaflet';
import { Alojamiento } from '../../../models/alojamientoDto';
import { AlojamientoService } from '../../../services/alojamiento.service';

/** Si el DTO trae coordenadas se usan; si no, se muestra la zona de la ciudad. */
type ConCoordenadas = Alojamiento & { latitud?: number | null; longitud?: number | null };

@Component({
  selector: 'app-map-view',
  standalone: true,
  templateUrl: './map-view.html',
})
export class MapView implements OnInit, AfterViewInit, OnDestroy {
  private readonly location = inject(Location);
  private readonly alojamientoService = inject(AlojamientoService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);

  @ViewChild('mapa', { static: true }) private mapaRef!: ElementRef<HTMLDivElement>;

  stay: Alojamiento | null = null;
  loading = true;
  errorCarga = false;
  /** true cuando no hay coordenadas y se muestra el centro de la ciudad. */
  zonaPorCiudad = false;

  private mapa?: L.Map;
  private zona?: L.Circle;

  private readonly RADIO_ZONA_M = 600;
  private readonly RADIO_CIUDAD_M = 4000;
  private readonly CENTRO_DEFECTO: L.LatLngTuple = [4.711, -74.0721]; // Bogotá

  private readonly centrosCiudad: Record<string, L.LatLngTuple> = {
    bogota: [4.711, -74.0721],
    medellin: [6.2442, -75.5812],
    cali: [3.4516, -76.532],
    cartagena: [10.391, -75.4794],
    barranquilla: [10.9685, -74.7813],
    bucaramanga: [7.1193, -73.1227],
    'santa marta': [11.2408, -74.199],
    pereira: [4.8133, -75.6961],
    manizales: [5.0703, -75.5138],
    'villa de leyva': [5.6347, -73.5266],
    guatape: [6.2339, -75.1594],
  };

  ngOnInit(): void {
    this.cargarAlojamiento();
  }

  ngAfterViewInit(): void {
    this.iniciarMapa();
    // El alojamiento pudo llegar antes de que el mapa existiera (respuesta en caché).
    this.mostrarZona();
  }

  ngOnDestroy(): void {
    // Libera el contenedor; sin esto Leaflet falla al volver a entrar a la página.
    this.mapa?.remove();
  }

  back(): void {
    this.location.back();
  }

  // ==================== MAPA ====================

  private iniciarMapa(): void {
    this.mapa = L.map(this.mapaRef.nativeElement, {
      center: this.CENTRO_DEFECTO,
      zoom: 12,
      scrollWheelZoom: false, // no secuestra el scroll de la página
    });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(this.mapa);
  }

  /** Dibuja el círculo de zona aproximada (sin marcador, no revela la dirección exacta). */
  private mostrarZona(): void {
    if (!this.mapa || !this.stay) {
      return;
    }
    const { centro, radio, porCiudad } = this.calcularZona(this.stay);
    this.zonaPorCiudad = porCiudad;
    this.zona?.remove();
    this.zona = L.circle(centro, {
      radius: radio,
      color: '#047857',
      weight: 2,
      fillColor: '#10b981',
      fillOpacity: 0.2,
    })
      .addTo(this.mapa)
      .bindPopup(this.crearPopup(this.stay));
    this.mapa.fitBounds(this.zona.getBounds(), { padding: [24, 24], maxZoom: 15 });
  }

  private calcularZona(stay: ConCoordenadas): { centro: L.LatLngTuple; radio: number; porCiudad: boolean } {
    const { latitud, longitud } = stay;
    if (
      typeof latitud === 'number' && typeof longitud === 'number' &&
      Number.isFinite(latitud) && Number.isFinite(longitud)
    ) {
      return { centro: this.desplazar(latitud, longitud, stay.idAlojamiento ?? 0), radio: this.RADIO_ZONA_M, porCiudad: false };
    }
    const centro = this.centrosCiudad[this.normalizar(stay.ciudad ?? '')];
    return { centro: centro ?? this.CENTRO_DEFECTO, radio: this.RADIO_CIUDAD_M, porCiudad: true };
  }

  /**
   * Mueve el centro entre 150 y 350 m de forma estable (según el id), para que
   * el círculo contenga el punto real sin quedar centrado sobre él.
   */
  private desplazar(lat: number, lng: number, id: number): L.LatLngTuple {
    const angulo = (((id * 137) % 360) * Math.PI) / 180;
    const metros = 150 + ((id * 53) % 200);
    const dLat = (metros * Math.cos(angulo)) / 111320;
    const dLng = (metros * Math.sin(angulo)) / (111320 * Math.cos((lat * Math.PI) / 180));
    return [lat + dLat, lng + dLng];
  }

  /** Popup construido con DOM (textContent) para no inyectar HTML del anfitrión. */
  private crearPopup(stay: Alojamiento): HTMLElement {
    const caja = document.createElement('div');
    caja.className = 'text-center';
    const titulo = document.createElement('strong');
    titulo.textContent = stay.titulo;
    caja.appendChild(titulo);
    const portada = stay.imagenes?.[0];
    if (portada) {
      const img = document.createElement('img');
      img.src = portada;
      img.alt = `Foto de ${stay.titulo}`;
      img.className = 'mx-auto mt-2 block h-24 w-40 rounded object-cover';
      caja.appendChild(img);
    }
    return caja;
  }

  private normalizar(texto: string): string {
    return texto.trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  }

  // ==================== CARGA ====================

  private cargarAlojamiento(): void {
    this.loading = true;
    this.errorCarga = false;
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);
    if (!idParam || idParam.trim() === '' || !Number.isInteger(id) || id <= 0) {
      this.errorCarga = true;
      this.loading = false;
      return;
    }
    this.alojamientoService.obtener(id).subscribe({
      next: (respuesta) => {
        if (!respuesta) {
          this.errorCarga = true;
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }
        this.stay = respuesta;
        this.loading = false;
        this.mostrarZona();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar el alojamiento:', error);
        this.errorCarga = true;
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
}
