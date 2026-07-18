package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.LoginRequest;
import pe.org.camaracomercioica.protestos.dto.LoginResponse;
import pe.org.camaracomercioica.protestos.dto.RegisterRequest;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import pe.org.camaracomercioica.protestos.exception.ConflictException;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.exception.UnauthorizedException;
import pe.org.camaracomercioica.protestos.model.Deudor;
import pe.org.camaracomercioica.protestos.model.Rol;
import pe.org.camaracomercioica.protestos.model.TipoDocumento;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.repository.DeudorRepository;
import pe.org.camaracomercioica.protestos.repository.RolRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;
import pe.org.camaracomercioica.protestos.security.JwtService;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository users;
    private final DeudorRepository deudores;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Credenciales invalidas");
        }
        return jwt.issue(resolveLoginUser(request.email()));
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El correo electronico ya esta registrado.");
        }

        TipoDocumento tipoDocumento = parseTipoDocumento(request.tipoDocumento());
        String numeroDocumento = normalizeNumeroDocumento(tipoDocumento, request.numeroDocumento());
        validateNumeroDocumento(tipoDocumento, numeroDocumento);

        if (users.existsByDeudorTipoDocumentoAndDeudorNumeroDocumento(tipoDocumento, numeroDocumento)) {
            throw new ConflictException("El documento de identidad ya esta asociado a otra cuenta.");
        }

        Rol role = roles.findByNombre("USER_DEBTOR")
                .orElseThrow(() -> new ResourceNotFoundException("Rol de deudor no configurado en el sistema."));

        Deudor deudor = deudores.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)
                .orElseThrow(() -> new BadRequestException(
                        "El documento no figura en el registro de protestos. Verifique el numero ingresado."
                ));
        if (deudor.getEmail() == null || deudor.getEmail().isBlank()) {
            deudor.setEmail(email);
        }
        deudor.setActualizadoEn(Instant.now());
        deudor = deudores.save(deudor);

        Usuario user = new Usuario();
        user.setNombreCompleto(deudor.getNombreRazonSocial());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRol(role);
        user.setDeudor(deudor);
        user.setActivo(true);

        users.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse getSession(String email) {
        Usuario user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado o sesion invalida."));

        String roleName = user.getRol().getNombre();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        return new LoginResponse(
                expiresAt,
                new LoginResponse.UserView(
                        user.getId(),
                        user.getNombreCompleto(),
                        user.getEmail(),
                        List.of(roleName),
                        user.getTipoDocumento(),
                        user.getNumeroDocumento()
                )
        );
    }

    @Transactional(readOnly = true)
    public DeudorLookup lookupDebtor(String tipoDocumento, String numeroDocumento) {
        TipoDocumento tipo = parseTipoDocumento(tipoDocumento);
        String numero = normalizeNumeroDocumento(tipo, numeroDocumento);
        validateNumeroDocumento(tipo, numero);

        return deudores.findByTipoDocumentoAndNumeroDocumento(tipo, numero)
                .map(deudor -> new DeudorLookup(true, deudor.getNombreRazonSocial()))
                .orElseGet(() -> new DeudorLookup(false, null));
    }

    private Usuario resolveLoginUser(String identifier) {
        String value = identifier.trim();
        if (value.contains("@")) {
            return users.findByEmailIgnoreCase(value)
                    .orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));
        }
        String document = value.replaceAll("\\s+", "").toUpperCase();
        if (document.matches("\\d+")) {
            document = document.replaceAll("\\D", "");
        }
        var matches = users.findByDeudorNumeroDocumento(document);
        if (matches.size() != 1) {
            throw new UnauthorizedException("Credenciales invalidas");
        }
        return matches.get(0);
    }

    private TipoDocumento parseTipoDocumento(String value) {
        try {
            return TipoDocumento.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BadRequestException("Tipo de documento no permitido.");
        }
    }

    private String normalizeNumeroDocumento(TipoDocumento tipo, String value) {
        String normalized = value.trim();
        if (tipo == TipoDocumento.DNI || tipo == TipoDocumento.RUC) {
            return normalized.replaceAll("\\D", "");
        }
        return normalized.replaceAll("\\s+", "").toUpperCase();
    }

    private void validateNumeroDocumento(TipoDocumento tipo, String numero) {
        if (tipo == TipoDocumento.DNI && !numero.matches("\\d{8}")) {
            throw new BadRequestException("El DNI debe tener exactamente 8 digitos.");
        }
        if (tipo == TipoDocumento.RUC && !numero.matches("\\d{11}")) {
            throw new BadRequestException("El RUC debe tener exactamente 11 digitos.");
        }
        if (tipo == TipoDocumento.CE && !numero.matches("[A-Z0-9]{8,12}")) {
            throw new BadRequestException("El CE debe tener entre 8 y 12 caracteres alfanumericos.");
        }
    }

    public record DeudorLookup(boolean found, String nombreCompleto) {
    }
}
