package cl.sernapesca.laboratorio;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cl.sernapesca.laboratorio.validators.ValidationTypes.ValidationResult;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    private final LaboratorioRepository repo;
    private final LaboratorioExcelService excelService;

    public LaboratorioController(LaboratorioRepository repo, LaboratorioExcelService excelService) {
        this.repo = repo;
        this.excelService = excelService;
    }

    @GetMapping
    public List<Laboratorio> list() { 
        return repo.findAll(); 
    }

    @PostMapping
    public Laboratorio create(@RequestBody Laboratorio l) { 
        return repo.save(l); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Laboratorio l) {
        return repo.findById(id).map(existing -> {
            existing.setNombre(l.getNombre());
            existing.setTipo_laboratorio(l.getTipo_laboratorio());
            existing.setActivo(l.getActivo());
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateType") String templateType,
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "mes", required = false) Integer mes) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No se recibió ningún archivo");
        }

        if (!"RRA".equals(templateType) && !"RRA_FAR".equals(templateType)) {
            return ResponseEntity.badRequest().body("Tipo de plantilla inválido. Use RRA o RRA_FAR");
        }

        try {
            ValidationResult result = excelService.validateFile(file, templateType, anio, mes);
            if (result.totalRows() == 0) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message",
                    "El archivo no contiene registros para validar. Verifique que haya cargado datos en la hoja 'ingreso'."));
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // Tipo de plantilla incorrecto u otra validación de entrada → 400 con mensaje legible
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message",
                        "Error procesando el archivo. Verifique que sea un .xlsx válido. Detalle: " + e.getMessage()));
        }
    }

    @GetMapping("/template/{type}")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable String type) {
        if (!"RRA".equals(type) && !"RRA_FAR".equals(type)) {
            return ResponseEntity.badRequest().build();
        }

        String filename = "RRA".equals(type) ? "RRA_plantilla.xlsm" : "RRA_FAR_plantilla.xlsm";
        Path path = Paths.get("templates", filename);
        File file = path.toFile();

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}