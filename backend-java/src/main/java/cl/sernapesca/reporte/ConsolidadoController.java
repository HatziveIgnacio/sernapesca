package cl.sernapesca.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consolidados")
@RequiredArgsConstructor
public class ConsolidadoController {

    private final ConsolidadoService service;

    /**
     * Genera y descarga el consolidado de un período: un Excel con todas las
     * filas de todos los reportes validados, apiladas.
     *
     * GET /api/consolidados/RRA/download?anio=2026&mes=1
     */
    @GetMapping("/{tipo}/download")
    public ResponseEntity<Resource> descargar(@PathVariable String tipo,
                                              @RequestParam int anio,
                                              @RequestParam int mes) throws Exception {
        ConsolidadoService.ConsolidadoArchivo archivo = service.generar(tipo, anio, mes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + archivo.nombreArchivo() + "\"")
                .header("X-Total-Filas", String.valueOf(archivo.totalFilas()))
                .header("X-Total-Reportes", String.valueOf(archivo.totalReportes()))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(archivo.contenido()));
    }
}
