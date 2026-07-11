package pe.org.camaracomercioica.protestos.dto;

import java.time.Instant;

public record DocumentoResponse(
        Long id,
        Long solicitudId,
        String filename,
        String mimeType,
        long sizeBytes,
        String downloadUrl,
        Instant creadoEn
) {
}
