package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Registro de un evento de auditoría")
public record AuditoriaResponse(
    @Schema(description = "ID único del registro de auditoría", example = "120")
    Long id,
    
    @Schema(description = "Email del usuario que realizó la acción", example = "admin@demo.local")
    String actor,
    
    @Schema(description = "Acción realizada (ej. CREAR, CAMBIAR_ESTADO)", example = "CREAR")
    String accion,
    
    @Schema(description = "Tipo de recurso afectado", example = "SOLICITUD")
    String recurso,
    
    @Schema(description = "Identificador único del recurso afectado (opcional)", example = "1")
    String recursoId,
    
    @Schema(description = "Detalle descriptivo de la acción", example = "Solicitud SOL-2026-A5B2 creada exitosamente")
    String detalle,
    
    @Schema(description = "Fecha y hora del evento", example = "2026-06-19T02:00:00Z")
    Instant fecha
) {}
