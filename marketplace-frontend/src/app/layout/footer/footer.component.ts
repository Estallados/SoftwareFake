import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  templateUrl: './footer.component.html',
})
export class FooterComponent {
  readonly groups = [
    { title: 'Explora', links: ['Alojamientos', 'Destinos', 'Tipos de propiedad'] },
    { title: 'Soporte', links: ['Centro de ayuda', 'Cancelaciones', 'Términos y condiciones'] },
    { title: 'Anfitriones', links: ['Publicar alojamiento', 'Administrar propiedades', 'Disponibilidad'] },
  ];
}
