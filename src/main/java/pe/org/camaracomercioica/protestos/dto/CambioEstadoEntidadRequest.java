package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Cambio rápido de estado de una entidad financiera")
public record CambioEstadoEntidadRequest(
        @NotNull
        @Schema(description = "Nuevo estado activo de la entidad", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean activo
) {}
