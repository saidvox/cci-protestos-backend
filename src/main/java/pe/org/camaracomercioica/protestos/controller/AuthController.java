package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.service.AuthService;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "API para la autenticación de usuarios, gestión de sesiones y tokens CSRF")
public class AuthController {

    private final AuthService service;

    @Value("${app.security.cookie.secure:true}")
    boolean secure;

    @Value("${app.security.cookie.name:CCI_ACCESS_TOKEN}")
    String cookieName;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y establece el token JWT en una cookie HttpOnly secure.")
    @ApiResponse(responseCode = "200", description = "Autenticación exitosa, retorna detalles del usuario")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes fallidas, cuenta temporalmente bloqueada")
    @SecurityRequirements // Elimina el candado global para este endpoint público
    public LoginResponse login(@Valid @RequestBody LoginRequest r, HttpServletResponse response) {
        var result = service.login(r);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(result.token(), Duration.between(java.time.Instant.now(), result.response().expiresAt())).toString());
        return result.response();
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtener token CSRF", description = "Obtiene el token CSRF actual necesario para peticiones POST/PUT/DELETE.")
    @ApiResponse(responseCode = "200", description = "Token CSRF obtenido con éxito")
    @SecurityRequirements // Elimina el candado global para este endpoint público
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "parameterName", token.getParameterName(), "token", token.getToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar sesión", description = "Limpia la cookie del token JWT de sesión.")
    @ApiResponse(responseCode = "204", description = "Cierre de sesión exitoso")
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration age) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(age.isNegative() ? Duration.ZERO : age)
                .build();
    }
}