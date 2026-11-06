package co.edu.unbosque.marketplace.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Role;
import co.edu.unbosque.marketplace.entity.Usuario;

/**
 * Acceso a los usuarios.
 * <p>
 * El correo se guarda cifrado con AES-GCM, con un IV aleatorio, así que no se
 * puede buscar por él directamente. Las búsquedas usan {@code correoHash}, que
 * es el HMAC del correo normalizado (ver {@code AESUtil.hash}).
 * </p>
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/**
	 * Busca un usuario por el hash de su correo electrónico.
	 *
	 * @param correoHash Hash del correo normalizado.
	 * @return Un objeto {@link Optional} que contiene el usuario si existe, o vacío
	 *         si no se encuentra.
	 */
	public Optional<Usuario> findByCorreoHash(String correoHash);

	/**
	 * Verifica si existe un usuario con el hash de correo especificado.
	 *
	 * @param correoHash Hash del correo normalizado.
	 * @return {@code true} si existe un usuario con ese correo, {@code false} en
	 *         caso contrario.
	 */
	public boolean existsByCorreoHash(String correoHash);

	/**
	 * Verifica si existe al menos un usuario con el rol indicado.
	 *
	 * @param rol Rol a buscar.
	 * @return {@code true} si hay algún usuario con ese rol.
	 */
	public boolean existsByRol(Role rol);
}
