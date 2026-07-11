package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Cambio rápido de disponibilidad de un analista bancario")
public record CambioEstadoAnalistaRequest(
        @NotNull
        @Schema(description = "Nueva disponibilidad del analista", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean disponible
) {}
