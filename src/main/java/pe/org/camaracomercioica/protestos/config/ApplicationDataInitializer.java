package pe.org.camaracomercioica.protestos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.org.camaracomercioica.protestos.model.Analista;
import pe.org.camaracomercioica.protestos.model.Deudor;
import pe.org.camaracomercioica.protestos.model.EntidadFinanciera;
import pe.org.camaracomercioica.protestos.model.Rol;
import pe.org.camaracomercioica.protestos.model.TipoDocumento;
import pe.org.camaracomercioica.protestos.model.TipoPersona;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.repository.AnalistaRepository;
import pe.org.camaracomercioica.protestos.repository.DeudorRepository;
import pe.org.camaracomercioica.protestos.repository.EntidadFinancieraRepository;
import pe.org.camaracomercioica.protestos.repository.RolRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

@Component
@RequiredArgsConstructor
public class ApplicationDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "password";

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final EntidadFinancieraRepository entidades;
    private final DeudorRepository deudores;
    private final AnalistaRepository analistas;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        var admin = role("CCI_ADMIN");
        var staff = role("CCI_STAFF");
        var bankAnalyst = role("BANK_ANALYST");
        var debtor = role("USER_DEBTOR");
        var entidad = entidadDemo();
        entidad("20100047218", "Banco Nacional Demo", "Mesa Protestos", "protestos@banconacional.demo");
        entidad("20462509236", "Financiera Ica Demo", "Operaciones", "operaciones@financieraica.demo");
        var deudor = deudorDemo();

        user("admin@demo.local", "Administrador CCI", admin, null, null);
        user("staff@demo.local", "Operador CCI", staff, null, null);
        var analystUser = user("analista@demo.local", "Analista Banco Demo", bankAnalyst, entidad, null);
        user("deudor@demo.local", "Deudor Demo", debtor, null, deudor);
        analistaDemo(analystUser);
    }

    private Rol role(String nombre) {
        return roles.findByNombre(nombre).orElseGet(() -> {
            var r = new Rol();
            r.setNombre(nombre);
            return roles.save(r);
        });
    }

    private EntidadFinanciera entidadDemo() {
        return entidad("20111111111", "Banco Demo Ica", "Mesa de Operaciones", "operaciones@bancodemo.local");
    }

    private EntidadFinanciera entidad(String ruc, String razonSocial, String contacto, String email) {
        return entidades.findByRuc(ruc).orElseGet(() -> {
            var e = new EntidadFinanciera();
            e.setRuc(ruc);
            e.setRazonSocial(razonSocial);
            e.setContacto(contacto);
            e.setEmail(email);
            return entidades.save(e);
        });
    }

    private Deudor deudorDemo() {
        return deudores.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.RUC, "20123456789")
                .orElseGet(() -> {
                    var d = new Deudor();
                    d.setTipoDocumento(TipoDocumento.RUC);
                    d.setNumeroDocumento("20123456789");
                    d.setNombreRazonSocial("Deudor Demo SAC");
                    d.setTipoPersona(TipoPersona.JURIDICA);
                    d.setEmail("deudor@demo.local");
                    return deudores.save(d);
                });
    }

    private Usuario user(String email, String nombre, Rol rol, EntidadFinanciera entidad, Deudor deudor) {
        var u = usuarios.findByEmailIgnoreCase(email).orElseGet(Usuario::new);
        u.setNombreCompleto(nombre);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        u.setRol(rol);
        u.setEntidad(entidad);
        u.setDeudor(deudor);
        u.setActivo(true);
        return usuarios.save(u);
    }

    private void analistaDemo(Usuario usuario) {
        boolean exists = analistas.findAll().stream()
                .anyMatch(a -> a.getUsuario().getId().equals(usuario.getId()));
        if (exists) {
            return;
        }

        var a = new Analista();
        a.setUsuario(usuario);
        a.setCodigo("AN-DEMO-001");
        analistas.save(a);
    }

}
