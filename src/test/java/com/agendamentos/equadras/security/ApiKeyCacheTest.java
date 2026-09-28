package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyCacheTest {

    private final ApiKeyCache cache = new ApiKeyCache();
    private final UsuarioAutenticado usuario = new UsuarioAutenticado(10L, Role.ADMIN, TipoAutenticacao.API_KEY);

    @Test
    @DisplayName("Segunda busca do mesmo hash não consulta o carregador")
    void reutilizaEntradaEmCache() {
        AtomicInteger cargas = new AtomicInteger();
        cache.obter("h1", h -> { cargas.incrementAndGet(); return Optional.of(usuario); });
        Optional<UsuarioAutenticado> segunda = cache.obter("h1", h -> { cargas.incrementAndGet(); return Optional.of(usuario); });

        assertEquals(1, cargas.get());
        assertEquals(Optional.of(usuario), segunda);
    }

    @Test
    @DisplayName("Hash sem usuário não é cacheado")
    void naoCacheiaAusencia() {
        AtomicInteger cargas = new AtomicInteger();
        cache.obter("h2", h -> { cargas.incrementAndGet(); return Optional.empty(); });
        cache.obter("h2", h -> { cargas.incrementAndGet(); return Optional.empty(); });

        assertEquals(2, cargas.get());
    }

    @Test
    @DisplayName("Invalidar por hash e por usuário remove a entrada")
    void invalida() {
        cache.obter("h3", h -> Optional.of(usuario));
        cache.invalidarHash("h3");
        AtomicInteger cargas = new AtomicInteger();
        cache.obter("h3", h -> { cargas.incrementAndGet(); return Optional.of(usuario); });
        assertEquals(1, cargas.get());

        cache.invalidarUsuario(10L);
        cache.obter("h3", h -> { cargas.incrementAndGet(); return Optional.of(usuario); });
        assertEquals(2, cargas.get());
    }

    @Test
    @DisplayName("Registro de uso só é liberado uma vez por janela por usuário")
    void throttleDeUso() {
        assertTrue(cache.deveRegistrarUso(10L));
        assertFalse(cache.deveRegistrarUso(10L));
        assertTrue(cache.deveRegistrarUso(11L));
    }
}
