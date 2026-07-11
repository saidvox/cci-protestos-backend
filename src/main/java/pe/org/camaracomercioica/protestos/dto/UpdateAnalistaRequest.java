package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos para actualizar un analista existente")
public record UpdateAnalistaRequest(
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
    String codigo,

    @NotNull
    @Schema(description = "ID de la entidad financiera asociada", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long entidadId,

    @Schema(description = "Indica si el analista está disponible", example = "true")
    boolean disponible
) {}
