package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.CambioEstadoRequest;
import pe.org.camaracomercioica.protestos.dto.SolicitudRequest;
import pe.org.camaracomercioica.protestos.dto.SolicitudResponse;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import pe.org.camaracomercioica.protestos.exception.ConflictException;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.Deudor;
import pe.org.camaracomercioica.protestos.model.EntidadFinanciera;
import pe.org.camaracomercioica.protestos.model.EstadoProtesto;
import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;
import pe.org.camaracomercioica.protestos.model.TipoTramite;
import pe.org.camaracomercioica.protestos.model.TipoDocumento;
import pe.org.camaracomercioica.protestos.model.TipoPersona;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.repository.AnalistaRepository;
import pe.org.camaracomercioica.protestos.repository.DeudorRepository;
import pe.org.camaracomercioica.protestos.repository.EntidadFinancieraRepository;
import pe.org.camaracomercioica.protestos.repository.ProtestoRepository;
import pe.org.camaracomercioica.protestos.repository.SolicitudRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;
import pe.org.camaracomercioica.protestos.util.SolicitudStatePolicy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitudService {
    private static final List<EstadoSolicitud> ESTADOS_SOLICITUD_ACTIVA = List.of(
            EstadoSolicitud.REGISTRADA,
            EstadoSolicitud.EN_REVISION_CCI,
            EstadoSolicitud.DERIVADA_ENTIDAD,
            EstadoSolicitud.EN_REVISION_ANALISTA,
            EstadoSolicitud.APROBADA_ENTIDAD
    );

    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final EntidadFinancieraRepository entidades;
    private final AnalistaRepository analistas;
    private final DeudorRepository deudores;
    private final ProtestoRepository protestos;
    private final AuditoriaService auditoria;

    @Transactional
    public SolicitudResponse crear(SolicitudRequest r, String email) {
        var usuario = usuario(email);
        var entidad = entidades.findById(r.entidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
        var documento = documentoSolicitud(r, usuario);
        var deudor = resolverDeudor(documento, usuario);

        if (esAnalistaBanco(usuario) && !perteneceAEntidad(usuario, entidad)) {
            throw new AccessDeniedException("Solo puede registrar solicitudes para su propia entidad");
        }
        validarRegularizacionDeudor(usuario, deudor, entidad, r);
        validarSinSolicitudActiva(deudor, entidad, r);

        var s = new pe.org.camaracomercioica.protestos.model.Solicitud();
        s.setCodigo("SOL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        s.setSolicitante(usuario);
        s.setEntidad(entidad);
        s.setDeudor(deudor);
        s.setMotivo(r.motivo().trim());
        s.setTipoTramite(r.tipoTramite());
        s.setMonto(r.monto());
        s.setMoneda(r.moneda());

        s = solicitudes.save(s);
        auditoria.registrar(email, "CREAR", "SOLICITUD", s.getId(), s.getCodigo());
        return map(s);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResponse> listar(EstadoSolicitud estado, Long entidadId, Pageable p, String email) {
        var usuario = usuario(email);
        Long filtroEntidad = entidadId;
        if (esAnalistaBanco(usuario)) {
            if (usuario.getEntidad() == null) {
                throw new AccessDeniedException("Analista sin entidad financiera asociada");
            }
            filtroEntidad = usuario.getEntidad().getId();
        }
        return solicitudes.filtrar(estado, filtroEntidad, p).map(this::map);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResponse> mias(String email, Pageable p) {
        var usuario = usuario(email);
        if (esAnalistaBanco(usuario)) {
            if (usuario.getEntidad() == null) {
                throw new AccessDeniedException("Analista sin entidad financiera asociada");
            }
            var estados = java.util.List.of(
                EstadoSolicitud.DERIVADA_ENTIDAD,
                EstadoSolicitud.EN_REVISION_ANALISTA,
                EstadoSolicitud.OBSERVADA_ENTIDAD,
                EstadoSolicitud.APROBADA_ENTIDAD,
                EstadoSolicitud.FINALIZADA,
                EstadoSolicitud.LEVANTAMIENTO_PROCESADO
            );
            return solicitudes.findByEntidadIdAndEstadoIn(usuario.getEntidad().getId(), estados, p).map(this::map);
        }
        return solicitudes.findBySolicitanteEmailIgnoreCase(email, p).map(this::map);
    }

    @Transactional(readOnly = true)
    public SolicitudResponse obtener(Long id, String email) {
        var usuario = usuario(email);
        var solicitud = solicitudes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        validarLectura(usuario, solicitud);
        return map(solicitud);
    }

    @Transactional
    public SolicitudResponse cambiarEstado(Long id, CambioEstadoRequest r, String actor) {
        var usuario = usuario(actor);
        var s = solicitudes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        validarLectura(usuario, s);
        if (s.getVersion() != null && !s.getVersion().equals(r.version())) {
            throw new ConflictException("La solicitud fue modificada por otro usuario");
        }
        if (!SolicitudStatePolicy.canTransition(s.getEstado(), r.estado())) {
            throw new BadRequestException("Transicion de estado no permitida");
        }
        s.setEstado(r.estado());
        s.setObservacion(r.observacion());
        if (r.analistaId() != null) {
            s.setAnalista(analistas.findById(r.analistaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Analista no encontrado")));
        }
        s.setActualizadoEn(Instant.now());
        s = solicitudes.saveAndFlush(s);
        auditoria.registrar(actor, "CAMBIAR_ESTADO", "SOLICITUD", id, r.estado().name());
        return map(s);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> historialDeudor(String numeroDocumento) {
        return solicitudes.findByDeudorNumeroDocumentoOrderByCreadoEnDesc(numeroDocumento).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public SolicitudResponse actualizar(Long id, SolicitudRequest r, String email) {
        var usuario = usuario(email);
        var s = solicitudes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        
        if (!s.getSolicitante().getEmail().equalsIgnoreCase(email)) {
            throw new org.springframework.security.access.AccessDeniedException("No tiene permisos para editar esta solicitud");
        }
        
        if (s.getEstado() != EstadoSolicitud.OBSERVADA_CCI && s.getEstado() != EstadoSolicitud.OBSERVADA_ENTIDAD) {
            throw new BadRequestException("Solo se pueden corregir solicitudes en estado observado");
        }
        
        var entidad = entidades.findById(r.entidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Entidad financiera no encontrada"));
        
        s.setEntidad(entidad);
        s.setTipoTramite(r.tipoTramite());
        s.setNumeroDocumentoDeudor(r.numeroDocumentoDeudor());
        s.setMonto(r.monto());
        s.setMoneda(r.moneda());
        s.setMotivo(r.motivo());
        s.setEstado(EstadoSolicitud.REGISTRADA);
        s.setActualizadoEn(Instant.now());
        
        s = solicitudes.save(s);
        auditoria.registrar(email, "CORREGIR", "SOLICITUD", id, "REGISTRADA");
        return map(s);
    }

    private Usuario usuario(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String documentoSolicitud(SolicitudRequest r, Usuario usuario) {
        var documento = r.numeroDocumentoDeudor().trim();
        if (esDeudor(usuario)) {
            var documentoUsuario = usuario.getNumeroDocumento();
            if (documentoUsuario == null || documentoUsuario.isBlank()) {
                throw new BadRequestException("El usuario deudor no tiene documento asociado");
            }
            if (!documentoUsuario.equals(documento)) {
                throw new AccessDeniedException("Solo puede registrar solicitudes para su propio documento");
            }
        }
        return documento;
    }

    private Deudor resolverDeudor(String documento, Usuario usuario) {
        var tipo = tipoDocumento(documento);
        if (esDeudor(usuario) && usuario.getDeudor() != null) {
            return usuario.getDeudor();
        }
        return deudores.findByTipoDocumentoAndNumeroDocumento(tipo, documento)
                .orElseGet(() -> {
                    var d = new Deudor();
                    d.setTipoDocumento(tipo);
                    d.setNumeroDocumento(documento);
                    d.setNombreRazonSocial("Deudor " + documento);
                    d.setTipoPersona(tipo == TipoDocumento.RUC ? TipoPersona.JURIDICA : TipoPersona.NATURAL);
                    return deudores.save(d);
                });
    }

    private void validarRegularizacionDeudor(Usuario usuario, Deudor deudor, EntidadFinanciera entidad, SolicitudRequest r) {
        if (!esDeudor(usuario) || r.tipoTramite() != TipoTramite.REGULARIZACION) {
            return;
        }
        if (!protestos.existsByDeudorIdAndEntidadIdAndEstado(deudor.getId(), entidad.getId(), EstadoProtesto.VIGENTE)) {
            throw new BadRequestException("La entidad financiera no corresponde a un protesto vigente del deudor");
        }
    }

    private void validarSinSolicitudActiva(Deudor deudor, EntidadFinanciera entidad, SolicitudRequest r) {
        if (solicitudes.existsActiveDuplicate(deudor.getId(), entidad.getId(), r.tipoTramite(), ESTADOS_SOLICITUD_ACTIVA)) {
            throw new ConflictException("Ya existe una solicitud activa para este protesto");
        }
    }

    private TipoDocumento tipoDocumento(String documento) {
        return documento.length() == 11 ? TipoDocumento.RUC : TipoDocumento.DNI;
    }

    private void validarLectura(Usuario usuario, pe.org.camaracomercioica.protestos.model.Solicitud solicitud) {
        if (esDeudor(usuario) && !solicitud.getSolicitante().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No puede acceder a una solicitud de otro usuario");
        }
        if (esAnalistaBanco(usuario) && !perteneceAEntidad(usuario, solicitud.getEntidad())) {
            throw new AccessDeniedException("No puede acceder a solicitudes de otra entidad");
        }
    }

    private boolean perteneceAEntidad(Usuario usuario, EntidadFinanciera entidad) {
        return usuario.getEntidad() != null && usuario.getEntidad().getId().equals(entidad.getId());
    }

    private boolean esDeudor(Usuario usuario) {
        return "USER_DEBTOR".equals(usuario.getRol().getNombre());
    }

    private boolean esAnalistaBanco(Usuario usuario) {
        return "BANK_ANALYST".equals(usuario.getRol().getNombre());
    }

    private SolicitudResponse map(pe.org.camaracomercioica.protestos.model.Solicitud s) {
        return new SolicitudResponse(
                s.getId(),
                s.getCodigo(),
                s.getSolicitante().getNombreCompleto(),
                s.getEntidad().getRazonSocial(),
                s.getAnalista() == null ? null : s.getAnalista().getUsuario().getNombreCompleto(),
                s.getEstado(),
                s.getTipoTramite(),
                s.getNumeroDocumentoDeudor(),
                s.getMonto(),
                s.getMoneda(),
                s.getMotivo(),
                s.getObservacion(),
                s.getVersion(),
                s.getCreadoEn(),
                s.getActualizadoEn()
        );
    }
}
