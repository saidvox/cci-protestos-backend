package pe.org.camaracomercioica.protestos.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.org.camaracomercioica.protestos.dto.RegisterRequest;
import pe.org.camaracomercioica.protestos.exception.ConflictException;
import pe.org.camaracomercioica.protestos.model.Deudor;
import pe.org.camaracomercioica.protestos.model.Rol;
import pe.org.camaracomercioica.protestos.model.TipoDocumento;
import pe.org.camaracomercioica.protestos.model.TipoPersona;
import pe.org.camaracomercioica.protestos.repository.DeudorRepository;
import pe.org.camaracomercioica.protestos.repository.RolRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthRegistrationIntegrationTest {

    @Autowired AuthService authService;
    @Autowired DeudorRepository deudores;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;

    @Test
    void registroPublicoAsociaUsuarioConDeudorExistenteDelExcel() {
        ensureDebtorRole();
        var deudor = new Deudor();
        deudor.setTipoDocumento(TipoDocumento.DNI);
        deudor.setNumeroDocumento("73451290");
        deudor.setNombreRazonSocial("Deudor Importado Excel");
        deudor.setTipoPersona(TipoPersona.NATURAL);
        deudor.setEmail("excel.deudor@test.local");
        deudor = deudores.save(deudor);

        authService.register(new RegisterRequest(
                "Nombre Ingresado En Registro",
                "registro.deudor@test.local",
                "Password123",
                "DNI",
                "73451290"
        ));

        var usuario = usuarios.findByEmailIgnoreCase("registro.deudor@test.local").orElseThrow();
        assertThat(usuario.getDeudor().getId()).isEqualTo(deudor.getId());
        var asociado = deudores.findById(deudor.getId()).orElseThrow();
        assertThat(asociado.getTipoDocumento()).isEqualTo(TipoDocumento.DNI);
        assertThat(asociado.getNumeroDocumento()).isEqualTo("73451290");
        assertThat(asociado.getNombreRazonSocial()).isEqualTo("Deudor Importado Excel");
    }

    @Test
    void registroPublicoCreaDeudorSiDocumentoNoExiste() {
        ensureDebtorRole();

        authService.register(new RegisterRequest(
                "Empresa Nueva SAC",
                "empresa.nueva@test.local",
                "Password123",
                "RUC",
                "20612345678"
        ));

        var usuario = usuarios.findByEmailIgnoreCase("empresa.nueva@test.local").orElseThrow();
        assertThat(usuario.getDeudor()).isNotNull();
        var deudor = deudores.findById(usuario.getDeudor().getId()).orElseThrow();
        assertThat(deudor.getTipoDocumento()).isEqualTo(TipoDocumento.RUC);
        assertThat(deudor.getNumeroDocumento()).isEqualTo("20612345678");
        assertThat(deudor.getTipoPersona()).isEqualTo(TipoPersona.JURIDICA);
    }

    @Test
    void registroPublicoRechazaDocumentoQueYaTieneCuenta() {
        ensureDebtorRole();
        authService.register(new RegisterRequest("Primer Usuario", "primer.usuario@test.local", "Password123", "DNI", "73451291"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "Segundo Usuario",
                "segundo.usuario@test.local",
                "Password123",
                "DNI",
                "73451291"
        ))).isInstanceOf(ConflictException.class);
    }

    private void ensureDebtorRole() {
        roles.findByNombre("USER_DEBTOR").orElseGet(() -> {
            var role = new Rol();
            role.setNombre("USER_DEBTOR");
            return roles.save(role);
        });
    }
}
