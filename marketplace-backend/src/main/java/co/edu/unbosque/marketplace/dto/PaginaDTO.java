package co.edu.unbosque.marketplace.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Página de resultados. Todas las listas de la API se devuelven paginadas con
 * este formato estable (en lugar de serializar {@code Page} de Spring).
 *
 * @param <T> tipo de cada elemento
 * @see "RNF-015"
 */
public class PaginaDTO<T> {
	private List<T> contenido;
	private int pagina;
	private int tamanio;
	private long totalElementos;
	private int totalPaginas;

	public PaginaDTO() {
	}

	/**
	 * Convierte una página de entidades en una página de DTO.
	 *
	 * @param page      página consultada al repositorio
	 * @param conversor función que pasa cada entidad a DTO
	 * @return la página de DTO
	 */
	public static <E, T> PaginaDTO<T> de(Page<E> page, Function<E, T> conversor) {
		PaginaDTO<T> resultado = new PaginaDTO<>();
		resultado.setContenido(page.getContent().stream().map(conversor).toList());
		resultado.setPagina(page.getNumber());
		resultado.setTamanio(page.getSize());
		resultado.setTotalElementos(page.getTotalElements());
		resultado.setTotalPaginas(page.getTotalPages());
		return resultado;
	}

	public List<T> getContenido() {
		return contenido;
	}

	public void setContenido(List<T> contenido) {
		this.contenido = contenido;
	}

	public int getPagina() {
		return pagina;
	}

	public void setPagina(int pagina) {
		this.pagina = pagina;
	}

	public int getTamanio() {
		return tamanio;
	}

	public void setTamanio(int tamanio) {
		this.tamanio = tamanio;
	}

	public long getTotalElementos() {
		return totalElementos;
	}

	public void setTotalElementos(long totalElementos) {
		this.totalElementos = totalElementos;
	}

	public int getTotalPaginas() {
		return totalPaginas;
	}

	public void setTotalPaginas(int totalPaginas) {
		this.totalPaginas = totalPaginas;
	}
}
