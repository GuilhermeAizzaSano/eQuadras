package com.agendamentos.equadras.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiKeyRateLimiter {

    private static final int MAX_FALHAS_POR_MINUTO = 20;
    private static final int MAX_REGENERACOES_POR_MINUTO = 5;
    private static final long JANELA_MS = 60_000L;

    private static class RegistroTentativas {
        final AtomicInteger contador = new AtomicInteger(0);
        volatile long timestampInicio = System.currentTimeMillis();

        void incrementar() {
            contador.incrementAndGet();
        }

        int getQuantidade() {
            return contador.get();
        }

        long getTempoRestanteSegundos() {
            long diff = JANELA_MS - (System.currentTimeMillis() - timestampInicio);
            return Math.max(1, diff / 1000);
        }

        boolean expirou() {
            return System.currentTimeMillis() - timestampInicio > JANELA_MS;
        }
    }

    private final Map<String, RegistroTentativas> falhasPorIp = new ConcurrentHashMap<>();
    private final Map<Long, RegistroTentativas> regeneracoesPorUsuario = new ConcurrentHashMap<>();

    /**
     * Verifica se o IP está bloqueado por excesso de falhas de autenticação de API-KEY.
     * @return true se o IP estiver bloqueado (limite atingido).
     */
    public boolean isIpBloqueado(String ip) {
        if (ip == null || ip.isBlank()) return false;
        RegistroTentativas reg = falhasPorIp.get(ip);
        if (reg == null) return false;
        if (reg.expirou()) {
            falhasPorIp.remove(ip);
            return false;
        }
        return reg.getQuantidade() >= MAX_FALHAS_POR_MINUTO;
    }

    /**
     * Retorna o tempo em segundos para o cabeçalho Retry-After.
     */
    public long getRetryAfterSegundos(String ip) {
        RegistroTentativas reg = falhasPorIp.get(ip);
        return reg != null ? reg.getTempoRestanteSegundos() : 60;
    }

    /**
     * Registra uma falha de autenticação para o IP informado.
     */
    public void registrarFalha(String ip) {
        if (ip == null || ip.isBlank()) return;
        falhasPorIp.compute(ip, (k, v) -> {
            if (v == null || v.expirou()) {
                RegistroTentativas novo = new RegistroTentativas();
                novo.incrementar();
                return novo;
            }
            v.incrementar();
            return v;
        });
    }

    /**
     * Verifica e consome o limite de regenerações por usuário (máximo 5 por minuto).
     * @return true se a requisição foi permitida, false se o limite foi excedido.
     */
    public boolean tentarRegenerar(Long usuarioId) {
        if (usuarioId == null) return false;
        RegistroTentativas reg = regeneracoesPorUsuario.compute(usuarioId, (k, v) -> {
            if (v == null || v.expirou()) {
                RegistroTentativas novo = new RegistroTentativas();
                novo.incrementar();
                return novo;
            }
            v.incrementar();
            return v;
        });
        return reg.getQuantidade() <= MAX_REGENERACOES_POR_MINUTO;
    }

    @Scheduled(fixedRate = 60_000)
    public void limparRegistrosExpirados() {
        falhasPorIp.entrySet().removeIf(e -> e.getValue().expirou());
        regeneracoesPorUsuario.entrySet().removeIf(e -> e.getValue().expirou());
    }

    // Método utilitário para reset em testes (mesmo pacote), sem expor reset na API pública
    void reset() {
        falhasPorIp.clear();
        regeneracoesPorUsuario.clear();
    }
}
