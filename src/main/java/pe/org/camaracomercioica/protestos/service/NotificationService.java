package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.NotificationFeedResponse;
import pe.org.camaracomercioica.protestos.dto.NotificationItemResponse;
import pe.org.camaracomercioica.protestos.exception.UnauthorizedException;
import pe.org.camaracomercioica.protestos.repository.AuditoriaRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final List<String> IMPORTANT_RESOURCES = List.of(
            "SOLICITUD", "CARGA_EXCEL", "DOCUMENTO_TRAMITE", "ENTIDAD", "ANALISTA"
    );

    private final AuditoriaRepository auditorias;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public NotificationFeedResponse feed(String email, int limit) {
        var usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        long lastSeenId = usuario.getUltimaNotificacionVistaId();
        var page = auditorias.findByRecursoIn(
                IMPORTANT_RESOURCES,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"))
        );
        var items = page.getContent().stream()
                .map(item -> new NotificationItemResponse(
                        item.getId(), item.getAccion(), item.getRecurso(), item.getRecursoId(),
                        item.getActor(), item.getDetalle(), item.getFecha(), item.getId() <= lastSeenId
                ))
                .toList();
        long unread = auditorias.countByRecursoInAndIdGreaterThan(IMPORTANT_RESOURCES, lastSeenId);
        return new NotificationFeedResponse(items, unread);
    }

    @Transactional
    public void markRead(String email, long throughId) {
        var usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        long currentMax = auditorias.findTopByRecursoInOrderByIdDesc(IMPORTANT_RESOURCES)
                .map(item -> item.getId())
                .orElse(0L);
        long safeId = Math.min(throughId, currentMax);
        if (safeId > usuario.getUltimaNotificacionVistaId()) {
            usuario.setUltimaNotificacionVistaId(safeId);
            usuarios.save(usuario);
        }
    }
}
