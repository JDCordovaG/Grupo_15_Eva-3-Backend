package com.BuildMyPC.msvc_auth_service.Services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtServicio {
    private final SecretKey clave;
    private final long duracionMs;

    public JwtServicio(
            @Value("${app.jwt.secreto}") String secreto,
            @Value("${app.jwt.duracion-ms:3600000}") long duracionMs) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRETO debe tener al menos 32 bytes para HS256.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracionMs = duracionMs;
    }

    public String generar(String email, String rol, Long usuarioId) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .claim("usuarioId", usuarioId)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMs))
                .signWith(clave)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}
