package pe.org.camaracomercioica.protestos.dto;

import java.util.List;

public record NotificationFeedResponse(
        List<NotificationItemResponse> items,
        long unreadCount
) {
}
