package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
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
    public AnalistaInvitationResponse crear(@Valid @RequestBody AnalistaRequest r, Authentication authentication) {
        return service.crear(r, authentication.getName());
    }

    @PostMapping("/{id}/invitacion")
    @Operation(summary = "Regenerar invitacion", description = "Revoca invitaciones anteriores y genera un nuevo token de activacion de un solo uso.")
    public AnalistaInvitationResponse regenerarInvitacion(@PathVariable Long id, Authentication authentication) {
        var invitation = service.regenerarInvitacion(id, authentication.getName());
        return invitation;
    }

    @PostMapping("/{id}/reactivacion")
    @Operation(
            summary = "Reiniciar activacion",
            description = "Invalida el acceso actual y genera una nueva invitacion de activacion de un solo uso."
    )
    public AnalistaInvitationResponse reiniciarActivacion(@PathVariable Long id, Authentication authentication) {
        return service.reiniciarActivacion(id, authentication.getName());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar analista", description = "Actualiza los datos de un analista existente. Exclusivo para administradores.")
    @ApiResponse(responseCode = "200", description = "Analista actualizado correctamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    @ApiResponse(responseCode = "404", description = "Analista no encontrado")
    public AnalistaResponse actualizar(@PathVariable Long id, @Valid @RequestBody UpdateAnalistaRequest r, Authentication authentication) {
        return service.actualizar(id, r, authentication.getName());
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar disponibilidad de analista", description = "Habilita o deshabilita un analista bancario sin modificar sus datos generales.")
    @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol de administrador)")
    @ApiResponse(responseCode = "404", description = "Analista no encontrado")
    public AnalistaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoAnalistaRequest r, Authentication authentication) {
        return service.cambiarEstadoAnalista(id, r, authentication.getName());
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablecer contrasena", description = "Asigna una nueva contrasena e invalida las sesiones anteriores del analista.")
    public void restablecerPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordAnalistaRequest request,
            Authentication authentication
    ) {
        service.restablecerPasswordAnalista(id, request, authentication.getName());
    }
}
