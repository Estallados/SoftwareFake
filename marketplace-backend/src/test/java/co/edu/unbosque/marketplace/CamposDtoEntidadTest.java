package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/** Los DTO llevan las mismas columnas que su entidad, sin exponer datos sensibles. */
class CamposDtoEntidadTest extends PruebaIntegracionBase {

	@Test
	void laCotizacionTraeUsuarioFechaYDesglose() throws Exception {
		Sesion anfitrion = registrar("ANFITRION");
		Long id = crearAlojamientoActivo(anfitrion);
		Sesion huesped = registrar("HUESPED");

		JsonNode cotizacion = cotizar(huesped, id, 5, 2, 1, List.of(), 201);

		assertEquals(huesped.id(), cotizacion.get("idUsuario").asLong());
		assertFalse(cotizacion.get("fechaCreacion").isNull());
		assertTrue(cotizacion.has("desglose"));
	}

	@Test
	void laTarifaVigenteTraeSuId() throws Exception {
		String admin = tokenAdmin();
		llamar(put("/admin/tarifas"), admin, Map.of("porcentajeLimpieza", 5, "porcentajeServicio", 10), 200);

		JsonNode tarifa = llamar(get("/admin/tarifas"), admin, null, 200);

		assertTrue(tarifa.get("idConfiguracion").asLong() > 0);
	}

	@Test
	void lasImagenesTraenSuRutaDeArchivo() throws Exception {
		Long id = crearAlojamientoActivo(registrar("ANFITRION"));

		JsonNode imagen = llamar(get("/alojamientos/" + id), null, null, 200).get("detalleImagenes").get(0);

		assertTrue(imagen.has("rutaArchivo"));
	}

	@Test
	void elPerfilNoExponeElHashDelCorreoNiElSecretoTotp() throws Exception {
		JsonNode perfil = llamar(get("/usuarios/perfil"), registrar("HUESPED").token(), null, 200);

		assertFalse(perfil.has("correoHash"));
		assertFalse(perfil.has("secretoTotp"));
	}
}
