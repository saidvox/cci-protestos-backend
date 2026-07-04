package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "carga_excel_filas",
        indexes = {
                @Index(name = "idx_carga_filas_carga", columnList = "carga_id"),
                @Index(name = "idx_carga_filas_estado", columnList = "estado")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CargaExcelFila {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "carga_id")
    private CargaExcel carga;

    @Column(nullable = false)
    private int numeroFila;

    @Column(nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 180)
    private String nombreDeudor;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(nullable = false)
    private LocalDate fechaProtesto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFilaCarga estado = EstadoFilaCarga.VALIDA;

    @Column(length = 500)
    private String detalleError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protesto_id")
    private Protesto protesto;
}
