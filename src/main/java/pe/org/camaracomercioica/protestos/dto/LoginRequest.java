package pe.org.camaracomercioica.protestos.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Credenciales de inicio de sesión")
public record LoginRequest(
    @NotBlank
    @Email
    @Size(max=150)
    @Schema(description = "Correo electrónico del usuario", example = "admin@demo.local", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,

    @NotBlank
    @Size(min=8,max=128)
    @Schema(description = "Contraseña de acceso", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    String password
) {}
