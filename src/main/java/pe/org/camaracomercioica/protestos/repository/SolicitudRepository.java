package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;
import pe.org.camaracomercioica.protestos.model.Solicitud;

import java.time.Instant;
import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    @Query(
            value = """
            select s from Solicitud s
            join fetch s.solicitante solicitante
            join fetch s.deudor deudor
            join fetch s.entidad entidad
            left join fetch s.analista analista
            left join fetch analista.usuario analistaUsuario
            where (:estado is null or s.estado = :estado)
              and (:entidadId is null or entidad.id = :entidadId)
            """,
            countQuery = """
            select count(s) from Solicitud s
            join s.entidad entidad
            where (:estado is null or s.estado = :estado)
              and (:entidadId is null or entidad.id = :entidadId)
            """
    )
    Page<Solicitud> filtrar(
            @Param("estado") EstadoSolicitud estado,
            @Param("entidadId") Long entidadId,
            Pageable pageable
    );

    @Query(
            value = """
            select s from Solicitud s
            join fetch s.solicitante solicitante
            join fetch s.deudor deudor
            join fetch s.entidad entidad
            left join fetch s.analista analista
            left join fetch analista.usuario analistaUsuario
            where lower(solicitante.email) = lower(:email)
            """,
            countQuery = """
            select count(s) from Solicitud s
            join s.solicitante solicitante
            where lower(solicitante.email) = lower(:email)
            """
    )
    Page<Solicitud> findBySolicitanteEmailIgnoreCase(@Param("email") String email, Pageable pageable);

    @Query(
            value = """
            select s from Solicitud s
            join fetch s.solicitante solicitante
            join fetch s.deudor deudor
            join fetch s.entidad entidad
            left join fetch s.analista analista
            left join fetch analista.usuario analistaUsuario
            where entidad.id = :entidadId
            """,
            countQuery = """
            select count(s) from Solicitud s
            join s.entidad entidad
            where entidad.id = :entidadId
            """
    )
    Page<Solicitud> findByEntidadId(@Param("entidadId") Long entidadId, Pageable pageable);

    long countByEstado(EstadoSolicitud estado);

    long countByEstadoAndEntidadId(EstadoSolicitud estado, Long entidadId);

    List<Solicitud> findByCreadoEnBetween(Instant desde, Instant hasta);
}
