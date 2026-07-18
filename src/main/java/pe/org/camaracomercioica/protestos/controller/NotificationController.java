package pe.org.camaracomercioica.protestos.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.org.camaracomercioica.protestos.dto.NotificationFeedResponse;
import pe.org.camaracomercioica.protestos.dto.NotificationReadRequest;
import pe.org.camaracomercioica.protestos.service.NotificationService;

@RestController
@Validated
@RequestMapping("/api/v1/erp/notificaciones")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public NotificationFeedResponse list(
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit,
            Authentication authentication
    ) {
        return service.feed(authentication.getName(), limit);
    }

    @PatchMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @Valid @RequestBody NotificationReadRequest request,
            Authentication authentication
    ) {
        service.markRead(authentication.getName(), request.throughId());
    }
}
