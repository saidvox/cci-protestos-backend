package pe.org.camaracomercioica.protestos.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.org.camaracomercioica.protestos.model.InvitacionAnalista;

import java.util.List;
import java.util.Optional;

public interface InvitacionAnalistaRepository extends JpaRepository<InvitacionAnalista, Long> {
    @EntityGraph(attributePaths = {"analista", "analista.usuario", "analista.usuario.entidad"})
    Optional<InvitacionAnalista> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitacion from InvitacionAnalista invitacion
            join fetch invitacion.analista analista
            join fetch analista.usuario usuario
            join fetch usuario.entidad
            where invitacion.tokenHash = :tokenHash
            """)
    Optional<InvitacionAnalista> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    List<InvitacionAnalista> findByAnalistaIdAndUsadoEnIsNullAndRevocadoEnIsNull(Long analistaId);
}
