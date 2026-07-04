package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos para el registro de un nuevo deudor")
public record RegisterRequest(
    @NotBlank
    @Size(max = 150)
    @Schema(description = "Nombre completo o razón social", example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
    String nombreCompleto,

    @NotBlank
    @Email
    @Size(max = 150)
    @Schema(description = "Correo electrónico para la cuenta", example = "deudor@test.local", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,

    @NotBlank
    @Size(min = 8, max = 128)
    @Schema(description = "Contraseña de acceso", example = "Demo123!", requiredMode = Schema.RequiredMode.REQUIRED)
    String password,

    @NotBlank
    @Schema(description = "Tipo de documento de identidad (DNI, RUC, CE)", example = "DNI", requiredMode = Schema.RequiredMode.REQUIRED)
    String tipoDocumento,

    @NotBlank
    @Schema(description = "Número de documento de identidad", example = "12345678", requiredMode = Schema.RequiredMode.REQUIRED)
    String numeroDocumento
) {}
