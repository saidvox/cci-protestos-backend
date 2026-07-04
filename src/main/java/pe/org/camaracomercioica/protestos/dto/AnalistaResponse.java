package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalle del analista registrado")
public record AnalistaResponse(
    @Schema(description = "ID único del analista", example = "1")
    Long id,
    
    @Schema(description = "Código del analista", example = "AN-001")
    String codigo,
    
    @Schema(description = "Nombre completo del analista", example = "Carlos Ramos")
    String nombre,
    
    @Schema(description = "Correo electrónico del analista", example = "carlos.ramos@demo.local")
    String email,
    
    @Schema(description = "Indica si el analista está disponible para recibir asignaciones", example = "true")
    boolean disponible,
    
    @Schema(description = "Contraseña autogenerada (sólo visible tras la creación inicial)", example = "aBcd1234")
    String password
) {}
