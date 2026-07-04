package pe.org.camaracomercioica.protestos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.org.camaracomercioica.protestos.model.SolicitudEvento;

import java.util.List;

public interface SolicitudEventoRepository extends JpaRepository<SolicitudEvento, Long> {
    List<SolicitudEvento> findBySolicitudIdOrderByCreadoEnAsc(Long solicitudId);
}
