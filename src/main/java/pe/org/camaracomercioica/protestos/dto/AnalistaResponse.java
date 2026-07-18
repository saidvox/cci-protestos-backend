package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalle del analista registrado")
public record AnalistaResponse(
        Long id,
        String codigo,
        String nombre,
        String email,
        boolean disponible,
        Long entidadId,
        String entidadNombre
) {
}
