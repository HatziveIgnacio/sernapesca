package cl.sernapesca.reporte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    List<Reporte> findByAnioAndMesAndTipoOrderByFechaEnvioDesc(Integer anio, Integer mes, String tipo);

    List<Reporte> findByPeriodoId(Long periodoId);

    List<Reporte> findAllByOrderByFechaEnvioDesc();
}
