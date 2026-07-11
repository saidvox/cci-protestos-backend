package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Documento oficial descargable para tramites de levantamiento")
public record DocumentoTramiteResponse(
        Long id,
        String titulo,
        String descripcion,
        String filename,
        String downloadUrl,
        long sizeBytes,
        String tipo,
        boolean activo,
        int orden,
        Instant creadoEn
) {
}
