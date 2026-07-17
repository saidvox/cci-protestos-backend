package pe.org.camaracomercioica.protestos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.bootstrap.admin.email=admin@demo.local",
        "app.bootstrap.admin.password="
})
class BootstrapAdminRestartTest {

    @Test
    void existingAdminAllowsRemovingBootstrapPassword() {
    }
}
