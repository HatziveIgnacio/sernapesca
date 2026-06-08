package cl.sernapesca.periodo;

import jakarta.persistence.*;
import lombok.*;

/**
 * Un valor permitido para un campo específico dentro de un período.
 *
 * Se extrae de las tablas maestras (ListObjects) de la plantilla Excel que
 * SERNAPESCA sube cada mes. Cuando un laboratorio sube su reporte, cada celda
 * se valida contra estos valores en lugar de listas hardcodeadas en Java.
 *
 * Ejemplo: para el período (2026, 7, RRA), campo="tipo_control",
 * valor="Verificación SERNAPESCA".
 */
@Entity
@Table(
    name = "valor_permitido",
    indexes = {
        @Index(name = "ix_valor_periodo_campo", columnList = "periodo_id, campo")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValorPermitido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK lógica → PeriodoReporte.id */
    @Column(name = "periodo_id", nullable = false)
    private Long periodoId;

    /** Nombre del campo del reporte, ej. "tipo_control", "codigo_producto" */
    @Column(nullable = false, length = 60)
    private String campo;

    /** Valor maestro permitido (código o texto) */
    @Column(nullable = false, length = 500)
    private String valor;
}
