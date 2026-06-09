package cl.sernapesca.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;
    private final ReporteRepository repo;
    private final RegistroReporteRepository registroRepo;

    /**
     * Finaliza y sube un reporte validado. Recibe el archivo Excel, lo valida y,
     * si no tiene errores, persiste sus registros en BD.
     *
     * POST /api/reportes/finalizar
     */
    @PostMapping("/finalizar")
    public ResponseEntity<?> finalizar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateType") String templateType,
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "mes", required = false) Integer mes) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No se recibió ningún archivo"));
        }
        if (!"RRA".equals(templateType) && !"RRA_FAR".equals(templateType)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tipo de plantilla inválido. Use RRA o RRA_FAR"));
        }

        ReporteService.FinalizarResumen r = service.finalizar(file, templateType, anio, mes);
        return ResponseEntity.ok(Map.of(
                "reporteId", r.reporteId(),
                "tipo", r.tipo(),
                "anio", r.anio(),
                "mes", r.mes(),
                "nombreLaboratorio", r.nombreLaboratorio(),
                "totalRegistros", r.totalRegistros(),
                "mensaje", "Reporte subido correctamente con " + r.totalRegistros() + " registros."
        ));
    }

    /** Lista los reportes subidos, opcionalmente filtrados por período. */
    @GetMapping
    public List<Reporte> listar(@RequestParam(required = false) Integer anio,
                                @RequestParam(required = false) Integer mes,
                                @RequestParam(required = false) String tipo) {
        if (anio != null && mes != null && tipo != null) {
            return repo.findByAnioAndMesAndTipoOrderByFechaEnvioDesc(anio, mes, tipo);
        }
        return repo.findAllByOrderByFechaEnvioDesc();
    }

    /** Registros (filas) de un reporte. Lo usará el consolidado para apilar. */
    @GetMapping("/{id}/registros")
    public List<RegistroReporte> registros(@PathVariable Long id) {
        return registroRepo.findByReporteIdOrderByNumeroFila(id);
    }
}
