package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.org.camaracomercioica.protestos.model.TipoDocumento;
import pe.org.camaracomercioica.protestos.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDeudorTipoDocumentoAndDeudorNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
