package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;
import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;

@Schema(description = "Resumen estadístico y actividad reciente del sistema")
public record DashboardResponse(
    @Schema(description = "Total general de solicitudes", example = "150")
    long total,
    
    @Schema(description = "Total de solicitudes pendientes de revisión", example = "12")
    long pendientes,
    
    @Schema(description = "Total de solicitudes aprobadas", example = "120")
    long aprobadas,
    
    @Schema(description = "Total de entidades financieras activas registradas", example = "5")
    long entidadesActivas,
    
    @Schema(description = "Desglose del conteo de solicitudes por cada estado", example = "{\"REGISTRADA\":10, \"EN_REVISION\":2, \"APROBADA\":120}")
    Map<EstadoSolicitud, Long> porEstado,
    
    @Schema(description = "Lista de las solicitudes más recientes registradas en el sistema")
    List<Reciente> solicitudesRecientes
) {
    @Schema(description = "Información básica simplificada de una solicitud reciente")
    public record Reciente(
        @Schema(description = "ID único de la solicitud", example = "1")
        Long id,
        
        @Schema(description = "Código de la solicitud", example = "SOL-2026-A5B2")
        String codigo,
        
        @Schema(description = "Razón social de la entidad financiera que la registró", example = "Financiera Demo Ica")
        String entidad,
        
        @Schema(description = "Estado actual de la solicitud", example = "REGISTRADA")
        EstadoSolicitud estado,
        
        @Schema(description = "Fecha de creación del registro", example = "2026-06-19T02:00:00Z")
        Instant creadoEn
    ) {}
}