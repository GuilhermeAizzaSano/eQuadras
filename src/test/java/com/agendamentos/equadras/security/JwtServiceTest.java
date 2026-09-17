package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("chave-secreta-para-testes-unitarios-com-jwt-seguro", 3600000L);
    }

    @Test
    void deveGerarTokenEExtrairClaimsCorretamente() {
        Usuario usuario = Usuario.builder()
                .id_usuario(42L)
                .role(Role.ADMIN)
                .build();

        String token = jwtService.gerarToken(usuario);
        Claims claims = jwtService.validarEExtrairClaims(token);

        assertEquals("42", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("read,write", claims.get("scope", String.class));
    }

    @Test
    void deveGerarTokenComExpiracaoEIssuedAt() {
        Usuario usuario = Usuario.builder()
                .id_usuario(100L)
                .role(Role.CLIENT)
                .build();

        String token = jwtService.gerarToken(usuario);

        Claims claims = jwtService.validarEExtrairClaims(token);
        assertNotNull(claims.getExpiration(), "O token de sessão deve ter data de expiração");
        assertNotNull(claims.getIssuedAt(), "O token de sessão deve ter data de emissão");
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()), "Expiração deve ser posterior à emissão");
        assertEquals("read", claims.get("scope", String.class));
    }

    @Test
    void deveRejeitarSegredoComMenosDe32Bytes() {
        assertThrows(IllegalArgumentException.class, () ->
                new JwtService("segredo-curto", 3600000L));
    }

    @Test
    void deveLancarExcecaoParaTokenInvalido() {
        assertThrows(JwtException.class, () -> jwtService.validarEExtrairClaims("token-invalido"));
    }

    @Test
    void deveLancarExcecaoParaTokenAssinadoComOutraChave() {
        JwtService outroServico = new JwtService(
                "outra-chave-de-teste-com-pelo-menos-32-bytes-de-tamanho",
                28800000L
        );
        Usuario usuario = Usuario.builder().id_usuario(1L).role(Role.CLIENT).build();
        String token = outroServico.gerarToken(usuario);

        assertThrows(JwtException.class, () -> jwtService.validarEExtrairClaims(token));
    }
}
