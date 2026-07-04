package pe.org.camaracomercioica.protestos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "solicitud_eventos",
        indexes = {
                @Index(name = "idx_solicitud_eventos_solicitud", columnList = "solicitud_id"),
                @Index(name = "idx_solicitud_eventos_fecha", columnList = "creadoEn")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SolicitudEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estadoNuevo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Usuario actor;

    @Column(nullable = false, length = 30)
    private String actorRol;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();
}
