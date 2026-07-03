package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.ProtestoResponse;
import pe.org.camaracomercioica.protestos.repository.ProtestoRepository;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/protestos")
@RequiredArgsConstructor
@Tag(name = "Protestos", description = "API para la consulta de títulos protestados y moras")
public class ProtestoController {

    private final ProtestoRepository repository;

    @GetMapping("/consulta")
    @Operation(summary = "Consultar protestos", description = "Busca protestos y moras registrados en base a filtros como documento del deudor, nombre, entidad financiera de origen y rango de fechas.")
    @ApiResponse(responseCode = "200", description = "Consulta exitosa, retorna la lista de coincidencias")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    public List<ProtestoResponse> consultar(
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long entidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return repository.consultar(blank(documento), blank(nombre), entidad, desde, hasta).stream()
                .map(p -> new ProtestoResponse(
                        p.getId(),
                        p.getNumeroDocumento(),
                        p.getNombreDeudor(),
                        p.getEntidad().getRazonSocial(),
                        p.getTipoTitulo(),
                        p.getMonto(),
                        p.getMoneda(),
                        p.getFechaProtesto(),
                        p.isVigente()
                ))
                .toList();
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

