package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import pe.org.camaracomercioica.protestos.model.TipoTramite;

@Schema(description = "Datos para crear una nueva solicitud de trámite")
public record SolicitudRequest(
    @NotNull
    @Schema(description = "ID de la entidad financiera solicitante", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long entidadId,
    
    @NotNull
    @Schema(description = "Tipo de trámite a realizar", example = "REGISTRO_PROTESTO", requiredMode = Schema.RequiredMode.REQUIRED)
    TipoTramite tipoTramite,
    
    @NotBlank
    @Size(max=20)
    @Schema(description = "Número de documento de identidad del deudor (RUC/DNI)", example = "20999999991", requiredMode = Schema.RequiredMode.REQUIRED)
    String numeroDocumentoDeudor,
    
    @NotNull
    @DecimalMin("0.01")
    @Schema(description = "Monto de la deuda o trámite (debe ser mayor a 0)", example = "1500.50", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal monto,
    
    @Pattern(regexp="PEN|USD")
    @Schema(description = "Moneda del monto ('PEN' o 'USD')", example = "PEN", defaultValue = "PEN")
    String moneda,
    
    @NotBlank
    @Size(max=1000)
    @Schema(description = "Motivo detallado o justificación de la solicitud", example = "Documento impago tras fecha de vencimiento", requiredMode = Schema.RequiredMode.REQUIRED)
    String motivo
) {
    public SolicitudRequest {
        if (moneda == null || moneda.isBlank()) {
            moneda = "PEN";
        }
    }
    public SolicitudRequest(Long entidadId, String motivo, String documento, BigDecimal monto) {
        this(entidadId, TipoTramite.REGISTRO_PROTESTO, documento, monto, "PEN", motivo);
    }
}