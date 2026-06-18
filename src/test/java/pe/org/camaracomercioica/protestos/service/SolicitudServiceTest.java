package pe.org.camaracomercioica.protestos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.org.camaracomercioica.protestos.dto.CambioEstadoRequest;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import pe.org.camaracomercioica.protestos.model.*;
import pe.org.camaracomercioica.protestos.repository.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {
  @Mock SolicitudRepository solicitudes; @Mock UsuarioRepository usuarios; @Mock EntidadFinancieraRepository entidades; @Mock AnalistaRepository analistas; @Mock AuditoriaService auditoria;
  @Test void rechazaCambioInvalido() {
    Solicitud s = new Solicitud(); s.setId(1L); s.setEstado(EstadoSolicitud.APROBADA);
    when(solicitudes.findById(1L)).thenReturn(Optional.of(s));
    var service = new SolicitudService(solicitudes, usuarios, entidades, analistas, auditoria);
    assertThatThrownBy(() -> service.cambiarEstado(1L, new CambioEstadoRequest(EstadoSolicitud.EN_REVISION, null, null), "admin@demo.pe")).isInstanceOf(BadRequestException.class);
    verify(solicitudes, never()).save(any());
  }
  @Test void entidadSoloPuedeCrearParaSuPropiaEntidad() {
    var role=new Rol(); role.setNombre("ENTIDAD"); var propia=new EntidadFinanciera(); propia.setId(10L); var otra=new EntidadFinanciera(); otra.setId(20L);
    var user=new Usuario(); user.setRol(role); user.setEntidad(propia);
    when(usuarios.findByEmailIgnoreCase("entidad@test.local")).thenReturn(Optional.of(user));
    when(entidades.findById(20L)).thenReturn(Optional.of(otra));
    var service=new SolicitudService(solicitudes,usuarios,entidades,analistas,auditoria);
    assertThatThrownBy(()->service.crear(new pe.org.camaracomercioica.protestos.dto.SolicitudRequest(20L,"Motivo"),"entidad@test.local")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    verify(solicitudes,never()).save(any());
  }
}
