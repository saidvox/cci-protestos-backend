package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.org.camaracomercioica.protestos.model.DocumentoTramite;

import java.util.List;
import java.util.Optional;

public interface DocumentoTramiteRepository extends JpaRepository<DocumentoTramite, Long> {
    List<DocumentoTramite> findByActivoTrueOrderByOrdenAscCreadoEnAsc();

    List<DocumentoTramite> findAllByOrderByActivoDescOrdenAscCreadoEnAsc();

    Optional<DocumentoTramite> findByTituloIgnoreCase(String titulo);
}
