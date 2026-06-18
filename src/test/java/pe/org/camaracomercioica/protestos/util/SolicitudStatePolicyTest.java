package pe.org.camaracomercioica.protestos.util;

import org.junit.jupiter.api.Test;
import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;
import static org.assertj.core.api.Assertions.*;

class SolicitudStatePolicyTest {
  @Test void permiteFlujoNormal() {
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.REGISTRADA, EstadoSolicitud.EN_REVISION)).isTrue();
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.EN_REVISION, EstadoSolicitud.APROBADA)).isTrue();
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.OBSERVADA, EstadoSolicitud.EN_REVISION)).isTrue();
  }
  @Test void rechazaTransicionDesdeEstadoFinal() {
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.APROBADA, EstadoSolicitud.EN_REVISION)).isFalse();
  }
}
