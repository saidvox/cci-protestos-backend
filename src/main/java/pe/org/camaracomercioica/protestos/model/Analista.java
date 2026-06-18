package pe.org.camaracomercioica.protestos.model;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="analistas") @Getter @Setter @NoArgsConstructor
public class Analista { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @OneToOne(optional=false) @JoinColumn(name="usuario_id",unique=true) private Usuario usuario; @Column(nullable=false,length=30) private String codigo; @Column(nullable=false) private boolean disponible=true; }
