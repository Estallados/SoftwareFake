package co.edu.unbosque.marketplace.entity;

/**
 * Enumeración que define los roles disponibles para los usuarios en el sistema.
 * Los roles determinan los permisos y niveles de acceso de cada usuario.
 */
public enum Role {

    /** Rol de administrador, con permisos completos en el sistema. */
    ADMIN,

    /** Rol de usuario estándar, con permisos limitados. */
    HUESPED,
    
    /** Rol de anfitrión, con permisos limitados. */
    ANFITRION
}
