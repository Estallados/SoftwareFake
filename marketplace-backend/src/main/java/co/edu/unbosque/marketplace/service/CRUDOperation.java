package co.edu.unbosque.marketplace.service;

import java.util.List;

import co.edu.unbosque.marketplace.entity.Usuario;

/**
 * Operaciones básicas (crear, leer, actualizar y eliminar) que comparten los
 * servicios que administran una entidad.
 *
 * @param <D> tipo de dato que maneja el servicio (normalmente un DTO)
 */
public interface CRUDOperation<D> {

	/**
	 * Crea una nueva entidad en la base de datos.
	 *
	 * @param data Entidad a crear.
	 * @return Número de registros afectados.
	 */
	public int create(D data);

	/**
	 * Elimina una entidad por su identificador.
	 *
	 * @param id Identificador de la entidad a eliminar.
	 * @return Número de registros afectados.
	 */
	public int deleteById(Long id);

	/**
	 * Obtiene todas las entidades almacenadas.
	 *
	 * @return Lista de todas las entidades.
	 */
	public List<D> getAll();

	/**
	 * Cuenta el número total de entidades almacenadas.
	 *
	 * @return Número total de entidades.
	 */
	public long count();

	/**
	 * Verifica si existe una entidad con el identificador especificado.
	 *
	 * @param id Identificador de la entidad.
	 * @return {@code true} si existe, {@code false} en caso contrario.
	 */
	public boolean exist(Long id);

	/**
	 * Actualiza una entidad por su identificador.
	 *
	 * @param id      Identificador de la entidad a actualizar.
	 * @param newData Nueva información de la entidad.
	 * @return Número de registros afectados.
	 */
	public int updateById(Long id, D newData);

	/**
	 * Obtiene un elemento por su id.
	 *
	 * @param id identificador del elemento
	 * @return el elemento encontrado
	 */
	public D getById(Long id);

	/**
	 * Verifica si el correo de un usuario ya está registrado.
	 *
	 * @param newUser usuario con el correo a verificar
	 * @return {@code true} si el correo ya está registrado
	 */
	public boolean findUsernameAlreadyTaken(Usuario newUser);

	/**
	 * Verifica si un correo ya existe en el sistema.
	 *
	 * @param username correo a verificar
	 * @return {@code true} si el correo existe
	 */
	boolean encontrarCorreoExitente(String username);
}
