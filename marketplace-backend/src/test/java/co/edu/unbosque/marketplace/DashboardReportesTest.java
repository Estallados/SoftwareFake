package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.RegistroExportacionRepository;

/** Módulo 8: dashboard administrativo y exportación de reportes. */
class DashboardReportesTest extends PruebaIntegracionBase {

	@Autowired
	private AlojamientoRepository alojamientoRepo;

	@Autowired
	private RegistroExportacionRepository exportacionRepo;

	private MvcResult descargar(String url, String token) throws Exception {
		return mvc.perform(get(url).header("Authorization", "Bearer " + token)).andReturn();
	}

	@Test
	void elDashboardMuestraTodosLosIndicadores_CA022_CA023_CA024_CA025_CA026() throws Exception {
		String ciudad = unico("Demanda");
		Long id = crearAlojamientoActivo(registrar("ANFITRION"), ciudad, "Casa", 4, new BigDecimal("100000"),
				Set.of(), "FLEXIBLE");
		Sesion huesped = registrar("HUESPED");
		reservaConfirmada(huesped, id, 10, 2);
		reservaConfirmada(huesped, id, 20, 2);

		JsonNode dashboard = llamar(get("/admin/dashboard"), tokenAdmin(), null, 200);

		assertTrue(dashboard.get("totalUsuarios").asLong() >= 3);
		assertEquals(alojamientoRepo.countByEstado(EstadoAlojamiento.ACTIVA),
				dashboard.get("alojamientosActivos").asLong());
		assertTrue(dashboard.get("reservasPorEstado").get("CONFIRMADA").asLong() >= 2);
		assertTrue(dashboard.get("reservasPorEstado").has("CANCELADA"));
		assertTrue(dashboard.get("ingresosEstimados").decimalValue().signum() > 0);
		assertTrue(dashboard.get("ciudadesMayorDemanda").isArray());
	}

	@Test
	void soloElAdministradorConsultaElDashboard_CA131() throws Exception {
		llamar(get("/admin/dashboard"), registrar("ANFITRION").token(), null, 403);
	}

	@Test
	void exportaEnPdfYRegistraSolicitanteYFecha_CA027_CA029() throws Exception {
		String admin = tokenAdmin();
		long antes = exportacionRepo.count();

		MvcResult pdf = descargar("/admin/reportes/reservas?formato=pdf", admin);

		assertEquals(200, pdf.getResponse().getStatus());
		assertEquals("application/pdf", pdf.getResponse().getContentType());
		byte[] contenido = pdf.getResponse().getContentAsByteArray();
		assertEquals("%PDF", new String(contenido, 0, 4, StandardCharsets.US_ASCII));
		assertEquals(antes + 1, exportacionRepo.count());
		var registro = exportacionRepo.findAll().stream().max((a, b) -> a.getIdRegistro().compareTo(b.getIdRegistro()))
				.orElseThrow();
		assertEquals("reservas", registro.getTipoReporte());
		assertTrue(registro.getIdUsuario() != null && registro.getFecha() != null);
	}

	@Test
	void elExcelCorrespondeConLaInformacionDelSistema_CA027_CA028() throws Exception {
		crearAlojamientoActivo(registrar("ANFITRION"));

		MvcResult excel = descargar("/admin/reportes/alojamientos?formato=excel", tokenAdmin());

		assertEquals(200, excel.getResponse().getStatus());
		try (XSSFWorkbook libro = new XSSFWorkbook(
				new ByteArrayInputStream(excel.getResponse().getContentAsByteArray()))) {
			int filasDeDatos = libro.getSheetAt(0).getLastRowNum();
			assertEquals(alojamientoRepo.count(), filasDeDatos);
			assertEquals("Título", libro.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
		}
	}

	@Test
	void todosLosReportesSeGeneranYUnTipoInvalidoSeRechaza() throws Exception {
		String admin = tokenAdmin();
		for (String tipo : new String[] { "usuarios", "alojamientos", "reservas", "ingresos", "demanda" }) {
			assertEquals(200, descargar("/admin/reportes/" + tipo + "?formato=excel", admin).getResponse().getStatus(),
					tipo);
		}
		llamar(get("/admin/reportes/inventado"), admin, null, 400);
		llamar(get("/admin/reportes/usuarios?formato=word"), admin, null, 400);
	}
}
