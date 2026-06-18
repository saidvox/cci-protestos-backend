package pe.org.camaracomercioica.protestos.model;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="auditoria") @Getter @Setter @NoArgsConstructor
public class Auditoria { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=150) private String actor; @Column(nullable=false,length=80) private String accion; @Column(nullable=false,length=80) private String recurso; @Column(length=80) private String recursoId; @Column(length=500) private String detalle; @Column(nullable=false,updatable=false) private Instant fecha=Instant.now(); }
