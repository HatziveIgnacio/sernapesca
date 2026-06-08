package cl.sernapesca.reporte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroReporteRepository extends JpaRepository<RegistroReporte, Long> {

    List<RegistroReporte> findByReporteIdOrderByNumeroFila(Long reporteId);
}
