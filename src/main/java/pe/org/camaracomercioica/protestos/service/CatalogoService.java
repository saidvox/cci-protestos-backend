package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.Analista;
import pe.org.camaracomercioica.protestos.model.EntidadFinanciera;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.repository.AnalistaRepository;
import pe.org.camaracomercioica.protestos.repository.EntidadFinancieraRepository;
import pe.org.camaracomercioica.protestos.repository.RolRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final EntidadFinancieraRepository entidades;
    private final AnalistaRepository analistas;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public List<EntidadResponse> entidades() {
        return entidades.findAll().stream()
                .map(e -> new EntidadResponse(e.getId(), e.getRuc(), e.getRazonSocial(), e.getContacto(), e.getEmail(), e.isActivo()))
                .toList();
    }

    @Transactional
    public EntidadResponse crear(EntidadRequest r) {
        if (entidades.existsByRuc(r.ruc())) {
            throw new BadRequestException("El RUC ya existe");
        }

        var e = new EntidadFinanciera();
        e.setRuc(r.ruc());
        e.setRazonSocial(r.razonSocial().trim());
        e.setContacto(r.contacto());
        e.setEmail(r.email());
        e = entidades.save(e);

        return new EntidadResponse(e.getId(), e.getRuc(), e.getRazonSocial(), e.getContacto(), e.getEmail(), e.isActivo());
    }

    @Transactional
    public EntidadResponse actualizar(Long id, UpdateEntidadRequest r) {
        var e = entidades.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
        
        if (!e.getRuc().equals(r.ruc()) && entidades.existsByRuc(r.ruc())) {
            throw new BadRequestException("El RUC ya existe");
        }

        e.setRuc(r.ruc());
        e.setRazonSocial(r.razonSocial().trim());
        e.setContacto(r.contacto());
        e.setEmail(r.email());
        e.setActivo(r.activo());
        e = entidades.save(e);

        return new EntidadResponse(e.getId(), e.getRuc(), e.getRazonSocial(), e.getContacto(), e.getEmail(), e.isActivo());
    }

    @Transactional
    public EntidadResponse cambiarEstadoEntidad(Long id, CambioEstadoEntidadRequest r) {
        var e = entidades.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));

        e.setActivo(r.activo());
        e = entidades.save(e);

        return new EntidadResponse(e.getId(), e.getRuc(), e.getRazonSocial(), e.getContacto(), e.getEmail(), e.isActivo());
    }

    @Transactional(readOnly = true)
    public List<AnalistaResponse> analistas() {
        return analistas.findAll().stream().map(this::map).toList();
    }

    @Transactional
    public AnalistaResponse crear(AnalistaRequest r) {
        if (analistas.existsByCodigo(r.codigo()) || usuarios.findByEmailIgnoreCase(r.email()).isPresent()) {
            throw new BadRequestException("Email o codigo ya existe");
        }

        String tempPass = "Demo!" + (100000 + new SecureRandom().nextInt(900000)) + "Aa";
        var u = new Usuario();
        u.setNombreCompleto(r.nombre());
        u.setEmail(r.email());
        u.setPasswordHash(encoder.encode(tempPass));
        u.setRol(roles.findByNombre("BANK_ANALYST")
                .orElseThrow(() -> new BadRequestException("Rol BANK_ANALYST no configurado")));
        
        var entidad = entidades.findById(r.entidadId())
                .orElseThrow(() -> new BadRequestException("Entidad financiera no encontrada"));
        u.setEntidad(entidad);

        u = usuarios.save(u);

        var a = new Analista();
        a.setUsuario(u);
        a.setCodigo(r.codigo());
        a = analistas.save(a);

        return new AnalistaResponse(a.getId(), a.getCodigo(), a.getUsuario().getNombreCompleto(), a.getUsuario().getEmail(), a.isDisponible(), tempPass, entidad.getId(), entidad.getRazonSocial());
    }

    @Transactional
    public AnalistaResponse actualizar(Long id, UpdateAnalistaRequest r) {
        var a = analistas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analista no encontrado"));
        var u = a.getUsuario();

        if (!a.getCodigo().equals(r.codigo()) && analistas.existsByCodigo(r.codigo())) {
            throw new BadRequestException("El código ya está en uso");
        }
        if (!u.getEmail().equalsIgnoreCase(r.email()) && usuarios.findByEmailIgnoreCase(r.email()).isPresent()) {
            throw new BadRequestException("El email ya está en uso");
        }

        var entidad = entidades.findById(r.entidadId())
                .orElseThrow(() -> new BadRequestException("Entidad financiera no encontrada"));

        u.setNombreCompleto(r.nombre());
        u.setEmail(r.email());
        u.setEntidad(entidad);
        usuarios.save(u);

        a.setCodigo(r.codigo());
        a.setDisponible(r.disponible());
        a = analistas.save(a);

        return new AnalistaResponse(a.getId(), a.getCodigo(), u.getNombreCompleto(), u.getEmail(), a.isDisponible(), null, entidad.getId(), entidad.getRazonSocial());
    }

    @Transactional
    public AnalistaResponse cambiarEstadoAnalista(Long id, CambioEstadoAnalistaRequest r) {
        var a = analistas.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analista no encontrado"));

        a.setDisponible(r.disponible());
        a = analistas.save(a);

        return map(a);
    }

    private AnalistaResponse map(Analista a) {
        var u = a.getUsuario();
        return new AnalistaResponse(
                a.getId(),
                a.getCodigo(),
                u.getNombreCompleto(),
                u.getEmail(),
                a.isDisponible(),
                null,
                u.getEntidad() != null ? u.getEntidad().getId() : null,
                u.getEntidad() != null ? u.getEntidad().getRazonSocial() : null
        );
    }
}
