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
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
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
    private static final String TIPO_FORMATO_REQUERIDO = "FORMATO_REQUERIDO";
    private static final String TIPO_GUIA = "GUIA";
    private static final String TIPO_PLANTILLA_EXCEL = "PLANTILLA_EXCEL";

    private final DocumentoTramiteRepository repository;
    private final AuditoriaService auditoria;

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
    public DocumentoTramiteResponse crear(String titulo, String descripcion, Integer orden, String tipo, MultipartFile file, String actor) throws IOException {
        String documentType = normalizeType(tipo);
        var validator = new UploadValidator(maxBytes);
        String mime = TIPO_PLANTILLA_EXCEL.equals(documentType)
                ? validator.validateExcel(file)
                : validator.validateDocument(file);
        if (!TIPO_PLANTILLA_EXCEL.equals(documentType) && !"application/pdf".equals(mime)) {
            throw new BadRequestException("Solo se permiten documentos PDF");
        }

        StoredFile stored = store(file, storagePrefix(documentType), mime);
        cleanupOnRollback(stored.key());

        var documento = new DocumentoTramite();
        documento.setTitulo(titulo.trim());
        documento.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        documento.setOrden(orden == null ? 0 : orden);
        documento.setTipo(documentType);
        documento.setNombreOriginal(file.getOriginalFilename());
        documento.setStorageKey(stored.key());
        documento.setMimeType(stored.mimeType());
        documento.setSizeBytes(stored.sizeBytes());
        documento.setChecksumSha256(stored.checksumSha256());
        documento = repository.saveAndFlush(documento);
        auditoria.registrar(actor, "CREAR", "DOCUMENTO_TRAMITE", documento.getId(), documento.getTitulo());
        return map(documento);
    }

    @Transactional
    public void eliminar(Long id, String actor) {
        var documento = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento de tramite no encontrado"));
        String storageKey = documento.getStorageKey();
        String titulo = documento.getTitulo();
        repository.delete(documento);
        repository.flush();
        auditoria.registrar(actor, "ELIMINAR", "DOCUMENTO_TRAMITE", id, titulo);
        deleteAfterCommit(storageKey);
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
                d.getTipo() == null ? TIPO_FORMATO_REQUERIDO : d.getTipo(),
                d.isActivo(),
                d.getOrden(),
                d.getCreadoEn()
        );
    }

    private String normalizeType(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return TIPO_FORMATO_REQUERIDO;
        }
        String value = tipo.trim().toUpperCase();
        if (!TIPO_FORMATO_REQUERIDO.equals(value) && !TIPO_GUIA.equals(value) && !TIPO_PLANTILLA_EXCEL.equals(value)) {
            throw new BadRequestException("Tipo de documento no permitido");
        }
        return value;
    }

    private String storagePrefix(String tipo) {
        return TIPO_PLANTILLA_EXCEL.equals(tipo) ? "plantillas-excel" : "documentos-tramite";
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

    private void deleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            deleteStoredFile(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteStoredFile(key);
            }
        });
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
