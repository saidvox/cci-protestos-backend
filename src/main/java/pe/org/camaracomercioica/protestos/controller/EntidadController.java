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
@RequestMapping("/api/entidades")
@RequiredArgsConstructor
@Tag(name = "Entidades", description = "API para la gestión de entidades financieras afiliadas")
public class EntidadController {

    private final CatalogoService service;

    @GetMapping
    @Operation(summary = "Listar entidades financieras", description = "Retorna la lista de todas las entidades financieras registradas en el sistema.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    public List<EntidadResponse> listar() {
        return service.entidades();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear entidad financiera", description = "Registra una nueva entidad financiera. Exclusivo para administradores.")
    @ApiResponse(responseCode = "201", description = "Entidad creada con éxito")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    public EntidadResponse crear(@Valid @RequestBody EntidadRequest r) {
        return service.crear(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar entidad financiera", description = "Actualiza los datos de una entidad financiera. Exclusivo para administradores.")
    @ApiResponse(responseCode = "200", description = "Entidad actualizada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    @ApiResponse(responseCode = "404", description = "Entidad no encontrada")
    public EntidadResponse actualizar(@PathVariable Long id, @Valid @RequestBody UpdateEntidadRequest r) {
        return service.actualizar(id, r);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de entidad", description = "Habilita o deshabilita una entidad financiera sin modificar sus datos generales.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    @ApiResponse(responseCode = "404", description = "Entidad no encontrada")
    public EntidadResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoEntidadRequest r) {
        return service.cambiarEstadoEntidad(id, r);
    }
}
