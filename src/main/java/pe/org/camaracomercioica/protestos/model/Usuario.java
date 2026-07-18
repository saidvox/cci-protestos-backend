package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "usuarios",
        indexes = {
                @Index(name = "idx_usuarios_email", columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidad_id")
    private EntidadFinanciera entidad;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "deudor_id", unique = true)
    private Deudor deudor;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private int sessionVersion;

    @Column(nullable = false)
    private long ultimaNotificacionVistaId;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public String getTipoDocumento() {
        return deudor == null ? null : deudor.getTipoDocumento().name();
    }

    public String getNumeroDocumento() {
        return deudor == null ? null : deudor.getNumeroDocumento();
    }

    public void setTipoDocumento(String tipoDocumento) {
        ensureDeudor();
        deudor.setTipoDocumento(TipoDocumento.valueOf(tipoDocumento.toUpperCase()));
    }

    public void setNumeroDocumento(String numeroDocumento) {
        ensureDeudor();
        deudor.setNumeroDocumento(numeroDocumento);
    }

    private void ensureDeudor() {
        if (deudor == null) {
            deudor = new Deudor();
            deudor.setNombreRazonSocial(nombreCompleto);
            deudor.setTipoPersona(TipoPersona.NATURAL);
        }
    }
}
