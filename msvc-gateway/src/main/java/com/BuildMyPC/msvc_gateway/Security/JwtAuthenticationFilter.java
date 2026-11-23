package com.BuildMyPC.msvc_gateway.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwt;

    public JwtAuthenticationFilter(JwtTokenService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.validar(header.substring(7).trim());
                String rol = normalizarRol(claims.get("rol", String.class));
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String normalizarRol(String rol) {
        String valor = rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return switch (valor) {
            case "ADMIN" -> "ADMINISTRADOR";
            case "TECH", "TECHNICIAN" -> "TECNICO";
            case "USER" -> "CLIENTE";
            default -> valor;
        };
    }
}
