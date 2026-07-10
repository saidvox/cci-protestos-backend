package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado de validar una plantilla Excel de protestos")
public record ExcelValidationResponse(
        boolean valid,
        int totalRows,
        int validRows,
        int errorRows,
        List<ExcelValidationError> errors,
        List<ExcelRowPreview> preview
) {
}
