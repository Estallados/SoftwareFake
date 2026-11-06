package co.edu.unbosque.marketplace.entity;

/**
 * Roles de los usuarios. Determinan a qué rutas puede entrar cada uno; Spring
 * los ve con el prefijo {@code ROLE_} (por ejemplo, {@code ROLE_ADMIN}).
 */
public enum Role {

	/** Administrador: modera usuarios, alojamientos y reseñas, y ve los reportes. */
	ADMIN,

	/** Huésped: busca alojamientos, reserva, paga y deja reseñas. */
	HUESPED,

	/** Anfitrión: publica alojamientos y administra sus reservas. */
	ANFITRION
}
