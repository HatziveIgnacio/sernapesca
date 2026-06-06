package cl.sernapesca.laboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LaboratorioRepository extends JpaRepository<Laboratorio, Integer> {
    Optional<Laboratorio> findByCodigo(String codigo);
}