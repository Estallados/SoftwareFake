package co.edu.unbosque.marketplace.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.entity.Usuario;

/**
 * Interfaz que define el repositorio para la entidad {@link Usuario}.
 * Proporciona métodos para realizar operaciones CRUD y consultas personalizadas
 * sobre la entidad {@link Usuario} en la base de datos.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario en la base de datos por su correo electrónico.
     *
     * @param correo Correo electrónico del usuario a buscar.
     * @return Un objeto {@link Optional} que contiene el usuario si existe,
     *         o vacío si no se encuentra.
     */
    public Optional<Usuario> findByCorreo(String correo);

    /**
     * Elimina un usuario de la base de datos según su correo electrónico.
     *
     * @param correo Correo electrónico del usuario a eliminar.
     */
    public void deleteByCorreo(String correo);

    /**
     * Verifica si existe un usuario en la base de datos con el correo electrónico especificado.
     *
     * @param correo Correo electrónico del usuario a verificar.
     * @return {@code true} si existe un usuario con el correo especificado,
     *         {@code false} en caso contrario.
     */
    public boolean existsByCorreo(String correo);
}
