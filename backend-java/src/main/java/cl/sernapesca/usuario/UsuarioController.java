package cl.sernapesca.usuario;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository repo;

    public UsuarioController(UsuarioRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Usuario> list() { return repo.findAll(); }

    @PostMapping
    public Usuario create(@RequestBody Usuario u) { return repo.save(u); }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Usuario u) {
        return repo.findById(id).map(existing -> {
            existing.setNombre_completo(u.getNombre_completo());
            existing.setEmail(u.getEmail());
            existing.setRol(u.getRol());
            existing.setActivo(u.getActivo());
            // laboratorio binding left to caller (set id_laboratorio if needed)
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Integer id) {
        return repo.findById(id).map(existing -> {
            existing.setActivo(!existing.getActivo());
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}