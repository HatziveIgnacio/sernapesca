package cl.sernapesca.auth;

import cl.sernapesca.usuario.Usuario;
import cl.sernapesca.usuario.UsuarioRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ExternalAuthFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;
    private final String headerName;

    public ExternalAuthFilter(UsuarioRepository usuarioRepository, String headerName) {
        this.usuarioRepository = usuarioRepository;
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String rut = request.getHeader(headerName);

        if (rut != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var opt = usuarioRepository.findByRut(rut);
            if (opt.isPresent()) {
                Usuario u = opt.get();
                String rol = u.getRol() != null ? u.getRol() : "";
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        rut,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // If header present but user not found, reject request
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\": \"User not found for provided RUT\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
