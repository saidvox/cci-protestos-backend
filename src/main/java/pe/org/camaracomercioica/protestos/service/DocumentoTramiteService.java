package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.DocumentoTramiteResponse;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.DocumentoTramite;
import pe.org.camaracomercioica.protestos.repository.DocumentoTramiteRepository;
import pe.org.camaracomercioica.protestos.util.StoragePaths;
import pe.org.camaracomercioica.protestos.util.UploadValidator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoTramiteService {
    private final DocumentoTramiteRepository repository;

    @Value("${app.storage.max-bytes:10485760}")
    private long maxBytes;

    @Value("${app.storage.location:./storage}")
    private String storageLocation;

    @Transactional(readOnly = true)
    public List<DocumentoTramiteResponse> listar(boolean incluirInactivos) {
        var documentos = incluirInactivos
                ? repository.findAllByOrderByActivoDescOrdenAscCreadoEnAsc()
                : repository.findByActivoTrueOrderByOrdenAscCreadoEnAsc();
        return documentos.stream().map(this::map).toList();
    }

    @Transactional
    public DocumentoTramiteResponse crear(String titulo, String descripcion, Integer orden, MultipartFile file) throws IOException {
        String mime = new UploadValidator(maxBytes).validateDocument(file);
        if (!"application/pdf".equals(mime)) {
            throw new pe.org.camaracomercioica.protestos.exception.BadRequestException("Solo se permiten documentos PDF");
        }

        StoredFile stored = store(file, "documentos-tramite", mime);
        cleanupOnRollback(stored.key());

        var documento = new DocumentoTramite();
        documento.setTitulo(titulo.trim());
        documento.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        documento.setOrden(orden == null ? 0 : orden);
        documento.setNombreOriginal(file.getOriginalFilename());
        documento.setStorageKey(stored.key());
        documento.setMimeType(stored.mimeType());
        documento.setSizeBytes(stored.sizeBytes());
        documento.setChecksumSha256(stored.checksumSha256());
        documento = repository.saveAndFlush(documento);
        return map(documento);
    }

    @Transactional
    public DocumentoTramiteResponse desactivar(Long id) {
        var documento = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento de tramite no encontrado"));
        documento.setActivo(false);
        return map(documento);
    }

    @Transactional(readOnly = true)
    public Download descargar(Long id) throws MalformedURLException {
        var documento = repository.findById(id)
                .filter(DocumentoTramite::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Documento de tramite no encontrado"));
        Path root = storageRoot();
        Path path = root.resolve(documento.getStorageKey()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new ResourceNotFoundException("Archivo fisico no encontrado");
        }
        Resource resource = new UrlResource(path.toUri());
        return new Download(documento.getNombreOriginal(), documento.getMimeType(), resource);
    }

    private DocumentoTramiteResponse map(DocumentoTramite d) {
        return new DocumentoTramiteResponse(
                d.getId(),
                d.getTitulo(),
                d.getDescripcion(),
                d.getNombreOriginal(),
                "/api/documentos-tramite/" + d.getId() + "/download",
                d.getSizeBytes(),
                d.isActivo(),
                d.getOrden(),
                d.getCreadoEn()
        );
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
            return ".pdf";
        }
        int index = name.lastIndexOf('.');
        return index < 0 ? ".pdf" : name.substring(index).toLowerCase();
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

    private record StoredFile(String key, String mimeType, long sizeBytes, String checksumSha256) {
    }

    public record Download(String filename, String mimeType, Resource resource) {
    }
}
