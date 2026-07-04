package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.DashboardResponse;
import pe.org.camaracomercioica.protestos.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API para la obtención de métricas y estadísticas de uso del sistema")
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/resumen")
    @Operation(summary = "Obtener resumen del dashboard", description = "Retorna métricas, totales y logs de actividad recientes según el rol del usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Resumen del dashboard cargado exitosamente")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    public DashboardResponse resumen(Authentication auth) {
        return service.resumen(auth.getName());
    }
}