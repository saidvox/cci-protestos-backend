package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Carga Excel realizada por el usuario autenticado")
public record CargaExcelResponse(
        Long id,
        String filename,
        Instant uploadedAt,
        int totalRows,
        int importedRows,
        int errorRows,
        String status,
        String summary,
        String uploader
) {
}
