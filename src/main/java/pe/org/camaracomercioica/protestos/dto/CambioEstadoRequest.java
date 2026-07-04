package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;

@Schema(description = "Datos para realizar la transición de estado de una solicitud")
public record CambioEstadoRequest(
    @NotNull
    @Schema(description = "Nuevo estado de la solicitud", example = "EN_REVISION", requiredMode = Schema.RequiredMode.REQUIRED)
    EstadoSolicitud estado,
    
    @Size(max=1000)
    @Schema(description = "Observaciones o comentarios acerca del cambio de estado", example = "Documentación correcta, pasa a revisión")
    String observacion,
    
    @Schema(description = "ID del analista asignado", example = "1")
    Long analistaId,
    
    @NotNull
    @PositiveOrZero
    @Schema(description = "Versión actual del registro para control de concurrencia optimista", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    Long version
) {
    public CambioEstadoRequest(EstadoSolicitud estado, String observacion, Long analistaId) {
        this(estado, observacion, analistaId, 0L);
    }
}