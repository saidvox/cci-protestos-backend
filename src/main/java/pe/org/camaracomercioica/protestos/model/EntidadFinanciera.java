package pe.org.camaracomercioica.protestos.model;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="entidades_financieras") @Getter @Setter @NoArgsConstructor
public class EntidadFinanciera { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true,length=11) private String ruc; @Column(nullable=false,length=150) private String razonSocial; @Column(length=150) private String contacto; @Column(length=150) private String email; @Column(nullable=false) private boolean activo=true; @Column(nullable=false,updatable=false) private Instant creadoEn=Instant.now(); }
