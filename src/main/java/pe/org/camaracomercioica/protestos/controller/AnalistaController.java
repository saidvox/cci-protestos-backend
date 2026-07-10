package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.service.CatalogoService;
import java.util.List;

@RestController
@RequestMapping("/api/analistas")
@RequiredArgsConstructor
@Tag(name = "Analistas", description = "API para la gestión de analistas y administradores")
public class AnalistaController {

    private final CatalogoService service;

    @GetMapping
    @Operation(summary = "Listar analistas", description = "Retorna la lista de todos los analistas registrados. Exclusivo para administradores.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    public List<AnalistaResponse> listar() {
        return service.analistas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear analista", description = "Registra un nuevo analista de la Cámara de Comercio. Exclusivo para administradores.")
    @ApiResponse(responseCode = "201", description = "Analista creado con éxito")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    public AnalistaResponse crear(@Valid @RequestBody AnalistaRequest r) {
        return service.crear(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar analista", description = "Actualiza los datos de un analista existente. Exclusivo para administradores.")
    @ApiResponse(responseCode = "200", description = "Analista actualizado correctamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    @ApiResponse(responseCode = "404", description = "Analista no encontrado")
    public AnalistaResponse actualizar(@PathVariable Long id, @Valid @RequestBody UpdateAnalistaRequest r) {
        return service.actualizar(id, r);
    }
}

