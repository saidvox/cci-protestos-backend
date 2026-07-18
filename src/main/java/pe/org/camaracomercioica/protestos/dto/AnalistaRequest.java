package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un nuevo analista")
public record AnalistaRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 30) String codigo,
        @NotNull Long entidadId
) {
}
