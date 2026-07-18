package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import pe.org.camaracomercioica.protestos.exception.ConflictException;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.Analista;
import pe.org.camaracomercioica.protestos.model.EntidadFinanciera;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.repository.AnalistaRepository;
import pe.org.camaracomercioica.protestos.repository.EntidadFinancieraRepository;
import pe.org.camaracomercioica.protestos.repository.RolRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final EntidadFinancieraRepository entidades;
    private final AnalistaRepository analistas;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<EntidadResponse> entidades() {
        return entidades.findAll().stream().map(this::map).toList();
    }

    @Transactional
    public EntidadResponse crear(EntidadRequest request, String actor) {
        if (entidades.existsByRuc(request.ruc())) {
            throw new ConflictException("El RUC ya esta registrado");
        }
        var entidad = new EntidadFinanciera();
        entidad.setRuc(request.ruc());
        entidad.setRazonSocial(request.razonSocial().trim());
        entidad.setContacto(request.contacto());
        entidad.setEmail(request.email());
        entidad = entidades.save(entidad);
        auditoria.registrar(actor, "CREAR", "ENTIDAD", entidad.getId(), entidad.getRazonSocial());
        return map(entidad);
    }

    @Transactional
    public EntidadResponse actualizar(Long id, UpdateEntidadRequest request, String actor) {
        var entidad = entidades.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
        if (!entidad.getRuc().equals(request.ruc()) && entidades.existsByRuc(request.ruc())) {
            throw new ConflictException("El RUC ya esta registrado");
        }
        entidad.setRuc(request.ruc());
        entidad.setRazonSocial(request.razonSocial().trim());
        entidad.setContacto(request.contacto());
        entidad.setEmail(request.email());
        entidad.setActivo(request.activo());
        entidad = entidades.save(entidad);
        auditoria.registrar(actor, "ACTUALIZAR", "ENTIDAD", entidad.getId(), entidad.getRazonSocial());
        return map(entidad);
    }

    @Transactional
    public EntidadResponse cambiarEstadoEntidad(Long id, CambioEstadoEntidadRequest request, String actor) {
        var entidad = entidades.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
        entidad.setActivo(request.activo());
        entidad = entidades.save(entidad);
        auditoria.registrar(
                actor, "CAMBIAR_ESTADO", "ENTIDAD", entidad.getId(),
                request.activo() ? "Entidad habilitada" : "Entidad deshabilitada"
        );
        return map(entidad);
    }

    @Transactional(readOnly = true)
    public List<AnalistaResponse> analistas() {
        return analistas.findAll().stream().map(this::map).toList();
    }

    @Transactional
    public AnalistaResponse crear(AnalistaRequest request, String actor) {
        String email = normalizeEmail(request.email());
        String codigo = normalizeCode(request.codigo());
        if (analistas.existsByCodigo(codigo)) {
            throw new ConflictException("El codigo del analista ya esta registrado");
        }
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El correo electronico ya esta registrado");
        }
        var entidad = activeEntity(request.entidadId());
        var usuario = new Usuario();
        usuario.setNombreCompleto(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(encoder.encode(request.password()));
        usuario.setRol(roles.findByNombre("BANK_ANALYST")
                .orElseThrow(() -> new BadRequestException("Rol BANK_ANALYST no configurado")));
        usuario.setEntidad(entidad);
        usuario.setActivo(true);
        usuario.setSessionVersion(1);
        usuario = usuarios.save(usuario);

        var analista = new Analista();
        analista.setUsuario(usuario);
        analista.setCodigo(codigo);
        analista.setDisponible(true);
        analista = analistas.save(analista);
        auditoria.registrar(actor, "CREAR", "ANALISTA", analista.getId(), analista.getCodigo());
        return map(analista);
    }

    @Transactional
    public AnalistaResponse actualizar(Long id, UpdateAnalistaRequest request, String actor) {
        var analista = analistas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analista no encontrado"));
        var usuario = analista.getUsuario();
        String codigo = normalizeCode(request.codigo());
        String email = normalizeEmail(request.email());
        if (!analista.getCodigo().equals(codigo) && analistas.existsByCodigo(codigo)) {
            throw new ConflictException("El codigo del analista ya esta registrado");
        }
        if (!usuario.getEmail().equalsIgnoreCase(email) && usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El correo electronico ya esta registrado");
        }
        var entidad = activeEntity(request.entidadId());
        usuario.setNombreCompleto(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setEntidad(entidad);
        syncAccess(analista, usuario, request.disponible());
        usuarios.save(usuario);

        analista.setCodigo(codigo);
        analista = analistas.save(analista);
        auditoria.registrar(actor, "ACTUALIZAR", "ANALISTA", analista.getId(), analista.getCodigo());
        return map(analista);
    }

    @Transactional
    public AnalistaResponse cambiarEstadoAnalista(Long id, CambioEstadoAnalistaRequest request, String actor) {
        var analista = analistas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analista no encontrado"));
        syncAccess(analista, analista.getUsuario(), request.disponible());
        usuarios.save(analista.getUsuario());
        analista = analistas.save(analista);
        auditoria.registrar(
                actor, "CAMBIAR_ESTADO", "ANALISTA", analista.getId(),
                request.disponible() ? "Analista habilitado" : "Analista deshabilitado"
        );
        return map(analista);
    }

    @Transactional
    public void restablecerPasswordAnalista(Long id, ResetPasswordAnalistaRequest request, String actor) {
        var analista = analistas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analista no encontrado"));
        var usuario = analista.getUsuario();
        usuario.setPasswordHash(encoder.encode(request.password()));
        usuario.setSessionVersion(usuario.getSessionVersion() + 1);
        usuarios.save(usuario);
        auditoria.registrar(actor, "RESTABLECER_PASSWORD", "ANALISTA", analista.getId(), analista.getCodigo());
    }

    private void syncAccess(Analista analista, Usuario usuario, boolean enabled) {
        if (analista.isDisponible() != enabled || usuario.isActivo() != enabled) {
            usuario.setSessionVersion(usuario.getSessionVersion() + 1);
        }
        analista.setDisponible(enabled);
        usuario.setActivo(enabled);
    }

    private EntidadFinanciera activeEntity(Long id) {
        var entidad = entidades.findById(id)
                .orElseThrow(() -> new BadRequestException("Entidad financiera no encontrada"));
        if (!entidad.isActivo()) {
            throw new BadRequestException("La entidad financiera esta inactiva");
        }
        return entidad;
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private EntidadResponse map(EntidadFinanciera entidad) {
        return new EntidadResponse(
                entidad.getId(), entidad.getRuc(), entidad.getRazonSocial(),
                entidad.getContacto(), entidad.getEmail(), entidad.isActivo()
        );
    }

    private AnalistaResponse map(Analista analista) {
        var usuario = analista.getUsuario();
        return new AnalistaResponse(
                analista.getId(), analista.getCodigo(), usuario.getNombreCompleto(), usuario.getEmail(),
                analista.isDisponible(),
                usuario.getEntidad() == null ? null : usuario.getEntidad().getId(),
                usuario.getEntidad() == null ? null : usuario.getEntidad().getRazonSocial()
        );
    }
}
