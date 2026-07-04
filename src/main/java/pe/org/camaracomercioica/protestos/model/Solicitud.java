package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "solicitudes",
        indexes = {
                @Index(name = "idx_solicitudes_estado", columnList = "estado"),
                @Index(name = "idx_solicitudes_entidad", columnList = "entidad_id"),
                @Index(name = "idx_solicitudes_deudor", columnList = "deudor_id"),
                @Index(name = "idx_solicitudes_creado", columnList = "creadoEn")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "deudor_id")
    private Deudor deudor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entidad_id")
    private EntidadFinanciera entidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analista_id")
    private Analista analista;

    @ManyToMany
    @JoinTable(
            name = "solicitud_protestos",
            joinColumns = @JoinColumn(name = "solicitud_id"),
            inverseJoinColumns = @JoinColumn(name = "protesto_id")
    )
    private Set<Protesto> protestos = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.REGISTRADA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoTramite tipoTramite = TipoTramite.REGISTRO_PROTESTO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(nullable = false, length = 1000)
    private String motivo;

    @Column(length = 1000)
    private String observacion;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(nullable = false)
    private Instant actualizadoEn = Instant.now();

    public String getNumeroDocumentoDeudor() {
        return deudor.getNumeroDocumento();
    }

    public void setNumeroDocumentoDeudor(String numeroDocumentoDeudor) {
        if (deudor == null) {
            deudor = new Deudor();
            deudor.setTipoDocumento(numeroDocumentoDeudor != null && numeroDocumentoDeudor.length() == 11 ? TipoDocumento.RUC : TipoDocumento.DNI);
            deudor.setNombreRazonSocial("Deudor " + numeroDocumentoDeudor);
        }
        deudor.setNumeroDocumento(numeroDocumentoDeudor);
    }
}
