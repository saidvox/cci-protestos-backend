package pe.org.camaracomercioica.protestos.model;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="cargas_excel") @Getter @Setter @NoArgsConstructor
public class CargaExcel { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(optional=false) @JoinColumn(name="usuario_id") private Usuario usuario; @Column(nullable=false,length=255) private String nombreArchivo; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EstadoCarga estado=EstadoCarga.RECIBIDA; @Column(length=500) private String resumen; @Column(nullable=false,updatable=false) private Instant creadoEn=Instant.now(); }
