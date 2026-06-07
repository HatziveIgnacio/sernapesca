package cl.sernapesca.periodo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Período mensual de reporte. Vincula una plantilla vigente con un
 * ciclo (año/mes/tipo) y controla si los laboratorios pueden seguir
 * cargando reportes (estado ABIERTO) o si ya se gatilló la generación
 * de consolidado (estado CERRADO).
 */
@Entity
@Table(
    name = "periodo_reporte",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_periodo_anio_mes_tipo",
        columnNames = {"anio", "mes", "tipo"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodoReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    /** RRA o RRA_FAR */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** UUID de la plantilla vinculada (PlantillaMetadata.id). Nullable mientras no se elige. */
    @Column(name = "plantilla_id", length = 64)
    private String plantillaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPeriodo estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /** Marca temporal del gatillo de consolidado. Wireado al módulo de consolidados cuando exista. */
    @Column(name = "consolidado_solicitado", nullable = false)
    private Boolean consolidadoSolicitado;
}
