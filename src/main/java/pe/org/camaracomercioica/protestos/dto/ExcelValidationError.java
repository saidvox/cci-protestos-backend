package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error detectado en una fila de la plantilla Excel")
public record ExcelValidationError(
        @Schema(description = "Numero de fila en Excel", example = "8")
        int row,
        @Schema(description = "Columna o campo observado", example = "Monto")
        String field,
        @Schema(description = "Valor recibido en la celda", example = "-120")
        String value,
        @Schema(description = "Mensaje claro para corregir la celda")
        String message
) {
}
