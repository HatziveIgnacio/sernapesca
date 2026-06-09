package cl.sernapesca.reporte;

import jakarta.persistence.*;
import lombok.*;

/**
 * Una fila de datos de un reporte finalizado. Guarda todos los campos de la
 * fila como JSON para poder reconstruirla al apilar el consolidado del período.
 */
@Entity
@Table(name = "registro_reporte", indexes = {
    @Index(name = "ix_registro_reporte", columnList = "reporte_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → Reporte.id */
    @Column(name = "reporte_id", nullable = false)
    private Long reporteId;

    /** Número de fila en el Excel original (1-based, para trazabilidad) */
    @Column(name = "numero_fila", nullable = false)
    private Integer numeroFila;

    /** Todos los campos de la fila serializados como JSON */
    @Lob
    @Column(name = "datos_json", nullable = false)
    private String datosJson;
}
