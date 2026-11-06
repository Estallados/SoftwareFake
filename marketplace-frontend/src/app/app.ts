import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {DashboardShellComponent} from './layout/dashboard-shell/dashboard-shell.component';
import {FooterComponent} from './layout/footer/footer.component';
import {NavbarComponent} from './layout/navbar/navbar.component';

/**
 * Componente raíz: muestra la barra de navegación, la página de la ruta actual y el pie de página.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, FooterComponent, NavbarComponent],
  templateUrl: './app.html',
})
export class App {

}
