package pe.org.camaracomercioica.protestos.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.org.camaracomercioica.protestos.exception.ApiError;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ObjectMapper mapper;
    private final ConcurrentMap<String, Window> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest r) {
        return !("POST".equals(r.getMethod()) && "/api/auth/login".equals(r.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {
        String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                .map(v -> v.split(",")[0].trim())
                .orElse(req.getRemoteAddr());
        var now = Instant.now();
        var w = attempts.compute(ip, (k, old) ->
                old == null || old.started.plus(WINDOW).isBefore(now)
                        ? new Window(now, 1)
                        : new Window(old.started, old.count + 1)
        );
        if (w.count > MAX) {
            res.setStatus(429);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(res.getOutputStream(), new ApiError(now, 429, "RATE_LIMITED",
                    "Demasiados intentos; inténtelo más tarde", req.getRequestURI(), Map.of()));
            return;
        }
        chain.doFilter(req, res);
    }

    private record Window(Instant started, int count) {}
}