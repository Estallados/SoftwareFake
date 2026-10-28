package co.edu.unbosque.marketplace.service;

import java.util.List;

import co.edu.unbosque.marketplace.entity.Usuario;

/**
 * Interfaz que define las operaciones CRUD (Crear, Leer, Actualizar, Eliminar)
 * para cualquier entidad genérica.
 *
 * @param <D> Tipo de dato genérico que representa la entidad.
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
	 * Obtiene un usuario por su ID.
	 *
	 * @param id ID del usuario.
	 * @return DTO del usuario encontrado.
	 */
	public D getById(Long id);

	/**
	 * Verifica si un correo de usuario ya está registrado.
	 *
	 * @param newUser Usuario con el correo a verificar.
	 * @return true si el correo ya está registrado, false en caso contrario.
	 */
	public boolean findUsernameAlreadyTaken(Usuario newUser);

	/**
	 * Verifica si un correo ya existe en el sistema.
	 *
	 * @param username Correo a verificar.
	 * @return true si el correo existe, false en caso contrario.
	 */
	boolean encontrarCorreoExitente(String username);
}
