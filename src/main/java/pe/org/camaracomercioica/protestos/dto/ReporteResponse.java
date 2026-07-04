package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Resumen del reporte estadístico de solicitudes")
public record ReporteResponse(
    @Schema(description = "Total de solicitudes en el rango reportado", example = "500")
    long total,
    
    @Schema(description = "Distribución del conteo de solicitudes por estado", example = "{\"APROBADA\":400, \"REGISTRADA\":50, \"EN_REVISION\":50}")
    Map<String, Long> porEstado
) {}
