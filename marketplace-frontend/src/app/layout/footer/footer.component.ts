import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';


@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './footer.component.html',
})
export class FooterComponent {
 
  readonly groups = [
    {
      title: 'Explora',
      links: [
        { texto: 'Inicio', ruta: '/home' },
        { texto: 'Explorar', ruta: '/explorar' },
      ],
    },
    {
      title: 'Anfitriones',
      links: [
        { texto: 'Mis alojamientos', ruta: '/host/alojamientos' },
        { texto: 'Publicar alojamiento', ruta: '/host/alojamientos/crear' },
        { texto: 'Reservas', ruta: '/host/reservas' },
      ],
    },
  ];
}
