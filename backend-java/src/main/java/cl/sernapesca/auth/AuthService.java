package cl.sernapesca.auth;

import cl.sernapesca.auth.dto.LoginRequest;
import cl.sernapesca.auth.dto.LoginResponse;
import cl.sernapesca.usuario.Usuario;
import cl.sernapesca.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest req) {
        var opt = usuarioRepository.findByRut(req.getRut());
        if (opt.isEmpty()) return null;
        Usuario u = opt.get();
        if (!Boolean.TRUE.equals(u.getActivo())) return null;

        var claims = new HashMap<String, Object>();
        claims.put("id", u.getId_usuario());
        claims.put("rut", u.getRut());
        claims.put("rol", u.getRol());
        claims.put("nombre", u.getNombre_completo());
        if (u.getLaboratorio() != null) claims.put("id_laboratorio", u.getLaboratorio().getId_laboratorio());

        String token = jwtUtil.generateToken(claims);
        return new LoginResponse(token, u.getRol(), u.getNombre_completo());
    }
}