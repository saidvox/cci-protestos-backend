package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "deudores",
        uniqueConstraints = @UniqueConstraint(name = "uk_deudores_documento", columnNames = {"tipoDocumento", "numeroDocumento"}),
        indexes = {
                @Index(name = "idx_deudores_documento", columnList = "numeroDocumento"),
                @Index(name = "idx_deudores_nombre", columnList = "nombreRazonSocial")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Deudor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 180)
    private String nombreRazonSocial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPersona tipoPersona = TipoPersona.NATURAL;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(nullable = false)
    private Instant actualizadoEn = Instant.now();
}
