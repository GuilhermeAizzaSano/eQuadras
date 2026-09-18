package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(
            @Value("${jwt.secret}") String segredo,
            @Value("${jwt.expiracao-ms:28800000}") long expiracaoMs) {
        
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("O segredo JWT (jwt.secret) deve possuir no mínimo 32 bytes (256 bits).");
        }

        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(Usuario usuario) {
        String roleStr = usuario.getRole() != null ? usuario.getRole().name() : "CLIENT";
        String scope = "ADMIN".equalsIgnoreCase(roleStr) ? "read,write" : "read";
        Instant agora = Instant.now();
        Instant expira = agora.plusMillis(expiracaoMs);

        return Jwts.builder()
                .subject(usuario.getId_usuario().toString())
                .claim("role", roleStr)
                .claim("scope", scope)
                .claim("ver", usuario.getTokenVersion())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expira))
                .signWith(chave)
                .compact();
    }

    public Claims validarEExtrairClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracaoMs() {
        return expiracaoMs;
    }
}
