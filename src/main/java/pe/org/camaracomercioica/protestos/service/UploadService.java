package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.DocumentoResponse;
import pe.org.camaracomercioica.protestos.dto.ExcelImportResponse;
import pe.org.camaracomercioica.protestos.dto.ExcelValidationResponse;
import pe.org.camaracomercioica.protestos.dto.UploadResponse;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.CargaExcel;
import pe.org.camaracomercioica.protestos.model.Documento;
import pe.org.camaracomercioica.protestos.model.EstadoCarga;
import pe.org.camaracomercioica.protestos.repository.CargaExcelRepository;
import pe.org.camaracomercioica.protestos.repository.DocumentoRepository;
import pe.org.camaracomercioica.protestos.repository.SolicitudRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;
import pe.org.camaracomercioica.protestos.util.StoragePaths;
import pe.org.camaracomercioica.protestos.util.UploadValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final DocumentoRepository documentos;
    private final CargaExcelRepository cargas;
    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final ExcelImportService excelImportService;

    @Value("${app.storage.max-bytes:10485760}")
    private long maxBytes;

    @Value("${app.storage.location:./storage}")
    private String storageLocation;

    @Transactional
    public UploadResponse documento(Long solicitudId, MultipartFile file, String email, boolean staff) throws IOException {
        String mime = new UploadValidator(maxBytes).validateDocument(file);
        var solicitud = solicitudes.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (!staff && !solicitud.getSolicitante().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("No puede adjuntar documentos a esta solicitud");
        }

        StoredFile stored = store(file, "documentos", mime);
        cleanupOnRollback(stored.key());

        var documento = new Documento();
        documento.setSolicitud(solicitud);
        documento.setNombreOriginal(file.getOriginalFilename());
        documento.setStorageKey(stored.key());
        documento.setMimeType(stored.mimeType());
        documento.setSizeBytes(stored.sizeBytes());
        documento.setChecksumSha256(stored.checksumSha256());
        documento = documentos.saveAndFlush(documento);

        return new UploadResponse(documento.getId(), documento.getNombreOriginal(), "RECIBIDO", "Documento almacenado");
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarDocumentos(Long solicitudId, String email, boolean staff) {
        var solicitud = solicitudes.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (!staff && !solicitud.getSolicitante().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("No puede ver documentos de esta solicitud");
        }
        return documentos.findBySolicitudIdOrderByCreadoEnAsc(solicitudId).stream()
                .map(doc -> map(doc, solicitudId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descargarDocumento(Long id, String email, boolean staff, boolean inline) throws IOException {
        var documento = documentos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        var solicitud = documento.getSolicitud();
        if (!staff && !solicitud.getSolicitante().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("No puede descargar documentos de esta solicitud");
        }

        Path root = storageRoot();
        Path target = root.resolve(documento.getStorageKey()).normalize();
        if (!target.startsWith(root) || !Files.exists(target)) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        }

        Resource resource = new UrlResource(target.toUri());
        var disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(documento.getNombreOriginal())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.getMimeType()))
                .contentLength(documento.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @Transactional
    public ExcelValidationResponse validateExcel(MultipartFile file) throws IOException {
        return excelImportService.validate(file, maxBytes);
    }

    @Transactional
    public ExcelImportResponse importExcel(MultipartFile file, String email) throws IOException {
        String mime = new UploadValidator(maxBytes).validateExcel(file);
        var usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        StoredFile stored = store(file, "excel", mime);
        cleanupOnRollback(stored.key());

        var carga = new CargaExcel();
        carga.setUsuario(usuario);
        carga.setEntidad(usuario.getEntidad());
        carga.setNombreArchivo(file.getOriginalFilename());
        carga.setStorageKey(stored.key());
        carga.setMimeType(stored.mimeType());
        carga.setSizeBytes(stored.sizeBytes());
        carga.setChecksumSha256(stored.checksumSha256());
        carga.setEstado(EstadoCarga.RECIBIDA);
        carga.setResumen("Archivo recibido para validacion e importacion");
        carga = cargas.saveAndFlush(carga);

        var response = excelImportService.importRows(file, carga, maxBytes);
        cargas.saveAndFlush(carga);
        return response;
    }

    @Transactional
    public UploadResponse excel(MultipartFile file, String email) throws IOException {
        var imported = importExcel(file, email);
        return new UploadResponse(imported.cargaId(), imported.filename(), imported.status(), imported.summary());
    }

    private StoredFile store(MultipartFile file, String prefix, String mimeType) throws IOException {
        Path root = storageRoot();
        Files.createDirectories(root.resolve(prefix));

        String key = prefix + "/" + UUID.randomUUID() + extension(file.getOriginalFilename());
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Ruta de archivo invalida");
        }

        MessageDigest digest = sha256();
        long size;
        try (var input = new DigestInputStream(file.getInputStream(), digest);
             var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            size = input.transferTo(output);
        }
        return new StoredFile(key, mimeType, size, HexFormat.of().formatHex(digest.digest()));
    }

    private void cleanupOnRollback(String key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteStoredFile(key);
                    }
                }
            });
        }
    }

    private void deleteStoredFile(String key) {
        try {
            Path root = storageRoot();
            Path target = root.resolve(key).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    private String extension(String name) {
        if (name == null) {
            return "";
        }
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index).toLowerCase();
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path storageRoot() {
        return StoragePaths.resolveRoot(storageLocation);
    }

    private DocumentoResponse map(Documento documento, Long solicitudId) {
        return new DocumentoResponse(
                documento.getId(),
                solicitudId,
                documento.getNombreOriginal(),
                documento.getMimeType(),
                documento.getSizeBytes(),
                "/api/documentos/" + documento.getId() + "/download",
                documento.getCreadoEn()
        );
    }

    private record StoredFile(String key, String mimeType, long sizeBytes, String checksumSha256) {
    }
}
