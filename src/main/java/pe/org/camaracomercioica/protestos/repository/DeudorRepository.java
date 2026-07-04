package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.org.camaracomercioica.protestos.model.Deudor;
import pe.org.camaracomercioica.protestos.model.TipoDocumento;

import java.util.Optional;

public interface DeudorRepository extends JpaRepository<Deudor, Long> {
    Optional<Deudor> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
