package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.org.camaracomercioica.protestos.model.Protesto;

import java.time.LocalDate;
import java.util.List;

public interface ProtestoRepository extends JpaRepository<Protesto, Long> {
    @Query("""
            select p from Protesto p
            join fetch p.deudor d
            join fetch p.entidad e
            where (:documento is null or lower(d.numeroDocumento) like lower(concat('%', :documento, '%')))
              and (:nombre is null or lower(d.nombreRazonSocial) like lower(concat('%', :nombre, '%')))
              and (:entidad is null or e.id = :entidad)
              and (:desde is null or p.fechaProtesto >= :desde)
              and (:hasta is null or p.fechaProtesto <= :hasta)
            """)
    List<Protesto> consultar(
            @Param("documento") String documento,
            @Param("nombre") String nombre,
            @Param("entidad") Long entidad,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );
}
