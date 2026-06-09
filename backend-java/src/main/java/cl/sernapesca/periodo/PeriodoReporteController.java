package cl.sernapesca.periodo;

import cl.sernapesca.periodo.dto.CrearPeriodoRequest;
import cl.sernapesca.periodo.dto.PeriodoDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/periodos")
@RequiredArgsConstructor
public class PeriodoReporteController {

    private final PeriodoReporteService service;

    @GetMapping
    public List<PeriodoDto> listar() {
        return service.listar().stream().map(PeriodoDto::from).toList();
    }

    @GetMapping("/{id}")
    public PeriodoDto obtener(@PathVariable Long id) {
        return PeriodoDto.from(service.obtener(id));
    }

    @GetMapping("/activo/{tipo}")
    public PeriodoDto obtenerActivo(@PathVariable String tipo) {
        return PeriodoDto.from(service.obtenerActivoPorTipo(tipo));
    }

    @PostMapping
    public ResponseEntity<PeriodoDto> crear(@Valid @RequestBody CrearPeriodoRequest req) {
        PeriodoReporte creado = service.crear(req);
        return ResponseEntity.status(201).body(PeriodoDto.from(creado));
    }

    @PutMapping("/{id}/cerrar")
    public ResponseEntity<?> cerrar(@PathVariable Long id) {
        PeriodoReporte cerrado = service.cerrar(id);
        return ResponseEntity.ok(Map.of(
                "periodo", PeriodoDto.from(cerrado),
                "mensaje", "Período cerrado. Señal de consolidado emitida."
        ));
    }

    @PutMapping("/{id}/plantilla")
    public PeriodoDto vincularPlantilla(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return PeriodoDto.from(service.vincularPlantilla(id, body.get("plantillaId")));
    }

    /** Re-extrae las listas maestras de la plantilla vinculada al período. */
    @PostMapping("/{id}/extraer-reglas")
    public ResponseEntity<?> extraerReglas(@PathVariable Long id) {
        var resumen = service.extraerReglas(id);
        return ResponseEntity.ok(Map.of(
                "periodoId", resumen.periodoId(),
                "totalValores", resumen.totalValores(),
                "porCampo", resumen.porCampo(),
                "mensaje", "Reglas extraídas correctamente desde la plantilla."
        ));
    }
}
