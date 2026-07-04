package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.UploadResponse;
import pe.org.camaracomercioica.protestos.service.UploadService;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Tag(name = "Carga de Archivos", description = "API para la carga de documentos de sustento y archivos Excel de importación masiva")
public class UploadController {

    private final UploadService service;

    @PostMapping(value = "/api/documentos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cargar documento adjunto", description = "Sube un documento o comprobante físico de sustento asociado a una solicitud de trámite.")
    @ApiResponse(responseCode = "201", description = "Documento cargado exitosamente")
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos o error en el archivo")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    public UploadResponse documento(
            @RequestParam Long solicitudId,
            @RequestPart MultipartFile file,
            Authentication auth
    ) throws IOException {
        var staff = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_CCI_ADMIN")
                        || a.getAuthority().equals("ROLE_CCI_STAFF")
                        || a.getAuthority().equals("ROLE_BANK_ANALYST"));
        return service.documento(solicitudId, file, auth.getName(), staff);
    }

    @PostMapping(value = "/api/excel/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cargar archivo Excel", description = "Realiza la importación masiva de protestos y moras desde una plantilla Excel. Exclusivo para analistas y administradores.")
    @ApiResponse(responseCode = "201", description = "Excel procesado e importado con éxito")
    @ApiResponse(responseCode = "400", description = "Formato de archivo inválido o error de validación en las filas")
    @ApiResponse(responseCode = "401", description = "No autorizado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public UploadResponse excel(
            @RequestPart MultipartFile file,
            Authentication a
    ) throws IOException {
        return service.excel(file, a.getName());
    }
}
