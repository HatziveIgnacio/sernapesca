package cl.sernapesca.laboratorio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    private final LaboratorioRepository repo;

    public LaboratorioController(LaboratorioRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Laboratorio> list() { return repo.findAll(); }

    @PostMapping
    public Laboratorio create(@RequestBody Laboratorio l) { return repo.save(l); }

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
}