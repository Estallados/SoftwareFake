package co.edu.unbosque.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Valores extremos que antes respondían 500 se manejan sin error interno. */
class EntradasExtremasTest extends PruebaIntegracionBase {

	@Test
	void unaPaginaEnormeNoRevientaLaConsulta() throws Exception {
		llamar(get("/alojamientos/disponibles?pagina=2147483647"), null, null, 200);
		llamar(get("/alojamientos?pagina=999999999&tamanio=100"), null, null, 200);
		llamar(get("/notificaciones?pagina=2147483647"), registrar("HUESPED").token(), null, 200);
	}

	@Test
	void laContrasenaNoPuedeSuperarLosLimitesDeBcrypt() throws Exception {
		llamar(post("/auth/register"), null, Map.of("nombre", "Larga", "correo", unico("larga") + "@test.com",
				"contrasenia", "Aa1#" + "a".repeat(61), "rol", "HUESPED"), 400);
		llamar(post("/auth/register"), null, Map.of("nombre", "Limite", "correo", unico("limite") + "@test.com",
				"contrasenia", "Aa1#" + "a".repeat(60), "rol", "HUESPED"), 201);
	}

	@Test
	void losMontosGigantesSeRechazanConCuatrocientos() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);

		llamar(put("/alojamientos/" + id), anfitrion.token(), Map.of("precioNoche", 1e40), 400);
		llamar(post("/alojamientos/" + id + "/servicios"), anfitrion.token(),
				Map.of("nombre", "Lujo", "costo", 1e40), 400);
	}
}
