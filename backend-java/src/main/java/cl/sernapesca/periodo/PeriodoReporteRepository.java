package cl.sernapesca.periodo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodoReporteRepository extends JpaRepository<PeriodoReporte, Long> {

    Optional<PeriodoReporte> findByAnioAndMesAndTipo(Integer anio, Integer mes, String tipo);

    Optional<PeriodoReporte> findFirstByTipoAndEstadoOrderByAnioDescMesDesc(String tipo, EstadoPeriodo estado);

    List<PeriodoReporte> findAllByOrderByAnioDescMesDescTipoAsc();
}
