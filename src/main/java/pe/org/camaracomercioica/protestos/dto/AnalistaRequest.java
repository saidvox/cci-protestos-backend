package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos para registrar un nuevo analista")
public record AnalistaRequest(
    @NotBlank
    @Size(max=150)
    @Schema(description = "Nombre completo del analista", example = "Carlos Ramos", requiredMode = Schema.RequiredMode.REQUIRED)
    String nombre,
    
    @NotBlank
    @Email
    @Schema(description = "Correo electrónico del analista", example = "carlos.ramos@demo.local", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,
    
    @NotBlank
    @Size(max=30)
    @Schema(description = "Código identificador único del analista", example = "AN-002", requiredMode = Schema.RequiredMode.REQUIRED)
    String codigo
) {}
