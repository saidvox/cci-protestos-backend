package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.org.camaracomercioica.protestos.model.Auditoria;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    @Query("select a from Auditoria a where (:actor is null or lower(a.actor) like lower(concat('%',:actor,'%'))) and (:accion is null or a.accion=:accion) and (:recurso is null or a.recurso=:recurso) and (:desde is null or a.fecha>=:desde) and (:hasta is null or a.fecha<=:hasta)")
    Page<Auditoria> filtrar(
            @Param("actor") String actor,
            @Param("accion") String accion,
            @Param("recurso") String recurso,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            Pageable pageable
    );

    Page<Auditoria> findByRecursoIn(Collection<String> recursos, Pageable pageable);

    long countByRecursoInAndIdGreaterThan(Collection<String> recursos, long id);

    Optional<Auditoria> findTopByRecursoInOrderByIdDesc(Collection<String> recursos);
}
