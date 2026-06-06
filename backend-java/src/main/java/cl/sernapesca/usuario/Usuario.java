package cl.sernapesca.usuario;

import jakarta.persistence.*;
import lombok.*;

import cl.sernapesca.laboratorio.Laboratorio;

@Entity
@Table(name = "Usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_usuario;

    @Column(nullable = false, unique = true, length = 20)
    private String rut;

    @Column(nullable = false, length = 200)
    private String nombre_completo;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String rol; // LABORATORIO, SERNAPESCA, ADMINISTRADOR

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_laboratorio")
    private Laboratorio laboratorio;
}