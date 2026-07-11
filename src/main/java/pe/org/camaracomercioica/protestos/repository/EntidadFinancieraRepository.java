package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.org.camaracomercioica.protestos.model.EntidadFinanciera;

import java.util.Optional;

public interface EntidadFinancieraRepository extends JpaRepository<EntidadFinanciera, Long> {
    boolean existsByRuc(String ruc);

    Optional<EntidadFinanciera> findByRuc(String ruc);

    long countByActivoTrue();
}
