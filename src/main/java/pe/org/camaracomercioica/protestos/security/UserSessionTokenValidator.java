package pe.org.camaracomercioica.protestos.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

@Component
@RequiredArgsConstructor
public class UserSessionTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_SESSION = new OAuth2Error(
            "invalid_token", "La sesion ya no es valida", null
    );

    private final UsuarioRepository usuarios;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Number claim = jwt.getClaim("session_version");
        int tokenVersion = claim == null ? 0 : claim.intValue();
        boolean valid = usuarios.findByEmailIgnoreCase(jwt.getSubject())
                .filter(user -> user.isActivo() && user.getSessionVersion() == tokenVersion)
                .isPresent();
        return valid
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_SESSION);
    }
}
