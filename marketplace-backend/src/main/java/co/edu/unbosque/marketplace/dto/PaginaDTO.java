package co.edu.unbosque.marketplace.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Página de resultados. Todas las listas de la API se devuelven paginadas con
 * este formato, que es más estable y sencillo que serializar el {@code Page}
 * de Spring.
 *
 * @param <T> tipo de cada elemento
 */
public class PaginaDTO<T> {
	/** Elementos de la página. */
	private List<T> contenido;

	/** Número de la página, desde 0. */
	private int pagina;

	/** Cantidad máxima de elementos por página. */
	private int tamanio;

	/** Cantidad total de elementos en todas las páginas. */
	private long totalElementos;

	/** Cantidad total de páginas. */
	private int totalPaginas;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public PaginaDTO() {
	}

	/**
	 * Convierte una página de entidades en una página de DTO.
	 *
	 * @param <E>       tipo de las entidades
	 * @param <T>       tipo de los DTO
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

	/**
	 * Devuelve los elementos de la página.
	 *
	 * @return elementos de la página
	 */
	public List<T> getContenido() {
		return contenido;
	}

	/**
	 * Asigna los elementos de la página.
	 *
	 * @param contenido elementos de la página
	 */
	public void setContenido(List<T> contenido) {
		this.contenido = contenido;
	}

	/**
	 * Devuelve el número de la página, desde 0.
	 *
	 * @return número de la página, desde 0
	 */
	public int getPagina() {
		return pagina;
	}

	/**
	 * Asigna el número de la página, desde 0.
	 *
	 * @param pagina número de la página, desde 0
	 */
	public void setPagina(int pagina) {
		this.pagina = pagina;
	}

	/**
	 * Devuelve la cantidad máxima de elementos por página.
	 *
	 * @return cantidad máxima de elementos por página
	 */
	public int getTamanio() {
		return tamanio;
	}

	/**
	 * Asigna la cantidad máxima de elementos por página.
	 *
	 * @param tamanio cantidad máxima de elementos por página
	 */
	public void setTamanio(int tamanio) {
		this.tamanio = tamanio;
	}

	/**
	 * Devuelve la cantidad total de elementos en todas las páginas.
	 *
	 * @return cantidad total de elementos en todas las páginas
	 */
	public long getTotalElementos() {
		return totalElementos;
	}

	/**
	 * Asigna la cantidad total de elementos en todas las páginas.
	 *
	 * @param totalElementos cantidad total de elementos en todas las páginas
	 */
	public void setTotalElementos(long totalElementos) {
		this.totalElementos = totalElementos;
	}

	/**
	 * Devuelve la cantidad total de páginas.
	 *
	 * @return cantidad total de páginas
	 */
	public int getTotalPaginas() {
		return totalPaginas;
	}

	/**
	 * Asigna la cantidad total de páginas.
	 *
	 * @param totalPaginas cantidad total de páginas
	 */
	public void setTotalPaginas(int totalPaginas) {
		this.totalPaginas = totalPaginas;
	}
}
