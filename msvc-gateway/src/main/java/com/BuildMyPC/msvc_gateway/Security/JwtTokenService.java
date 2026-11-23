package com.BuildMyPC.msvc_gateway.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
public class JwtTokenService {
    private final SecretKey clave;

    public JwtTokenService(@Value("${app.jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRETO debe tener al menos 32 bytes para HS256.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
    }

    public Claims validar(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}
