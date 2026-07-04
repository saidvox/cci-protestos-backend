package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.DashboardResponse;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.*;
import pe.org.camaracomercioica.protestos.repository.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final EntidadFinancieraRepository entidades;

    @Transactional(readOnly = true)
    public DashboardResponse resumen(String email) {
        var u = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        Long entidadId = "BANK_ANALYST".equals(u.getRol().getNombre())
                && u.getEntidad() != null ? u.getEntidad().getId() : null;

        var counts = new EnumMap<EstadoSolicitud, Long>(EstadoSolicitud.class);
        long total = 0;
        for (var e : EstadoSolicitud.values()) {
            long n = entidadId == null 
                    ? solicitudes.countByEstado(e) 
                    : solicitudes.countByEstadoAndEntidadId(e, entidadId);
            counts.put(e, n);
            total += n;
        }

        var page = entidadId == null
                ? solicitudes.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "creadoEn")))
                : solicitudes.findByEntidadId(entidadId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "creadoEn")));

        var recientes = page.stream().map(s -> new DashboardResponse.Reciente(
                s.getId(),
                s.getCodigo(),
                s.getEntidad().getRazonSocial(),
                s.getEstado(),
                s.getCreadoEn()
        )).toList();

        // Pendientes incluye todos los estados previos a aprobación o rechazo final.
        long pendientes = (counts.getOrDefault(EstadoSolicitud.REGISTRADA, 0L))
                + (counts.getOrDefault(EstadoSolicitud.EN_REVISION_CCI, 0L))
                + (counts.getOrDefault(EstadoSolicitud.OBSERVADA_CCI, 0L))
                + (counts.getOrDefault(EstadoSolicitud.DERIVADA_ENTIDAD, 0L))
                + (counts.getOrDefault(EstadoSolicitud.EN_REVISION_ANALISTA, 0L))
                + (counts.getOrDefault(EstadoSolicitud.OBSERVADA_ENTIDAD, 0L));

        long aprobadas = counts.getOrDefault(EstadoSolicitud.APROBADA_ENTIDAD, 0L);

        return new DashboardResponse(
                total,
                pendientes,
                aprobadas,
                entidades.countByActivoTrue(),
                counts,
                recientes
        );
    }
}
