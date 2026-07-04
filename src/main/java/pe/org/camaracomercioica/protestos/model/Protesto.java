package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "protestos",
        indexes = {
                @Index(name = "idx_protestos_deudor", columnList = "deudor_id"),
                @Index(name = "idx_protestos_entidad", columnList = "entidad_id"),
                @Index(name = "idx_protestos_estado", columnList = "estado"),
                @Index(name = "idx_protestos_fecha", columnList = "fechaProtesto")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Protesto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entidad_id")
    private EntidadFinanciera entidad;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "deudor_id")
    private Deudor deudor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_carga_id")
    private CargaExcel origenCarga;

    @Column(length = 50)
    private String numeroTitulo;

    @Column(nullable = false, length = 50)
    private String tipoTitulo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(nullable = false)
    private LocalDate fechaProtesto;

    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoProtesto estado = EstadoProtesto.VIGENTE;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(nullable = false)
    private Instant actualizadoEn = Instant.now();

    public String getNumeroDocumento() {
        return deudor.getNumeroDocumento();
    }

    public String getNombreDeudor() {
        return deudor.getNombreRazonSocial();
    }

    public boolean isVigente() {
        return estado == EstadoProtesto.VIGENTE;
    }
}
