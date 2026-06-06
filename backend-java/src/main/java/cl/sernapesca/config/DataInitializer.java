package cl.sernapesca.config;

import cl.sernapesca.laboratorio.Laboratorio;
import cl.sernapesca.laboratorio.LaboratorioRepository;
import cl.sernapesca.usuario.Usuario;
import cl.sernapesca.usuario.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;

    public DataInitializer(LaboratorioRepository laboratorioRepository, UsuarioRepository usuarioRepository) {
        this.laboratorioRepository = laboratorioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create demo laboratorio if not exists
        if (laboratorioRepository.findByCodigo("LAB01").isEmpty()) {
            Laboratorio lab = Laboratorio.builder()
                    .codigo("LAB01")
                    .nombre("Laboratorio Test")
                    .tipo_laboratorio("RRA")
                    .activo(true)
                    .build();
            lab = laboratorioRepository.save(lab);
        }

        // Create demo admin user if not exists
        if (usuarioRepository.findByRut("12345678-9").isEmpty()) {
            Usuario u = Usuario.builder()
                    .rut("12345678-9")
                    .nombre_completo("Admin Test")
                    .email("admin@example.com")
                    .rol("ADMINISTRADOR")
                    .activo(true)
                    .laboratorio(null)
                    .build();
            usuarioRepository.save(u);
        }
    }
}
