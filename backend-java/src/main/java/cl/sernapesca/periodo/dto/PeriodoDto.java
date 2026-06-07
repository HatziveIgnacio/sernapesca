package cl.sernapesca.periodo.dto;

import cl.sernapesca.periodo.EstadoPeriodo;
import cl.sernapesca.periodo.PeriodoReporte;

import java.time.LocalDateTime;

public record PeriodoDto(
        Long id,
        Integer anio,
        Integer mes,
        String tipo,
        String plantillaId,
        EstadoPeriodo estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCierre,
        Boolean consolidadoSolicitado
) {
    public static PeriodoDto from(PeriodoReporte p) {
        return new PeriodoDto(
                p.getId(),
                p.getAnio(),
                p.getMes(),
                p.getTipo(),
                p.getPlantillaId(),
                p.getEstado(),
                p.getFechaCreacion(),
                p.getFechaCierre(),
                p.getConsolidadoSolicitado()
        );
    }
}
