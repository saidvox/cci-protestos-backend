package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.service.AuditoriaService;
import java.time.Instant;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "API para la visualización de registros y logs de auditoría del sistema")
public class AuditoriaController {

    private final AuditoriaService service;

    @GetMapping
    @Operation(summary = "Listar logs de auditoría", description = "Retorna una lista paginada de actividades realizadas por los usuarios en el sistema. Exclusivo para administradores.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    public PageResponse<AuditoriaResponse> listar(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String recurso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @ParameterObject @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable p
    ) {
        return PageResponse.from(service.listar(actor, accion, recurso, desde, hasta, p));
    }
}

