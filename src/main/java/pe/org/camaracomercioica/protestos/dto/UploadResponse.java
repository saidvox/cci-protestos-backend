package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de carga de archivos")
public record UploadResponse(
    @Schema(description = "Identificador único del lote o registro creado", example = "1")
    Long id,

    @Schema(description = "Nombre del archivo cargado", example = "protestos_2026_06.xlsx")
    String nombre,

    @Schema(description = "Estado del procesamiento del archivo", example = "PROCESADO")
    String estado,

    @Schema(description = "Mensaje informativo o detalle del procesamiento", example = "Carga exitosa: 45 registros procesados")
    String mensaje
) {}

