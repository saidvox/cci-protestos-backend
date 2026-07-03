package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.org.camaracomercioica.protestos.model.*;
import java.time.Instant;
import java.math.BigDecimal;

@Schema(description = "Detalle completo de una solicitud de trámite")
public record SolicitudResponse(
    @Schema(description = "ID único de la solicitud", example = "1")
    Long id,
    
    @Schema(description = "Código autogenerado de la solicitud", example = "SOL-2026-A5B2")
    String codigo,
    
    @Schema(description = "Nombre completo del usuario que registró la solicitud", example = "Entidad Demo")
    String solicitante,
    
    @Schema(description = "Razón social de la entidad financiera asociada", example = "Financiera Demo Ica")
    String entidad,
    
    @Schema(description = "Nombre completo del analista asignado (si lo hay)", example = "Analista Demo")
    String analista,
    
    @Schema(description = "Estado actual de la solicitud", example = "REGISTRADA")
    EstadoSolicitud estado,
    
    @Schema(description = "Tipo de trámite solicitado", example = "REGISTRO_PROTESTO")
    TipoTramite tipoTramite,
    
    @Schema(description = "Número de documento del deudor", example = "20999999991")
    String numeroDocumentoDeudor,
    
    @Schema(description = "Monto del protesto o trámite", example = "1500.50")
    BigDecimal monto,
    
    @Schema(description = "Moneda del monto ('PEN' o 'USD')", example = "PEN")
    String moneda,
    
    @Schema(description = "Motivo o descripción de la solicitud", example = "Documento impago tras fecha de vencimiento")
    String motivo,
    
    @Schema(description = "Observaciones provistas por el analista durante la revisión", example = "Falta adjuntar copia de la letra física")
    String observacion,
    
    @Schema(description = "Versión del registro para control de concurrencia optimista", example = "0")
    Long version,
    
    @Schema(description = "Fecha de creación del registro", example = "2026-06-19T02:00:00Z")
    Instant creadoEn,
    
    @Schema(description = "Fecha de la última actualización del registro", example = "2026-06-19T02:05:00Z")
    Instant actualizadoEn
) {}