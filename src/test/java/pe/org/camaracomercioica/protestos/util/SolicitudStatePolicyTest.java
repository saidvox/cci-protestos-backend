package pe.org.camaracomercioica.protestos.util;

import org.junit.jupiter.api.Test;
import pe.org.camaracomercioica.protestos.model.EstadoSolicitud;
import static org.assertj.core.api.Assertions.*;

class SolicitudStatePolicyTest {
  @Test void permiteFlujoNormal() {
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.REGISTRADA, EstadoSolicitud.EN_REVISION_CCI)).isTrue();
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.EN_REVISION_CCI, EstadoSolicitud.DERIVADA_ENTIDAD)).isTrue();
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.OBSERVADA_CCI, EstadoSolicitud.EN_REVISION_CCI)).isTrue();
  }
  @Test void rechazaTransicionDesdeEstadoFinal() {
    assertThat(SolicitudStatePolicy.canTransition(EstadoSolicitud.APROBADA_ENTIDAD, EstadoSolicitud.EN_REVISION_CCI)).isFalse();
  }
}
