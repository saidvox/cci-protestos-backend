package pe.org.camaracomercioica.protestos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.model.*;
import pe.org.camaracomercioica.protestos.repository.*;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final EntidadFinancieraRepository entidades;
    private final AnalistaRepository analistas;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        var rolAdmin = roles.findByNombre("ADMIN").orElseGet(() -> { var r = new Rol(); r.setNombre("ADMIN"); return roles.save(r); });
        var rolAnalista = roles.findByNombre("ANALISTA").orElseGet(() -> { var r = new Rol(); r.setNombre("ANALISTA"); return roles.save(r); });
        var rolEntidad = roles.findByNombre("ENTIDAD").orElseGet(() -> { var r = new Rol(); r.setNombre("ENTIDAD"); return roles.save(r); });

        // Crear Entidad Demo si no hay ninguna
        var entidad = entidades.findAll().stream().findFirst().orElseGet(() -> {
            var e = new EntidadFinanciera();
            e.setRuc("20111111111");
            e.setRazonSocial("Financiera Demo Ica");
            e.setContacto("Contacto Demo");
            e.setEmail("contacto@demo.local");
            return entidades.save(e);
        });

        // Crear Admin
        if (usuarios.findByEmailIgnoreCase("admin@demo.local").isEmpty()) {
            var u = new Usuario();
            u.setNombreCompleto("Administrador Demo");
            u.setEmail("admin@demo.local");
            u.setPasswordHash(passwordEncoder.encode("password"));
            u.setRol(rolAdmin);
            usuarios.save(u);
        }

        // Crear Analista
        var usuarioAnalista = usuarios.findByEmailIgnoreCase("analista@demo.local").orElseGet(() -> {
            var u = new Usuario();
            u.setNombreCompleto("Analista Demo");
            u.setEmail("analista@demo.local");
            u.setPasswordHash(passwordEncoder.encode("password"));
            u.setRol(rolAnalista);
            return usuarios.save(u);
        });

        if (analistas.findAll().stream().noneMatch(a -> a.getUsuario().getId().equals(usuarioAnalista.getId()))) {
            var a = new Analista();
            a.setUsuario(usuarioAnalista);
            a.setCodigo("AN-001");
            analistas.save(a);
        }

        // Crear Entidad Demo User
        if (usuarios.findByEmailIgnoreCase("entidad@demo.local").isEmpty()) {
            var u = new Usuario();
            u.setNombreCompleto("Entidad Demo");
            u.setEmail("entidad@demo.local");
            u.setPasswordHash(passwordEncoder.encode("password"));
            u.setRol(rolEntidad);
            u.setEntidad(entidad);
            usuarios.save(u);
        }
    }
}
