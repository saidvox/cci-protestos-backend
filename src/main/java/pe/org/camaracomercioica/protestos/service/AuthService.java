package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.exception.*;
import pe.org.camaracomercioica.protestos.model.*;
import pe.org.camaracomercioica.protestos.repository.*;
import pe.org.camaracomercioica.protestos.security.JwtService;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository users;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest r) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(r.email(), r.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        return jwt.issue(users.findByEmailIgnoreCase(r.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas")));
    }

    @Transactional
    public void register(RegisterRequest r) {
        // 1. Validar unicidad del email
        if (users.existsByEmailIgnoreCase(r.email())) {
            throw new ConflictException("El correo electrónico ya está registrado.");
        }

        // 2. Validar formato y longitud del número de documento
        String tipo = r.tipoDocumento().toUpperCase();
        String numero = r.numeroDocumento().trim();

        if ("DNI".equals(tipo) && numero.length() != 8) {
            throw new BadRequestException("El DNI debe tener exactamente 8 caracteres.");
        } else if ("RUC".equals(tipo) && numero.length() != 11) {
            throw new BadRequestException("El RUC debe tener exactamente 11 caracteres.");
        }

        // 3. Validar unicidad del documento
        if (users.existsByDeudorTipoDocumentoAndDeudorNumeroDocumento(TipoDocumento.valueOf(tipo), numero)) {
            throw new ConflictException("El documento de identidad ya está registrado por otro deudor.");
        }

        // 4. Buscar rol USER_DEBTOR
        Rol role = roles.findByNombre("USER_DEBTOR")
                .orElseThrow(() -> new ResourceNotFoundException("Rol de deudor no configurado en el sistema."));

        // 5. Crear y persistir usuario
        Usuario u = new Usuario();
        u.setNombreCompleto(r.nombreCompleto());
        u.setEmail(r.email());
        u.setPasswordHash(passwordEncoder.encode(r.password()));
        u.setRol(role);
        u.setTipoDocumento(tipo);
        u.setNumeroDocumento(numero);
        u.setActivo(true);

        users.save(u);
    }

    @Transactional(readOnly = true)
    public LoginResponse getSession(String email) {
        Usuario u = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado o sesión inválida."));
        
        String roleName = u.getRol().getNombre();
        // Asignamos una expiración simulada razonable para la sesión en el frontend (ej. +60 minutos)
        Instant expiresAt = Instant.now().plusSeconds(3600);
        
        return new LoginResponse(
                expiresAt,
                new LoginResponse.UserView(u.getId(), u.getNombreCompleto(), u.getEmail(), List.of(roleName), u.getTipoDocumento(), u.getNumeroDocumento())
        );
    }
}
