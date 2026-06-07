package cl.sernapesca.periodo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearPeriodoRequest {

    @NotNull(message = "anio es obligatorio")
    @Min(value = 2020, message = "anio debe ser >= 2020")
    private Integer anio;

    @NotNull(message = "mes es obligatorio")
    @Min(value = 1, message = "mes debe ser entre 1 y 12")
    @Max(value = 12, message = "mes debe ser entre 1 y 12")
    private Integer mes;

    @NotBlank(message = "tipo es obligatorio (RRA o RRA_FAR)")
    private String tipo;

    /** Opcional: id de plantilla a vincular. Puede dejarse vacío y vincular después. */
    private String plantillaId;
}
