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
 * Genera reportes tabulares en PDF (OpenPDF) y Excel (Apache POI).
 *
 * @see "RF-006, CA-027"
 */
public final class ExportadorUtil {

	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final int ANCHO_COLUMNA = 22 * 256;

	private ExportadorUtil() {
		throw new IllegalStateException("Clase utilitaria");
	}

	/**
	 * Genera un PDF horizontal con título, fecha de generación y una tabla.
	 *
	 * @param titulo      Título del reporte.
	 * @param encabezados Nombres de las columnas.
	 * @param filas       Valores de cada fila, en el mismo orden de los
	 *                    encabezados.
	 * @return Bytes del PDF.
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
	 * Genera un libro de Excel con una hoja: encabezados en negrita y una fila por
	 * registro.
	 *
	 * @param titulo      Nombre de la hoja.
	 * @param encabezados Nombres de las columnas.
	 * @param filas       Valores de cada fila.
	 * @return Bytes del archivo .xlsx.
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
			// Ancho fijo: autoSizeColumn necesita fuentes del sistema y falla en servidores
			// sin interfaz
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
