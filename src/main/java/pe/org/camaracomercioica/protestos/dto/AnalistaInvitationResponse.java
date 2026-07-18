package pe.org.camaracomercioica.protestos.dto;

import java.time.Instant;

public record AnalistaInvitationResponse(
        AnalistaResponse analista,
        String activationToken,
        Instant expiresAt
) {
}
