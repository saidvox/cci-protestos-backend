package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalle de la entidad financiera registrada")
public record EntidadResponse(
    @Schema(description = "ID único de la entidad", example = "1")
    Long id,
    
    @Schema(description = "RUC de la entidad", example = "20111111111")
    String ruc,
    
    @Schema(description = "Razón social o nombre legal de la entidad", example = "Financiera Demo Ica")
    String razonSocial,
    
    @Schema(description = "Nombre de la persona de contacto", example = "Contacto Demo")
    String contacto,
    
    @Schema(description = "Correo electrónico de contacto", example = "contacto@demo.local")
    String email,
    
    @Schema(description = "Indica si la entidad financiera se encuentra activa", example = "true")
    boolean activo
) {}
