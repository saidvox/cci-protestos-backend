package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;
import pe.org.camaracomercioica.protestos.service.SolicitudService;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
@Tag(name = "Solicitudes", description = "API para gestionar solicitudes de trámite y regularización de protestos")
public class SolicitudController {

    private final SolicitudService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear solicitud", description = "Crea una nueva solicitud asociada al usuario de la entidad financiera autenticada.")
    @ApiResponse(responseCode = "201", description = "Solicitud creada con éxito")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    public SolicitudResponse crear(@Valid @RequestBody SolicitudRequest r, Authentication a) {
        return service.crear(r, a.getName());
    }

    @GetMapping
    @Operation(summary = "Listar todas las solicitudes", description = "Obtiene una lista paginada de solicitudes con filtros opcionales de estado y entidad. Exclusivo para administradores y analistas.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    public PageResponse<SolicitudResponse> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Long entidadId,
            Authentication a,
            @ParameterObject @PageableDefault(size = 20, sort = "creadoEn", direction = Sort.Direction.DESC) Pageable p
    ) {
        return PageResponse.from(service.listar(estado, entidadId, p, a.getName()));
    }

    @GetMapping("/mis-solicitudes")
    @Operation(summary = "Listar mis solicitudes", description = "Obtiene una lista paginada de las solicitudes creadas por la entidad autenticada.")
    @ApiResponse(responseCode = "200", description = "Listado de solicitudes propias obtenido con éxito")
    public PageResponse<SolicitudResponse> mias(
            Authentication a,
            @ParameterObject @PageableDefault(size = 20) Pageable p
    ) {
        return PageResponse.from(service.mias(a.getName(), p));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de solicitud", description = "Obtiene una solicitud por ID respetando los permisos del usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Solicitud encontrada")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    public SolicitudResponse obtener(@PathVariable Long id, Authentication a) {
        return service.obtener(id, a.getName());
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de solicitud", description = "Permite a un analista o administrador cambiar el estado de una solicitud (Aprobado, Rechazado, Observado).")
    @ApiResponse(responseCode = "200", description = "Estado cambiado con éxito")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    public SolicitudResponse estado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest r,
            Authentication a
    ) {
        return service.cambiarEstado(id, r, a.getName());
    }

    @GetMapping("/deudor/{numeroDocumento}")
    @Operation(summary = "Obtener historial de solicitudes del deudor", description = "Retorna la lista de todas las solicitudes previas del deudor.")
    @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente")
    public java.util.List<SolicitudResponse> historialDeudor(@PathVariable String numeroDocumento) {
        return service.historialDeudor(numeroDocumento);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar/Corregir solicitud observada", description = "Permite al deudor corregir los datos de una solicitud observada y volver a enviarla.")
    @ApiResponse(responseCode = "200", description = "Solicitud corregida y reenviada con éxito")
    public SolicitudResponse actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudRequest r, Authentication a) {
        return service.actualizar(id, r, a.getName());
    }
}
