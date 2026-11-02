package co.edu.unbosque.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Los textos más largos que su columna se rechazan con 400, no con 500. */
class LimitesTextoTest extends PruebaIntegracionBase {

	@Test
	void elRegistroRechazaNombreYCorreoDemasiadoLargos() throws Exception {
		llamar(post("/auth/register"), null, Map.of("nombre", "a".repeat(256), "correo", unico("largo") + "@test.com",
				"contrasenia", CLAVE, "rol", "HUESPED"), 400);
		llamar(post("/auth/register"), null, Map.of("nombre", "Largo", "correo", "a".repeat(150) + "@test.com",
				"contrasenia", CLAVE, "rol", "HUESPED"), 400);
	}

	@Test
	void elAlojamientoRechazaTitulosYDescripcionesDemasiadoLargos() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Map<String, Object> titulo = new HashMap<>(datosAlojamientoBasico());
		titulo.put("titulo", "t".repeat(256));
		crearBorrador(anfitrion, titulo, 400);
		Map<String, Object> descripcion = new HashMap<>(datosAlojamientoBasico());
		descripcion.put("descripcion", "d".repeat(2001));
		crearBorrador(anfitrion, descripcion, 400);
	}

	@Test
	void laCancelacionRechazaUnMotivoDemasiadoLargo() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));
		Sesion huesped = registrar("HUESPED");
		long idReserva = reservaConfirmada(huesped, id, 30, 2).get("idReserva").asLong();

		llamar(post("/reservas/" + idReserva + "/cancelar").header("Idempotency-Key", unico("clave")),
				huesped.token(), Map.of("motivo", "m".repeat(501)), 400);
	}

	private Map<String, Object> datosAlojamientoBasico() {
		return datosAlojamiento("Bogotá", "Casa", 4, new BigDecimal("100000"), Set.of(),
				"FLEXIBLE");
	}
}
