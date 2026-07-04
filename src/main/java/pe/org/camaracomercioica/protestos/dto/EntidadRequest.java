package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos para registrar una nueva entidad financiera")
public record EntidadRequest(
    @NotBlank
    @Pattern(regexp="\\d{11}")
    @Schema(description = "RUC único de la entidad (11 dígitos)", example = "20111111111", requiredMode = Schema.RequiredMode.REQUIRED)
    String ruc,
    
    @NotBlank
    @Size(max=150)
    @Schema(description = "Razón social o nombre legal de la entidad", example = "Financiera Demo Ica", requiredMode = Schema.RequiredMode.REQUIRED)
    String razonSocial,
    
    @Size(max=150)
    @Schema(description = "Nombre de la persona de contacto", example = "Contacto Demo")
    String contacto,
    
    @Email
    @Size(max=150)
    @Schema(description = "Correo electrónico de contacto", example = "contacto@demo.local")
    String email
) {}
