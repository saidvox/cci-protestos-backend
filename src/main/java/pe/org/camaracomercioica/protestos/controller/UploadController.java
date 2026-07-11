package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.DocumentoResponse;
import pe.org.camaracomercioica.protestos.dto.CargaExcelResponse;
import pe.org.camaracomercioica.protestos.dto.ExcelImportResponse;
import pe.org.camaracomercioica.protestos.dto.ExcelValidationResponse;
import pe.org.camaracomercioica.protestos.dto.UploadResponse;
import pe.org.camaracomercioica.protestos.service.UploadService;
import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Carga de Archivos", description = "API para la carga de documentos de sustento y archivos Excel de importación masiva")
public class UploadController {

    private final UploadService service;

    private boolean staff(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_CCI_ADMIN")
                        || a.getAuthority().equals("ROLE_CCI_STAFF")
                        || a.getAuthority().equals("ROLE_BANK_ANALYST"));
    }

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
        return service.documento(solicitudId, file, auth.getName(), staff(auth));
    }

    @GetMapping("/api/documentos/solicitud/{solicitudId}")
    @Operation(summary = "Listar documentos adjuntos", description = "Lista los archivos cargados como sustento para una solicitud.")
    public List<DocumentoResponse> listarDocumentos(@PathVariable Long solicitudId, Authentication auth) {
        return service.listarDocumentos(solicitudId, auth.getName(), staff(auth));
    }

    @GetMapping("/api/documentos/{id}/download")
    @Operation(summary = "Descargar documento adjunto", description = "Descarga o previsualiza un documento cargado como sustento.")
    public ResponseEntity<Resource> descargarDocumento(
            @PathVariable Long id,
            @RequestParam(defaultValue = "attachment") String disposition,
            Authentication auth
    ) throws IOException {
        return service.descargarDocumento(id, auth.getName(), staff(auth), "inline".equalsIgnoreCase(disposition));
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

    @GetMapping("/api/excel/cargas")
    @Operation(summary = "Listar historial de cargas", description = "Devuelve las cargas Excel reales realizadas por el usuario autenticado.")
    public List<CargaExcelResponse> listarCargas(Authentication authentication) {
        return service.listarCargas(authentication.getName());
    }

    @PostMapping(value = "/api/excel/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validar plantilla Excel", description = "Revisa estructura, columnas, catálogos y filas del Excel antes de guardar registros en la base de datos.")
    @ApiResponse(responseCode = "200", description = "Validación completada")
    @ApiResponse(responseCode = "400", description = "Formato inválido o plantilla incompatible")
    public ExcelValidationResponse validarExcel(@RequestPart MultipartFile file) throws IOException {
        return service.validateExcel(file);
    }

    @PostMapping(value = "/api/excel/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Importar plantilla Excel", description = "Valida nuevamente el archivo y, si no tiene errores, registra deudores, protestos y el historial de carga.")
    @ApiResponse(responseCode = "201", description = "Excel importado correctamente")
    @ApiResponse(responseCode = "400", description = "Formato inválido o errores de validación en filas")
    public ExcelImportResponse importarExcel(
            @RequestPart MultipartFile file,
            Authentication a
    ) throws IOException {
        return service.importExcel(file, a.getName());
    }
}
