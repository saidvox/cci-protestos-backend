package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado de importar una plantilla Excel de protestos")
public record ExcelImportResponse(
        Long cargaId,
        String filename,
        String status,
        String summary,
        int totalRows,
        int importedRows,
        int errorRows,
        List<ExcelValidationError> errors
) {
}
