package pe.org.camaracomercioica.protestos.dto;

import java.time.Instant;

public record NotificationItemResponse(
        Long id,
        String action,
        String resource,
        String resourceId,
        String actor,
        String detail,
        Instant occurredAt,
        boolean read
) {
}
