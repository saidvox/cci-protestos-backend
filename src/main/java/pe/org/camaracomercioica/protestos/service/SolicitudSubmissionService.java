package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.SolicitudRequest;
import pe.org.camaracomercioica.protestos.dto.SolicitudResponse;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudSubmissionService {
    private final SolicitudService solicitudes;
    private final UploadService uploads;

    @Transactional
    public SolicitudResponse crear(SolicitudRequest request, List<MultipartFile> files, String actor) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Debe adjuntar los documentos requeridos");
        }

        var solicitud = solicitudes.crear(request, actor);
        for (var file : files) {
            uploads.documento(solicitud.id(), file, actor, false);
        }
        return solicitud;
    }
}
