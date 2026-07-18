package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.AnalystActivationInfoResponse;
import pe.org.camaracomercioica.protestos.dto.AnalystActivationRequest;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.Analista;
import pe.org.camaracomercioica.protestos.model.InvitacionAnalista;
import pe.org.camaracomercioica.protestos.repository.AnalistaRepository;
import pe.org.camaracomercioica.protestos.repository.InvitacionAnalistaRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AnalystInvitationService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String INVALID_INVITATION = "La invitacion es invalida, vencio o ya fue utilizada";

    private final InvitacionAnalistaRepository invitaciones;
    private final AnalistaRepository analistas;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    @Value("${app.security.analyst-invitation-hours:72}")
    private long invitationHours;

    @Transactional
    public IssuedInvitation emitir(Long analistaId, String actor) {
        var analista = analistas.findById(analistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Analista no encontrado"));
        return emitir(analista, actor);
    }

    @Transactional
    public IssuedInvitation emitir(Analista analista, String actor) {
        var usuario = analista.getUsuario();
        if (usuario.getActivadoEn() != null) {
            throw new BadRequestException("La cuenta del analista ya fue activada");
        }
        if (usuario.getEntidad() == null || !usuario.getEntidad().isActivo()) {
            throw new BadRequestException("La entidad financiera asociada esta inactiva");
        }

        Instant now = Instant.now();
        revocarPendientes(analista.getId(), now);
        String rawToken = newToken();
        var invitacion = new InvitacionAnalista();
        invitacion.setAnalista(analista);
        invitacion.setTokenHash(hash(rawToken));
        invitacion.setExpiraEn(now.plus(Duration.ofHours(invitationHours)));
        invitacion.setCreadoPor(actor);
        invitacion = invitaciones.save(invitacion);
        auditoria.registrar(actor, "CREAR_INVITACION", "ANALISTA", analista.getId(), analista.getCodigo());
        return new IssuedInvitation(rawToken, invitacion.getExpiraEn());
    }

    @Transactional
    public void revocar(Long analistaId, String actor) {
        revocarPendientes(analistaId, Instant.now());
        auditoria.registrar(actor, "REVOCAR_INVITACION", "ANALISTA", analistaId, "Invitaciones pendientes revocadas");
    }

    @Transactional(readOnly = true)
    public AnalystActivationInfoResponse validar(String rawToken) {
        var invitacion = invitaciones.findByTokenHash(hashToken(rawToken))
                .orElseThrow(this::invalidInvitation);
        validateAvailable(invitacion, Instant.now());
        var usuario = invitacion.getAnalista().getUsuario();
        return new AnalystActivationInfoResponse(
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getEntidad().getRazonSocial(),
                invitacion.getExpiraEn()
        );
    }

    @Transactional
    public void activar(AnalystActivationRequest request) {
        Instant now = Instant.now();
        var invitacion = invitaciones.findForUpdateByTokenHash(hashToken(request.token()))
                .orElseThrow(this::invalidInvitation);
        validateAvailable(invitacion, now);

        var analista = invitacion.getAnalista();
        var usuario = analista.getUsuario();
        usuario.setPasswordHash(encoder.encode(request.password()));
        usuario.setActivo(true);
        usuario.setActivadoEn(now);
        usuario.setSessionVersion(usuario.getSessionVersion() + 1);
        analista.setDisponible(true);
        invitacion.setUsadoEn(now);

        usuarios.save(usuario);
        analistas.save(analista);
        invitaciones.save(invitacion);
        revocarPendientesExcepto(analista.getId(), invitacion.getId(), now);
        auditoria.registrar(usuario.getEmail(), "ACTIVAR_CUENTA", "ANALISTA", analista.getId(), analista.getCodigo());
    }

    private void validateAvailable(InvitacionAnalista invitacion, Instant now) {
        var usuario = invitacion.getAnalista().getUsuario();
        if (invitacion.getUsadoEn() != null
                || invitacion.getRevocadoEn() != null
                || !invitacion.getExpiraEn().isAfter(now)
                || usuario.getActivadoEn() != null) {
            throw invalidInvitation();
        }
        if (usuario.getEntidad() == null || !usuario.getEntidad().isActivo()) {
            throw new BadRequestException("La entidad financiera asociada esta inactiva");
        }
    }

    private void revocarPendientes(Long analistaId, Instant now) {
        invitaciones.findByAnalistaIdAndUsadoEnIsNullAndRevocadoEnIsNull(analistaId)
                .forEach(item -> item.setRevocadoEn(now));
    }

    private void revocarPendientesExcepto(Long analistaId, Long invitationId, Instant now) {
        invitaciones.findByAnalistaIdAndUsadoEnIsNullAndRevocadoEnIsNull(analistaId).stream()
                .filter(item -> !item.getId().equals(invitationId))
                .forEach(item -> item.setRevocadoEn(now));
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 256) {
            throw invalidInvitation();
        }
        return hash(rawToken.trim());
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }

    private BadRequestException invalidInvitation() {
        return new BadRequestException(INVALID_INVITATION);
    }

    public record IssuedInvitation(String token, Instant expiresAt) {
    }
}
