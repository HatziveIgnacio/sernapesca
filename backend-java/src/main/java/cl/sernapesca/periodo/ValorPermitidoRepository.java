package cl.sernapesca.periodo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ValorPermitidoRepository extends JpaRepository<ValorPermitido, Long> {

    List<ValorPermitido> findByPeriodoId(Long periodoId);

    List<ValorPermitido> findByPeriodoIdAndCampo(Long periodoId, String campo);

    boolean existsByPeriodoId(Long periodoId);

    /** Borra todos los valores de un período antes de re-extraer (idempotencia). */
    @Transactional
    void deleteByPeriodoId(Long periodoId);
}
