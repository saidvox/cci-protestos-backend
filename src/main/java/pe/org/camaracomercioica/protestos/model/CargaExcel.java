package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "cargas_excel",
        indexes = {
                @Index(name = "idx_cargas_excel_entidad", columnList = "entidad_id"),
                @Index(name = "idx_cargas_excel_usuario", columnList = "usuario_id"),
                @Index(name = "idx_cargas_excel_estado", columnList = "estado")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CargaExcel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidad_id")
    private EntidadFinanciera entidad;

    @OneToMany(mappedBy = "carga", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CargaExcelFila> filas = new ArrayList<>();

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCarga estado = EstadoCarga.RECIBIDA;

    @Column(nullable = false)
    private int totalFilas = 0;

    @Column(nullable = false)
    private int filasImportadas = 0;

    @Column(nullable = false)
    private int filasConError = 0;

    @Column(length = 500)
    private String resumen;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();
}
