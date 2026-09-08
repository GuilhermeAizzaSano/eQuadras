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
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;
    private final String fixedToken;
    private final Long fixedUserId;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(
            @Value("${jwt.secret}") String segredo,
            @Value("${jwt.expiracao-ms}") long expiracaoMs,
            @Value("${jwt.fixed-token:}") String fixedToken,
            @Value("${jwt.fixed-user-id:}") Long fixedUserId) {
        
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                bytes = md.digest(bytes);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao gerar SHA-256 da chave JWT", e);
            }
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMs = expiracaoMs;
        this.fixedToken = fixedToken;
        this.fixedUserId = fixedUserId;
    }

    public boolean isFixedToken(String token) {
        return fixedToken != null && !fixedToken.isBlank() && fixedToken.equals(token);
    }

    public Long getFixedUserId() {
        return fixedUserId;
    }

    public String gerarToken(Usuario usuario) {
        String roleStr = usuario.getRole() != null ? usuario.getRole().name() : "CLIENT";
        String scope = "ADMIN".equalsIgnoreCase(roleStr) ? "read,write" : "read";

        return Jwts.builder()
                .subject(usuario.getId_usuario().toString())
                .claim("role", roleStr)
                .claim("scope", scope)
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
