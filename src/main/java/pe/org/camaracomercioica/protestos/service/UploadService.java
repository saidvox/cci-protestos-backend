package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.UploadResponse;
import pe.org.camaracomercioica.protestos.exception.ResourceNotFoundException;
import pe.org.camaracomercioica.protestos.model.CargaExcel;
import pe.org.camaracomercioica.protestos.model.Documento;
import pe.org.camaracomercioica.protestos.model.EstadoCarga;
import pe.org.camaracomercioica.protestos.repository.CargaExcelRepository;
import pe.org.camaracomercioica.protestos.repository.DocumentoRepository;
import pe.org.camaracomercioica.protestos.repository.SolicitudRepository;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;
import pe.org.camaracomercioica.protestos.util.UploadValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final DocumentoRepository documentos;
    private final CargaExcelRepository cargas;
    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;

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

    @Transactional
    public UploadResponse excel(MultipartFile file, String email) throws IOException {
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
        carga.setResumen("Archivo validado y registrado; importacion pendiente");
        carga = cargas.saveAndFlush(carga);

        return new UploadResponse(carga.getId(), carga.getNombreArchivo(), carga.getEstado().name(), carga.getResumen());
    }

    private StoredFile store(MultipartFile file, String prefix, String mimeType) throws IOException {
        Path root = Paths.get(storageLocation).toAbsolutePath().normalize();
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
            Path root = Paths.get(storageLocation).toAbsolutePath().normalize();
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

    private record StoredFile(String key, String mimeType, long sizeBytes, String checksumSha256) {
    }
}
