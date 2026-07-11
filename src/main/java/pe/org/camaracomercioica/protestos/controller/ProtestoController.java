package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.PageResponse;
import pe.org.camaracomercioica.protestos.dto.ProtestoResponse;
import pe.org.camaracomercioica.protestos.model.EstadoProtesto;
import pe.org.camaracomercioica.protestos.model.Protesto;
import pe.org.camaracomercioica.protestos.repository.ProtestoRepository;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/protestos")
@RequiredArgsConstructor
@Tag(name = "Protestos", description = "API para la consulta de títulos protestados y moras")
public class ProtestoController {

    private final ProtestoRepository repository;

    @GetMapping("/consulta")
    @Operation(summary = "Consultar protestos", description = "Busca protestos y moras registrados en base a filtros como documento del deudor, nombre, entidad financiera de origen y rango de fechas.")
    @ApiResponse(responseCode = "200", description = "Consulta exitosa, retorna una pagina de coincidencias")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    public PageResponse<ProtestoResponse> consultar(
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long entidad,
            @RequestParam(required = false) EstadoProtesto estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @ParameterObject @PageableDefault(size = 10, sort = "fechaProtesto", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return PageResponse.from(repository
                .consultar(blankLower(documento), blankLower(nombre), entidad, estado, desde, hasta, pageable)
                .map(this::map));
    }

    private String blankLower(String s) {
        return s == null || s.isBlank() ? null : s.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private ProtestoResponse map(Protesto p) {
        return new ProtestoResponse(
                p.getId(),
                p.getNumeroDocumento(),
                p.getNombreDeudor(),
                p.getEntidad().getId(),
                p.getEntidad().getRazonSocial(),
                p.getTipoTitulo(),
                p.getMonto(),
                p.getMoneda(),
                p.getFechaProtesto(),
                p.isVigente()
        );
    }
}
