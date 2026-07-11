package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "documentos_tramite",
        indexes = {
                @Index(name = "idx_documentos_tramite_activo", columnList = "activo"),
                @Index(name = "idx_documentos_tramite_orden", columnList = "orden")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentoTramite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType = "application/pdf";

    @Column(nullable = false, length = 30, columnDefinition = "varchar(30) default 'FORMATO_REQUERIDO'")
    private String tipo = "FORMATO_REQUERIDO";

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(nullable = false)
    private int orden = 0;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();
}
