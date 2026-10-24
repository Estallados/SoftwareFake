package co.edu.unbosque.marketplace.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import co.edu.unbosque.marketplace.entity.Alojamiento;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoReserva;
import co.edu.unbosque.marketplace.entity.PeriodoDisponibilidad;
import co.edu.unbosque.marketplace.entity.Reserva;
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
            periodo.select(p.get("idPeriodo")).where(
                    cb.equal(p.get(ID_ALOJAMIENTO), root.get(ID_ALOJAMIENTO)),
                    cb.lessThanOrEqualTo(p.get("fechaInicio"), llegada),
                    cb.greaterThanOrEqualTo(p.get("fechaFin"), salida));

            Subquery<Long> cruce = query.subquery(Long.class);
            Root<Reserva> r = cruce.from(Reserva.class);
            cruce.select(r.get("idReserva")).where(
                    cb.equal(r.get(ID_ALOJAMIENTO), root.get(ID_ALOJAMIENTO)),
                    cb.lessThan(r.get("fechaEntrada"), salida),
                    cb.greaterThan(r.get("fechaSalida"), llegada),
                    cb.or(r.get("estado").in(estadosActivos),
                            cb.and(cb.equal(r.get("estado"), EstadoReserva.PENDIENTE_PAGO),
                                    cb.greaterThanOrEqualTo(r.get("fechaCreacion"), limitePendiente))));

            return cb.and(cb.exists(periodo), cb.not(cb.exists(cruce)));
        };
    }
}
