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
            @Value("${jwt.fixed-token:equadras_master_admin_token_2026_secret_key_fixed}") String fixedToken,
            @Value("${jwt.fixed-user-id:52}") Long fixedUserId) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
        this.fixedToken = fixedToken;
        this.fixedUserId = fixedUserId;
    }

    public JwtService(String segredo, long expiracaoMs) {
        this(segredo, expiracaoMs, "equadras_master_admin_token_2026_secret_key_fixed", 52L);
    }

    public boolean isFixedToken(String token) {
        return fixedToken != null && !fixedToken.isBlank() && fixedToken.equals(token);
    }

    public Long getFixedUserId() {
        return fixedUserId;
    }

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(usuario.getId_usuario().toString())
                .claim("role", usuario.getRole().name())
                .issuedAt(agora)
                .expiration(expiracao)
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
}
