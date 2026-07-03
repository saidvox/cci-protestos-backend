package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.util.List;

@Schema(description = "Respuesta paginada genérica")
public record PageResponse<T>(
    @Schema(description = "Lista de elementos en la página actual")
    List<T> content,

    @Schema(description = "Número de página actual (0-indexed)", example = "0")
    int page,

    @Schema(description = "Tamaño de la página actual", example = "20")
    int size,

    @Schema(description = "Total de elementos disponibles en la consulta", example = "100")
    long totalElements,

    @Schema(description = "Total de páginas disponibles en la consulta", example = "5")
    int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}

