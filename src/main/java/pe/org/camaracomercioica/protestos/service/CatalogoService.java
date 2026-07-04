package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.dto.AnalistaRequest;
import pe.org.camaracomercioica.protestos.dto.AnalistaResponse;
import pe.org.camaracomercioica.protestos.dto.EntidadRequest;
import pe.org.camaracomercioica.protestos.dto.EntidadResponse;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
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
        u = usuarios.save(u);

        var a = new Analista();
        a.setUsuario(u);
        a.setCodigo(r.codigo());
        a = analistas.save(a);

        return new AnalistaResponse(a.getId(), a.getCodigo(), a.getUsuario().getNombreCompleto(), a.getUsuario().getEmail(), a.isDisponible(), tempPass);
    }

    private AnalistaResponse map(Analista a) {
        return new AnalistaResponse(a.getId(), a.getCodigo(), a.getUsuario().getNombreCompleto(), a.getUsuario().getEmail(), a.isDisponible(), null);
    }
}
