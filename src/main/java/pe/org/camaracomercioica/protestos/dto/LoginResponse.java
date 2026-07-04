package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Respuesta de inicio de sesión exitoso")
public record LoginResponse(
    @Schema(description = "Fecha de expiración de la sesión", example = "2026-06-19T02:00:00Z")
    Instant expiresAt,
    
    @Schema(description = "Datos básicos del usuario autenticado")
    UserView usuario
) {
    @Schema(description = "Vista de información básica del usuario")
    public record UserView(
        @Schema(description = "ID único del usuario", example = "1")
        Long id,
        
        @Schema(description = "Nombre completo del usuario", example = "Administrador Demo")
        String nombre,
        
        @Schema(description = "Correo electrónico del usuario", example = "admin@demo.local")
        String email,
        
        @Schema(description = "Lista de roles asignados al usuario", example = "[\"CCI_ADMIN\"]")
        List<String> roles,

        @Schema(description = "Tipo de documento del deudor", example = "DNI")
        String tipoDocumento,

        @Schema(description = "Número de documento del deudor", example = "12345678")
        String numeroDocumento
    ) {}
}
