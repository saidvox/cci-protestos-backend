package pe.org.camaracomercioica.protestos.model;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="documentos") @Getter @Setter @NoArgsConstructor
public class Documento { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(optional=false) @JoinColumn(name="solicitud_id") private Solicitud solicitud; @Column(nullable=false,length=255) private String nombreOriginal; @Column(nullable=false,length=255) private String nombreAlmacenado; @Column(nullable=false,length=100) private String tipoContenido; @Column(nullable=false) private long tamano; @Column(nullable=false,updatable=false) private Instant creadoEn=Instant.now(); }
