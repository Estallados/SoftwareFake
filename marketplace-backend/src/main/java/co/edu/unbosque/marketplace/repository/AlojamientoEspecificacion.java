package co.edu.unbosque.marketplace.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoResena;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.PeriodoDisponibilidad;
import co.edu.unbosque.marketplace.entity.Resena;
import co.edu.unbosque.marketplace.entity.Reserva;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Filtros de la búsqueda pública de alojamientos. Cada método devuelve una
 * condición; los que reciben null no filtran.
 *
 * @see "RF-024, CA-087 a CA-092, CA-190"
 */
public final class AlojamientoEspecificacion {

	private static final String ID_ALOJAMIENTO = "idAlojamiento";

	private AlojamientoEspecificacion() {
		throw new IllegalStateException("Clase utilitaria");
	}

	/** Solo publicaciones en el estado indicado (la búsqueda usa ACTIVA). */
	public static Specification<Alojamiento> conEstado(EstadoAlojamiento estado) {
		return (root, query, cb) -> cb.equal(root.get("estado"), estado);
	}

	public static Specification<Alojamiento> enCiudad(String ciudad) {
		return (root, query, cb) -> ciudad == null || ciudad.isBlank() ? null
				: cb.equal(cb.lower(root.get("ciudad")), ciudad.trim().toLowerCase());
	}

	public static Specification<Alojamiento> deTipo(String tipo) {
		return (root, query, cb) -> tipo == null || tipo.isBlank() ? null
				: cb.equal(cb.lower(root.get("tipo")), tipo.trim().toLowerCase());
	}

	public static Specification<Alojamiento> conCapacidadPara(Integer huespedes) {
		return (root, query, cb) -> huespedes == null ? null
				: cb.greaterThanOrEqualTo(root.get("capacidad"), huespedes);
	}

	public static Specification<Alojamiento> precioDesde(BigDecimal minimo) {
		return (root, query, cb) -> minimo == null ? null : cb.greaterThanOrEqualTo(root.get("precioNoche"), minimo);
	}

	public static Specification<Alojamiento> precioHasta(BigDecimal maximo) {
		return (root, query, cb) -> maximo == null ? null : cb.lessThanOrEqualTo(root.get("precioNoche"), maximo);
	}

	/** El alojamiento debe tener todas las amenidades indicadas. */
	public static Specification<Alojamiento> conAmenidades(Collection<String> amenidades) {
		return (root, query, cb) -> {
			if (amenidades == null || amenidades.isEmpty()) {
				return null;
			}
			return cb.and(amenidades.stream().filter(a -> a != null && !a.isBlank())
					.map(a -> cb.isMember(a.trim().toLowerCase(), root.<Collection<String>>get("amenidades")))
					.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	/**
	 * Con fechas para reservar: tiene un período ofrecido con al menos una noche
	 * desde {@code desde}. Así el catálogo no muestra publicaciones activas sin
	 * calendario o con todos sus períodos vencidos.
	 */
	public static Specification<Alojamiento> conFechasDesde(LocalDate desde) {
		return (root, query, cb) -> {
			Subquery<Long> periodo = query.subquery(Long.class);
			Root<PeriodoDisponibilidad> p = periodo.from(PeriodoDisponibilidad.class);
			periodo.select(p.get("idPeriodo")).where(cb.equal(p.get(ID_ALOJAMIENTO), root.get(ID_ALOJAMIENTO)),
					cb.greaterThan(p.get("fechaFin"), desde));
			return cb.exists(periodo);
		};
	}

	/**
	 * Ordena por las reseñas publicadas: por cantidad y luego por promedio
	 * ({@code primeroCantidad}, los más recomendados) o por promedio y luego por
	 * cantidad (los mejor calificados). Los empates quedan del más reciente al más
	 * antiguo. No filtra nada; tampoco ordena la consulta que cuenta el total de la
	 * paginación.
	 */
	public static Specification<Alojamiento> ordenadoPorResenas(boolean primeroCantidad) {
		return (root, query, cb) -> {
			if (Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType())) {
				return null;
			}
			Subquery<Double> promedio = query.subquery(Double.class);
			Root<Resena> r = promedio.from(Resena.class);
			promedio.select(cb.coalesce(cb.avg(r.get("calificacion")), 0.0)).where(
					cb.equal(r.get(ID_ALOJAMIENTO), root.get(ID_ALOJAMIENTO)),
					cb.equal(r.get("estado"), EstadoResena.PUBLICADA));

			Subquery<Long> cantidad = query.subquery(Long.class);
			Root<Resena> c = cantidad.from(Resena.class);
			cantidad.select(cb.count(c)).where(cb.equal(c.get(ID_ALOJAMIENTO), root.get(ID_ALOJAMIENTO)),
					cb.equal(c.get("estado"), EstadoResena.PUBLICADA));

			Order porPromedio = cb.desc(promedio);
			Order porCantidad = cb.desc(cantidad);
			query.orderBy(primeroCantidad ? porCantidad : porPromedio, primeroCantidad ? porPromedio : porCantidad,
					cb.desc(root.get("fechaCreacion")), cb.desc(root.get(ID_ALOJAMIENTO)));
			return null;
		};
	}

	/**
	 * Libre en [llegada, salida): existe un período ofrecido que contiene las
	 * fechas y no hay reservas activas que se crucen.
	 */
	public static Specification<Alojamiento> libreEntre(LocalDate llegada, LocalDate salida,
			List<EstadoReserva> estadosActivos, LocalDateTime limitePendiente) {
		return (root, query, cb) -> {
			if (llegada == null || salida == null) {
				return null;
			}
			Subquery<Long> periodo = query.subquery(Long.class);
			Root<PeriodoDisponibilidad> p = periodo.from(PeriodoDisponibilidad.class);
			periodo.select(p.get("idPeriodo")).where(cb.equal(p.get(ID_ALOJAMIENTO), root.get(ID_ALOJAMIENTO)),
					cb.lessThanOrEqualTo(p.get("fechaInicio"), llegada),
					cb.greaterThanOrEqualTo(p.get("fechaFin"), salida));

			Subquery<Long> cruce = query.subquery(Long.class);
			Root<Reserva> r = cruce.from(Reserva.class);
			cruce.select(r.get("idReserva")).where(cb.equal(r.get(ID_ALOJAMIENTO), root.get(ID_ALOJAMIENTO)),
					cb.lessThan(r.get("fechaEntrada"), salida), cb.greaterThan(r.get("fechaSalida"), llegada),
					cb.or(r.get("estado").in(estadosActivos),
							cb.and(cb.equal(r.get("estado"), EstadoReserva.PENDIENTE_PAGO),
									cb.greaterThanOrEqualTo(r.get("fechaCreacion"), limitePendiente))));

			return cb.and(cb.exists(periodo), cb.not(cb.exists(cruce)));
		};
	}
}
