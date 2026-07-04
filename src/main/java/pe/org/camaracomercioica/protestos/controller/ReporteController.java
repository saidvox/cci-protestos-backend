package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.ReporteResponse;
import pe.org.camaracomercioica.protestos.model.*;
import pe.org.camaracomercioica.protestos.repository.SolicitudRepository;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "API para la generación de reportes y estadísticas operativas")
public class ReporteController {

    private final SolicitudRepository repository;

    @GetMapping("/solicitudes")
    @Operation(summary = "Obtener reporte consolidado de solicitudes", description = "Genera un reporte agrupado por estados, opcionalmente filtrado por un rango de fechas. Exclusivo para analistas y administradores.")
    @ApiResponse(responseCode = "200", description = "Reporte generado exitosamente")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ReporteResponse reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        if (desde == null && hasta == null) {
            var data = new LinkedHashMap<String, Long>();
            for (var e : EstadoSolicitud.values()) {
                data.put(e.name(), repository.countByEstado(e));
            }
            return new ReporteResponse(repository.count(), data);
        }
        var start = (desde == null ? LocalDate.of(1970, 1, 1) : desde).atStartOfDay(ZoneOffset.UTC).toInstant();
        var end = (hasta == null ? LocalDate.of(9999, 12, 31) : hasta).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
        var rows = repository.findByCreadoEnBetween(start, end);
        var grouped = rows.stream().collect(Collectors.groupingBy(s -> s.getEstado().name(), LinkedHashMap::new, Collectors.counting()));
        for (var e : EstadoSolicitud.values()) {
            grouped.putIfAbsent(e.name(), 0L);
        }
        return new ReporteResponse(rows.size(), grouped);
    }
}

