package cl.sernapesca.laboratorio;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Laboratorio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Laboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_laboratorio;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 100)
    private String tipo_laboratorio;

    @Column(nullable = false)
    private Boolean activo = true;
}