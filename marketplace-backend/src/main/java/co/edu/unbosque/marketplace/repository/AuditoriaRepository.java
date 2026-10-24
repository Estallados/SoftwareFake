package co.edu.unbosque.marketplace.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import co.edu.unbosque.marketplace.entity.EventoAuditoria;

/**
 * Repositorio de solo inserción para {@link EventoAuditoria}.
 * <p>
 * Extiende {@link Repository} (no {@code JpaRepository}) para exponer
 * únicamente guardar y consultar: no hay métodos de actualización ni de
 * borrado.
 * </p>
 *
 * @see "RNF-023, RNF-026"
 */
public interface AuditoriaRepository extends Repository<EventoAuditoria, Long> {

    /**
     * Inserta un evento de auditoría.
     *
     * @param evento Evento nuevo (sin id).
     * @return El evento guardado con su id.
     */
    public EventoAuditoria save(EventoAuditoria evento);

    /**
     * Consulta los eventos de forma paginada.
     *
     * @param pageable Página, tamaño y orden.
     * @return Página de eventos.
     */
    public Page<EventoAuditoria> findAll(Pageable pageable);

    /**
     * Consulta los eventos de una acción, de forma paginada.
     *
     * @param accion   Acción auditada, por ejemplo LOGIN.
     * @param pageable Página, tamaño y orden.
     * @return Página de eventos de esa acción.
     */
    public Page<EventoAuditoria> findByAccion(String accion, Pageable pageable);

    /**
     * Consulta paginada con filtros opcionales (null = sin filtro).
     *
     * @see "RF-015, CA-062"
     */
    @Query("select e from EventoAuditoria e where (:accion is null or e.accion = :accion) "
            + "and (:idActor is null or e.idUsuario = :idActor) "
            + "and (cast(:desde as timestamp) is null or e.fecha >= :desde) "
            + "and (cast(:hasta as timestamp) is null or e.fecha <= :hasta)")
    public Page<EventoAuditoria> buscar(@Param("accion") String accion, @Param("idActor") Long idActor,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, Pageable pageable);

    /**
     * Detalle de un evento.
     *
     * @see "RF-015, CA-064"
     */
    public Optional<EventoAuditoria> findById(Long idEvento);
}
