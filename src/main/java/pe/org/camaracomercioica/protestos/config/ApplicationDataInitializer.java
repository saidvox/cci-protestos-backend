package pe.org.camaracomercioica.protestos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import pe.org.camaracomercioica.protestos.repository.SolicitudRepository;
import pe.org.camaracomercioica.protestos.repository.ProtestoRepository;

@Component
@RequiredArgsConstructor
public class ApplicationDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "password";

    @Value("${app.data.demo-enabled:false}")
    private boolean demoEnabled;

    @Value("${app.bootstrap.admin.email:}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    @Value("${app.bootstrap.admin.name:Administrador CCI}")
    private String bootstrapAdminName;

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final EntidadFinancieraRepository entidades;
    private final DeudorRepository deudores;
    private final AnalistaRepository analistas;
    private final PasswordEncoder passwordEncoder;
    private final SolicitudRepository solicitudes;
    private final ProtestoRepository protestos;

    @Override
    @Transactional
    public void run(String... args) {
        var admin = role("CCI_ADMIN");
        var staff = role("CCI_STAFF");
        var bankAnalyst = role("BANK_ANALYST");
        var debtor = role("USER_DEBTOR");

        if (demoEnabled) {
            seedDemoData(admin, staff, bankAnalyst, debtor);
        }
        bootstrapAdmin(admin);
        synchronizeApprovedRequests();
    }

    private void seedDemoData(Rol admin, Rol staff, Rol bankAnalyst, Rol debtor) {
        var entidad = entidadPrincipal();
        entidad("20100047218", "Banco Nacional", "Mesa Protestos", "protestos@banconacional.local");
        entidad("20462509236", "Financiera Ica", "Operaciones", "operaciones@financieraica.local");
        var deudor = deudorInicial();

        user("admin@demo.local", "Administrador CCI", admin, null, null);
        user("staff@demo.local", "Operador CCI", staff, null, null);
        var analystUser = user("analista@demo.local", "Analista Banco Ica", bankAnalyst, entidad, null);
        user("deudor@demo.local", "Distribuidora Sol del Sur S.R.L.", debtor, null, deudor);
        analistaInicial(analystUser);
    }

    private void bootstrapAdmin(Rol adminRole) {
        boolean hasEmail = StringUtils.hasText(bootstrapAdminEmail);
        boolean hasPassword = StringUtils.hasText(bootstrapAdminPassword);
        if (!hasEmail && !hasPassword) {
            return;
        }
        if (!hasEmail || !hasPassword) {
            throw new IllegalStateException("El correo y la contraseña del administrador inicial deben configurarse juntos");
        }
        if (bootstrapAdminPassword.length() < 12) {
            throw new IllegalStateException("La contraseña del administrador inicial debe tener al menos 12 caracteres");
        }
        if (usuarios.findByEmailIgnoreCase(bootstrapAdminEmail).isPresent()) {
            return;
        }

        var user = new Usuario();
        user.setNombreCompleto(bootstrapAdminName);
        user.setEmail(bootstrapAdminEmail.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(bootstrapAdminPassword));
        user.setRol(adminRole);
        user.setActivo(true);
        usuarios.save(user);
    }

    private void synchronizeApprovedRequests() {
        var approvedRequests = solicitudes.findAll().stream()
                .filter(s -> s.getEstado() == pe.org.camaracomercioica.protestos.model.EstadoSolicitud.APROBADA_ENTIDAD 
                        || s.getEstado() == pe.org.camaracomercioica.protestos.model.EstadoSolicitud.FINALIZADA
                        || s.getEstado() == pe.org.camaracomercioica.protestos.model.EstadoSolicitud.LEVANTAMIENTO_PROCESADO)
                .toList();
        for (var s : approvedRequests) {
            var activeProtests = protestos.findByDeudorIdAndEntidadIdAndEstado(s.getDeudor().getId(), s.getEntidad().getId(), pe.org.camaracomercioica.protestos.model.EstadoProtesto.VIGENTE);
            if (!activeProtests.isEmpty()) {
                for (var protesto : activeProtests) {
                    protesto.setEstado(pe.org.camaracomercioica.protestos.model.EstadoProtesto.REGULARIZADO);
                    protesto.setActualizadoEn(java.time.Instant.now());
                }
                protestos.saveAll(activeProtests);
            }
        }
    }

    private Rol role(String nombre) {
        return roles.findByNombre(nombre).orElseGet(() -> {
            var r = new Rol();
            r.setNombre(nombre);
            return roles.save(r);
        });
    }

    private EntidadFinanciera entidadPrincipal() {
        return entidad("20111111111", "Banco Ica", "Mesa de Operaciones", "operaciones@bancoica.local");
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

    private Deudor deudorInicial() {
        return deudores.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.RUC, "20123456789")
                .orElseGet(() -> {
                    var d = new Deudor();
                    d.setTipoDocumento(TipoDocumento.RUC);
                    d.setNumeroDocumento("20123456789");
                    d.setNombreRazonSocial("Distribuidora Sol del Sur S.R.L.");
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

    private void analistaInicial(Usuario usuario) {
        boolean exists = analistas.findAll().stream()
                .anyMatch(a -> a.getUsuario().getId().equals(usuario.getId()));
        if (exists) {
            return;
        }

        var a = new Analista();
        a.setUsuario(usuario);
        a.setCodigo("AN-CCI-001");
        analistas.save(a);
    }

}
