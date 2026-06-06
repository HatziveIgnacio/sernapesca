package cl.sernapesca.auth;

import cl.sernapesca.auth.dto.LoginRequest;
import cl.sernapesca.auth.dto.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        var res = authService.login(req);
        if (res == null) return ResponseEntity.status(401).body(java.util.Map.of("message","Unauthorized or inactive"));
        return ResponseEntity.ok(res);
    }
}