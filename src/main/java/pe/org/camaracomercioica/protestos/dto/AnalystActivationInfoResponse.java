package pe.org.camaracomercioica.protestos.dto;

import java.time.Instant;

public record AnalystActivationInfoResponse(
        String nombre,
        String email,
        String entidad,
        Instant expiresAt
) {
}
