package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Vista resumida de una fila valida antes de importarla")
public record ExcelRowPreview(
        int row,
        String numeroDocumento,
        String nombreRazonSocial,
        String entidad,
        String numeroTitulo,
        String tipoTitulo,
        LocalDate fechaProtesto,
        String moneda,
        BigDecimal monto,
        String estado
) {
}
