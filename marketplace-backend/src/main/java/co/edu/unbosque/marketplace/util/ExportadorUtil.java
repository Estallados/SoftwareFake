package co.edu.unbosque.marketplace.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Genera los reportes en forma de tabla, en PDF (con OpenPDF) o en Excel (con
 * Apache POI).
 * <p>
 * Las dos salidas reciben lo mismo: un título, los encabezados de las columnas
 * y las filas como listas de textos. Así el servicio de reportes arma los datos
 * una sola vez y solo elige el formato al final.
 * </p>
 */
public final class ExportadorUtil {

	/** Formato de la fecha de generación que se imprime en el PDF. */
	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/** Ancho fijo de cada columna en Excel (22 caracteres, en unidades de POI). */
	private static final int ANCHO_COLUMNA = 22 * 256;

	/** Clase utilitaria: no se instancia. */
	private ExportadorUtil() {
		throw new IllegalStateException("Clase utilitaria");
	}

	/**
	 * Genera un PDF horizontal con el título, la fecha de generación, el número
	 * de registros y una tabla con los datos.
	 *
	 * @param titulo      título del reporte
	 * @param encabezados nombres de las columnas
	 * @param filas       valores de cada fila, en el mismo orden de los
	 *                    encabezados; los {@code null} se imprimen vacíos
	 * @return bytes del PDF
	 * @throws IllegalStateException si OpenPDF no puede armar el documento
	 */
	public static byte[] pdf(String titulo, List<String> encabezados, List<List<String>> filas) {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		Document documento = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
		try {
			PdfWriter.getInstance(documento, salida);
			documento.open();
			documento.add(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
			documento.add(new Paragraph(
					"Generado: " + LocalDateTime.now().format(FORMATO_FECHA) + " · Registros: " + filas.size(),
					FontFactory.getFont(FontFactory.HELVETICA, 9)));
			documento.add(new Paragraph(" "));

			PdfPTable tabla = new PdfPTable(encabezados.size());
			tabla.setWidthPercentage(100);
			com.lowagie.text.Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
			com.lowagie.text.Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
			for (String encabezado : encabezados) {
				tabla.addCell(new PdfPCell(new Phrase(encabezado, negrita)));
			}
			for (List<String> fila : filas) {
				for (String valor : fila) {
					tabla.addCell(new PdfPCell(new Phrase(valor == null ? "" : valor, normal)));
				}
			}
			documento.add(tabla);
		} catch (DocumentException e) {
			throw new IllegalStateException("No se pudo generar el PDF", e);
		} finally {
			documento.close();
		}
		return salida.toByteArray();
	}

	/**
	 * Genera un libro de Excel con una sola hoja: los encabezados en negrita y una
	 * fila por registro.
	 *
	 * @param titulo      nombre de la hoja; Excel solo admite 31 caracteres, así
	 *                    que se corta si es más largo
	 * @param encabezados nombres de las columnas
	 * @param filas       valores de cada fila; los {@code null} quedan vacíos
	 * @return bytes del archivo .xlsx
	 * @throws IllegalStateException si POI no puede escribir el archivo
	 */
	public static byte[] excel(String titulo, List<String> encabezados, List<List<String>> filas) {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Sheet hoja = libro.createSheet(titulo.length() > 31 ? titulo.substring(0, 31) : titulo);
			Font negrita = libro.createFont();
			negrita.setBold(true);
			CellStyle estiloEncabezado = libro.createCellStyle();
			estiloEncabezado.setFont(negrita);

			Row encabezado = hoja.createRow(0);
			for (int i = 0; i < encabezados.size(); i++) {
				encabezado.createCell(i).setCellValue(encabezados.get(i));
				encabezado.getCell(i).setCellStyle(estiloEncabezado);
			}
			for (int f = 0; f < filas.size(); f++) {
				Row fila = hoja.createRow(f + 1);
				List<String> valores = filas.get(f);
				for (int c = 0; c < valores.size(); c++) {
					fila.createCell(c).setCellValue(valores.get(c) == null ? "" : valores.get(c));
				}
			}
			// Uso un ancho fijo porque autoSizeColumn necesita las fuentes del sistema y
			// falla en servidores sin interfaz gráfica
			for (int i = 0; i < encabezados.size(); i++) {
				hoja.setColumnWidth(i, ANCHO_COLUMNA);
			}
			libro.write(salida);
			return salida.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo generar el Excel", e);
		}
	}
}
