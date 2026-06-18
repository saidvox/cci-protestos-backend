package pe.org.camaracomercioica.protestos.dto; import jakarta.validation.constraints.*; public record SolicitudRequest(@NotNull Long entidadId,@NotBlank @Size(max=1000) String motivo){}
