package com.agendamentos.equadras.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/**
 * Cache de autenticação por API key (hash SHA-256 → usuário autenticado) e throttle do registro de último uso.
 * Instância única (uma VM): a invalidação local em revogar/regenerar/editar/excluir é imediata.
 */
@Component
public class ApiKeyCache {

    private static final Duration TTL_AUTENTICACAO = Duration.ofSeconds(60);
    private static final Duration JANELA_ULTIMO_USO = Duration.ofMinutes(5);
    private static final int MAXIMO_ENTRADAS = 1_000;

    private final Cache<String, UsuarioAutenticado> porHash = Caffeine.newBuilder()
            .expireAfterWrite(TTL_AUTENTICACAO)
            .maximumSize(MAXIMO_ENTRADAS)
            .build();

    private final Cache<Long, Boolean> usoRegistrado = Caffeine.newBuilder()
            .expireAfterWrite(JANELA_ULTIMO_USO)
            .maximumSize(MAXIMO_ENTRADAS)
            .build();

    public Optional<UsuarioAutenticado> obter(String hash, Function<String, Optional<UsuarioAutenticado>> carregador) {
        UsuarioAutenticado emCache = porHash.getIfPresent(hash);
        if (emCache != null) {
            return Optional.of(emCache);
        }
        Optional<UsuarioAutenticado> carregado = carregador.apply(hash);
        carregado.ifPresent(u -> porHash.put(hash, u));
        return carregado;
    }

    public void invalidarHash(String hash) {
        if (hash != null) {
            porHash.invalidate(hash);
        }
    }

    public void invalidarUsuario(Long usuarioId) {
        porHash.asMap().values().removeIf(u -> u.id().equals(usuarioId));
    }

    public void invalidarTudo() {
        porHash.invalidateAll();
        usoRegistrado.invalidateAll();
    }

    /** true na primeira chamada por usuário dentro da janela; false nas seguintes. */
    public boolean deveRegistrarUso(Long usuarioId) {
        return usoRegistrado.asMap().putIfAbsent(usuarioId, Boolean.TRUE) == null;
    }
}
