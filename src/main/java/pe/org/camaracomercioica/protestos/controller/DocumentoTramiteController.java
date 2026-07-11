package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.DocumentoTramiteResponse;
import pe.org.camaracomercioica.protestos.service.DocumentoTramiteService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documentos-tramite")
@RequiredArgsConstructor
@Tag(name = "Documentos de tramite", description = "Catalogo de PDFs oficiales descargables por los deudores")
public class DocumentoTramiteController {
    private final DocumentoTramiteService service;

    @GetMapping
    @Operation(summary = "Listar documentos oficiales", description = "Lista documentos oficiales activos o, para ERP, tambien inactivos.")
    @ApiResponse(responseCode = "200", description = "Documentos obtenidos correctamente")
    public List<DocumentoTramiteResponse> listar(
            @RequestParam(defaultValue = "false") boolean incluirInactivos
    ) {
        return service.listar(incluirInactivos);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir documento oficial", description = "Permite al ERP cargar un PDF oficial para que el deudor lo descargue.")
    @ApiResponse(responseCode = "201", description = "Documento creado")
    public DocumentoTramiteResponse crear(
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Integer orden,
            @RequestParam(required = false, defaultValue = "FORMATO_REQUERIDO") String tipo,
            @RequestPart MultipartFile file
    ) throws IOException {
        return service.crear(titulo, descripcion, orden, tipo, file);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar documento oficial", description = "Elimina definitivamente el registro y su archivo almacenado.")
    @ApiResponse(responseCode = "204", description = "Documento eliminado")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Descargar documento oficial", description = "Descarga el PDF oficial activo.")
    @ApiResponse(responseCode = "200", description = "Archivo PDF")
    public ResponseEntity<?> descargar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "attachment") String disposition
    ) throws IOException {
        var download = service.descargar(id);
        var contentDisposition = "inline".equalsIgnoreCase(disposition)
                ? ContentDisposition.inline()
                : ContentDisposition.attachment();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition
                        .filename(download.filename())
                        .build()
                        .toString())
                .body(download.resource());
    }
}
