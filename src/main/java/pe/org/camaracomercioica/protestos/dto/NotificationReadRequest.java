package pe.org.camaracomercioica.protestos.dto;

import jakarta.validation.constraints.Positive;

public record NotificationReadRequest(
        @Positive Long throughId
) {
}
