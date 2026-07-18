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
    private static final int LOGIN_MAX = 10;
    private static final int LOOKUP_MAX = 30;
    private static final int ACTIVATION_MAX = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ObjectMapper mapper;
    private final ConcurrentMap<String, Window> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest r) {
        boolean login = "POST".equals(r.getMethod()) && "/api/auth/login".equals(r.getRequestURI());
        boolean lookup = "GET".equals(r.getMethod()) && "/api/v1/auth/debtor-lookup".equals(r.getRequestURI());
        boolean activation = "/api/v1/auth/analyst-activation".equals(r.getRequestURI());
        return !login && !lookup && !activation;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        String key = req.getRequestURI() + ":" + ip;
        var now = Instant.now();
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(entry -> entry.getValue().started.plus(WINDOW).isBefore(now));
        }
        var w = attempts.compute(key, (k, old) ->
                old == null || old.started.plus(WINDOW).isBefore(now)
                        ? new Window(now, 1)
                        : new Window(old.started, old.count + 1)
        );
        int max = "/api/auth/login".equals(req.getRequestURI())
                ? LOGIN_MAX
                : "/api/v1/auth/analyst-activation".equals(req.getRequestURI()) ? ACTIVATION_MAX : LOOKUP_MAX;
        if (w.count > max) {
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
