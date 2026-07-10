package pe.org.camaracomercioica.protestos.util;

import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;
import java.util.*;

public final class SolicitudStatePolicy {

    private SolicitudStatePolicy() {}

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> ALLOWED = Map.of(
        EstadoSolicitud.REGISTRADA, Set.of(EstadoSolicitud.EN_REVISION_CCI, EstadoSolicitud.OBSERVADA_CCI, EstadoSolicitud.DERIVADA_ENTIDAD, EstadoSolicitud.RECHAZADA),
        EstadoSolicitud.EN_REVISION_CCI, Set.of(EstadoSolicitud.OBSERVADA_CCI, EstadoSolicitud.DERIVADA_ENTIDAD, EstadoSolicitud.RECHAZADA),
        EstadoSolicitud.OBSERVADA_CCI, Set.of(EstadoSolicitud.EN_REVISION_CCI, EstadoSolicitud.RECHAZADA),
        EstadoSolicitud.DERIVADA_ENTIDAD, Set.of(EstadoSolicitud.EN_REVISION_ANALISTA, EstadoSolicitud.RECHAZADA),
        EstadoSolicitud.EN_REVISION_ANALISTA, Set.of(EstadoSolicitud.OBSERVADA_ENTIDAD, EstadoSolicitud.APROBADA_ENTIDAD, EstadoSolicitud.RECHAZADA),
        EstadoSolicitud.OBSERVADA_ENTIDAD, Set.of(EstadoSolicitud.EN_REVISION_ANALISTA, EstadoSolicitud.RECHAZADA),
        EstadoSolicitud.APROBADA_ENTIDAD, Set.of(EstadoSolicitud.FINALIZADA, EstadoSolicitud.LEVANTAMIENTO_PROCESADO),
        EstadoSolicitud.FINALIZADA, Set.of(EstadoSolicitud.LEVANTAMIENTO_PROCESADO),
        EstadoSolicitud.RECHAZADA, Set.of(),
        EstadoSolicitud.LEVANTAMIENTO_PROCESADO, Set.of()
    );

    public static boolean canTransition(EstadoSolicitud from, EstadoSolicitud to) {
        return from == to || ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
