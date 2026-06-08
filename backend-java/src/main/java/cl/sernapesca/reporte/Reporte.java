package cl.sernapesca.reporte;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Un reporte de laboratorio finalizado y subido a SERNAPESCA.
 *
 * Se persiste cuando el laboratorio confirma ("Finalizar y subir datos") un
 * reporte que pasó la validación sin errores. Sus registros (RegistroReporte)
 * quedan disponibles para apilar en el consolidado del período.
 */
@Entity
@Table(name = "reporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK lógica → PeriodoReporte.id (nullable si aún no hay período creado) */
    @Column(name = "periodo_id")
    private Long periodoId;

    /** RRA o RRA_FAR */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    /** Nombre del laboratorio que emite el reporte (tomado del propio archivo) */
    @Column(name = "nombre_laboratorio", length = 200)
    private String nombreLaboratorio;

    @Column(name = "nombre_archivo", length = 300)
    private String nombreArchivo;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "total_registros", nullable = false)
    private Integer totalRegistros;
}
