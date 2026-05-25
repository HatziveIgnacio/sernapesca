package cl.sernapesca.plantillas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Endpoints para gestión de plantillas mensuales.
 *
 * POST /api/plantillas/upload  → SERNAPESCA sube la plantilla del mes
 * GET  /api/plantillas         → lista todas las plantillas
 * GET  /api/plantillas/{id}/download → descarga una plantilla específica
 */
@Slf4j
@RestController
@RequestMapping("/api/plantillas")
@RequiredArgsConstructor
public class PlantillaController {

    private final PlantillaService plantillaService;

    /**
     * SERNAPESCA sube la plantilla Excel mensual.
     * Parámetros: archivo + año + mes + tipo (RRA o RRA_FAR)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam("tipo") String tipo
    ) {
        log.info("Upload plantilla: tipo={}, anio={}, mes={}, archivo={}", tipo, anio, mes, file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo está vacío"));
        }

        String nombre = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!nombre.endsWith(".xlsx") && !nombre.endsWith(".xlsm")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Solo se permiten archivos .xlsx o .xlsm"));
        }

        try {
            PlantillaMetadata meta = plantillaService.guardar(file, anio, mes, tipo);
            return ResponseEntity.ok(meta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            log.error("Error al guardar plantilla", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Error al guardar el archivo"));
        }
    }

    /**
     * Lista todas las plantillas disponibles.
     */
    @GetMapping
    public ResponseEntity<List<PlantillaMetadata>> listar() {
        return ResponseEntity.ok(plantillaService.listar());
    }

    /**
     * Descarga la plantilla más reciente de un tipo dado.
     * Usado por la vista de Laboratorios para el botón "Descargar Plantilla".
     *
     * GET /api/plantillas/latest/RRA
     * GET /api/plantillas/latest/RRA_FAR
     */
    @GetMapping("/latest/{tipo}")
    public ResponseEntity<Resource> downloadLatest(@PathVariable String tipo) {
        return plantillaService.obtenerUltima(tipo)
                .flatMap(meta -> plantillaService.obtenerArchivo(meta.id())
                        .map(path -> {
                            Resource resource = new PathResource(path);
                            return ResponseEntity.ok()
                                    .header(HttpHeaders.CONTENT_DISPOSITION,
                                            "attachment; filename=\"" + meta.nombreArchivo() + "\"")
                                    .contentType(MediaType.parseMediaType(
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                    .body(resource);
                        }))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Descarga el archivo Excel de una plantilla específica.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        return plantillaService.obtenerArchivo(id)
                .map(path -> {
                    Resource resource = new PathResource(path);
                    String filename = plantillaService.obtenerMetadata(id)
                            .map(PlantillaMetadata::nombreArchivo)
                            .orElse("plantilla.xlsx");
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + filename + "\"")
                            .contentType(MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                            .body(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
