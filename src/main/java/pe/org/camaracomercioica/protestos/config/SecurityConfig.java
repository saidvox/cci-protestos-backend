package pe.org.camaracomercioica.protestos.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.org.camaracomercioica.protestos.security.CookieBearerTokenResolver;
import pe.org.camaracomercioica.protestos.security.DatabaseUserDetailsService;
import pe.org.camaracomercioica.protestos.security.LoginRateLimitFilter;
import pe.org.camaracomercioica.protestos.security.SecurityErrorHandler;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(DatabaseUserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${app.security.jwt.secret}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer
    ) {
        var decoder = NimbusJwtDecoder.withSecretKey(key(secret))
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtIssuerValidator(issuer)
        ));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter converter,
            SecurityErrorHandler errors,
            LoginRateLimitFilter limiter,
            @Value("${app.security.cookie.name:CCI_ACCESS_TOKEN}") String cookie
    ) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieName("XSRF-TOKEN");
        csrf.setHeaderName("X-XSRF-TOKEN");

        return http
                .csrf(c -> c.csrfTokenRepository(csrf)
                        .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/login", "/api/v1/auth/register"))
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/csrf",
                                "/api/v1/auth/register",
                                "/api/v1/auth/debtor-lookup",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/portal/debtor/**").hasRole("USER_DEBTOR")
                        .requestMatchers("/api/v1/portal/analyst/**").hasRole("BANK_ANALYST")
                        .requestMatchers("/api/v1/erp/**").hasAnyRole("CCI_ADMIN", "CCI_STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/entidades").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes").hasAnyRole("USER_DEBTOR", "BANK_ANALYST", "CCI_ADMIN", "CCI_STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/mis-solicitudes").hasAnyRole("USER_DEBTOR", "BANK_ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes").hasAnyRole("CCI_ADMIN", "CCI_STAFF", "BANK_ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/documentos/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/documentos-tramite/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/documentos-tramite").hasAnyRole("CCI_ADMIN", "CCI_STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/documentos-tramite/*").hasAnyRole("CCI_ADMIN", "CCI_STAFF")
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/reportes/**", "/api/excel/**").hasAnyRole("CCI_ADMIN", "CCI_STAFF", "BANK_ANALYST")
                        .requestMatchers("/api/entidades/**", "/api/analistas/**", "/api/auditoria/**").hasRole("CCI_ADMIN")
                        .requestMatchers("/api/solicitudes/*/estado").hasAnyRole("CCI_ADMIN", "CCI_STAFF", "BANK_ANALYST")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(new CookieBearerTokenResolver(cookie))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors)
                        .jwt(j -> j.jwtAuthenticationConverter(converter)))
                .addFilterBefore(limiter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String origin) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private SecretKeySpec key(String secret) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
