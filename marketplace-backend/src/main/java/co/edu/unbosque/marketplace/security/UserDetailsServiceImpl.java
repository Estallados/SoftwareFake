package co.edu.unbosque.marketplace.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.entity.Usuario;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.util.AESUtil;

/**
 * Implementación personalizada de la interfaz UserDetailsService.
 * 
 * Esta clase se encarga de cargar los detalles del usuario desde la base de datos
 * para el proceso de autenticación en Spring Security.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    /** Repositorio para acceder a los datos de los usuarios. */
    private final UsuarioRepository userRepository;
    
    /**
     * Constructor que inyecta el repositorio de usuarios.
     * 
     * @param userRepository repositorio que maneja las operaciones con la entidad Usuario
     */
    public UserDetailsServiceImpl(UsuarioRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Carga un usuario desde la base de datos según su correo electrónico.
     * 
     * El correo se encripta antes de realizar la búsqueda,
     * y si el usuario no existe, se lanza una excepción.
     * 
     * @param correo correo electrónico del usuario a autenticar
     * @return los detalles del usuario como objeto UserDetails
     * @throws UsernameNotFoundException si el usuario no es encontrado en la base de datos
     */
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        String correoEncriptado = AESUtil.encrypt(correo);
        Usuario usuario = userRepository.findByCorreo(correoEncriptado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
  
        return usuario;
    }
}
