package pe.org.camaracomercioica.protestos.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import pe.org.camaracomercioica.protestos.dto.LoginResponse;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.service.AuthResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder encoder;

    @Value("${app.security.jwt.expiration-minutes:60}")
    private long expirationMinutes;

    @Value("${app.security.jwt.issuer:cci-protestos}")
    private String issuer;

    public AuthResult issue(Usuario user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(expirationMinutes));
        String role = user.getRol().getNombre();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("roles", List.of(role))
                .claim("session_version", user.getSessionVersion())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims
        )).getTokenValue();
        return new AuthResult(token, new LoginResponse(
                expiresAt,
                new LoginResponse.UserView(
                        user.getId(), user.getNombreCompleto(), user.getEmail(), List.of(role),
                        user.getTipoDocumento(), user.getNumeroDocumento()
                )
        ));
    }
}
