package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciales de inicio de sesion")
public record LoginRequest(
        @NotBlank
        @Size(max = 150)
        @Schema(description = "Correo electronico, DNI, RUC o CE del usuario", example = "20123456789", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        @Schema(description = "Contrasena de acceso", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {
}
