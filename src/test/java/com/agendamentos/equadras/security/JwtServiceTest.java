package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "chave-de-teste-com-pelo-menos-32-bytes-de-tamanho",
            28800000L
    );

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
