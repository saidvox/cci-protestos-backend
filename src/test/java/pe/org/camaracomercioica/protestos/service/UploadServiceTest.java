package pe.org.camaracomercioica.protestos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import pe.org.camaracomercioica.protestos.model.Solicitud;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.repository.CargaExcelRepository;
import pe.org.camaracomercioica.protestos.repository.DocumentoRepository;
import pe.org.camaracomercioica.protestos.repository.SolicitudRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {
    @Mock DocumentoRepository documentos;
    @Mock CargaExcelRepository cargas;
    @Mock SolicitudRepository solicitudes;
    @Mock UsuarioRepository usuarios;
    @Mock ExcelImportService excelImportService;

    @Test
    void rechazaDocumentoSiUsuarioNoEsPropietario() throws Exception {
        var service = new UploadService(documentos, cargas, solicitudes, usuarios, excelImportService);
        ReflectionTestUtils.setField(service, "maxBytes", 1024L);
        ReflectionTestUtils.setField(service, "storageLocation", "./target/test-storage");

        var owner = new Usuario();
        owner.setEmail("owner@test.local");
        var solicitud = new Solicitud();
        solicitud.setSolicitante(owner);
        when(solicitudes.findById(1L)).thenReturn(Optional.of(solicitud));

        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-1.7".getBytes());

        assertThatThrownBy(() -> service.documento(1L, file, "other@test.local", false))
                .isInstanceOf(AccessDeniedException.class);
        verify(documentos, never()).save(any());
    }
}
