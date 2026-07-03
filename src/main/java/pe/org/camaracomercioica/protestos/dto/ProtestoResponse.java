package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Detalle del protesto de un título valor")
public record ProtestoResponse(
    @Schema(description = "ID único del protesto", example = "1")
    Long id,
    
    @Schema(description = "Número de documento del deudor (RUC/DNI)", example = "20999999991")
    String numeroDocumento,
    
    @Schema(description = "Nombre completo o razón social del deudor", example = "Persona Ficticia")
    String nombreDeudor,
    
    @Schema(description = "Razón social de la entidad financiera asociada", example = "Financiera Demo Ica")
    String entidad,
    
    @Schema(description = "Tipo de título valor protestado", example = "LETRA_DE_CAMBIO")
    String tipoTitulo,
    
    @Schema(description = "Monto protestado", example = "1500.50")
    BigDecimal monto,
    
    @Schema(description = "Moneda del monto ('PEN' o 'USD')", example = "PEN")
    String moneda,
    
    @Schema(description = "Fecha de registro del protesto", example = "2026-06-18")
    LocalDate fechaProtesto,
    
    @Schema(description = "Indica si el protesto se encuentra vigente", example = "true")
    boolean vigente
) {}
