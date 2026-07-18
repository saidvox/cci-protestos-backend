package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "invitaciones_analista")
@Getter
@Setter
@NoArgsConstructor
public class InvitacionAnalista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "analista_id")
    private Analista analista;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "creado_por", nullable = false, length = 150)
    private String creadoPor;
}
