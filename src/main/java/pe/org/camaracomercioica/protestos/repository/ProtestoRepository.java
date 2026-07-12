package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.org.camaracomercioica.protestos.model.EstadoProtesto;
import pe.org.camaracomercioica.protestos.model.Protesto;

import java.time.LocalDate;

public interface ProtestoRepository extends JpaRepository<Protesto, Long> {
    @Query(
            value = """
            select p from Protesto p
            join fetch p.deudor d
            join fetch p.entidad e
            where (:documento is null or lower(d.numeroDocumento) like concat('%', cast(:documento as string), '%'))
              and (:nombre is null or lower(d.nombreRazonSocial) like concat('%', cast(:nombre as string), '%'))
              and (:entidad is null or e.id = :entidad)
              and (:estado is null or p.estado = :estado)
              and (:desde is null or p.fechaProtesto >= :desde)
              and (:hasta is null or p.fechaProtesto <= :hasta)
            """,
            countQuery = """
            select count(p) from Protesto p
            join p.deudor d
            join p.entidad e
            where (:documento is null or lower(d.numeroDocumento) like concat('%', cast(:documento as string), '%'))
              and (:nombre is null or lower(d.nombreRazonSocial) like concat('%', cast(:nombre as string), '%'))
              and (:entidad is null or e.id = :entidad)
              and (:estado is null or p.estado = :estado)
              and (:desde is null or p.fechaProtesto >= :desde)
              and (:hasta is null or p.fechaProtesto <= :hasta)
            """
    )
    Page<Protesto> consultar(
            @Param("documento") String documento,
            @Param("nombre") String nombre,
            @Param("entidad") Long entidad,
            @Param("estado") EstadoProtesto estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable
    );

    boolean existsByDeudorIdAndEntidadIdAndEstado(Long deudorId, Long entidadId, EstadoProtesto estado);

    java.util.List<Protesto> findByDeudorIdAndEntidadIdAndEstado(Long deudorId, Long entidadId, EstadoProtesto estado);
}
