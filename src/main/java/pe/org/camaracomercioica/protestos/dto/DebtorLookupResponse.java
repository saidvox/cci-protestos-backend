package pe.org.camaracomercioica.protestos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de busqueda publica de deudor por documento")
public record DebtorLookupResponse(
        boolean found,
        String nombreCompleto,
        String email
) {
}
