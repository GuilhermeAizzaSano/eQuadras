package com.agendamentos.equadras.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Componente de proteção contra força bruta em contas de usuário (ataques distribuídos por botnets).
 * Limita o número de falhas consecutivas de autenticação por endereço de e-mail.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FALHAS_CONSECUTIVAS = 5;
    private static final long JANELA_BLOQUEIO_MS = 15 * 60 * 1000L; // 15 minutos

    private static class RegistroTentativas {
        final AtomicInteger falhas = new AtomicInteger(0);
        volatile long timestampInicio = System.currentTimeMillis();

        void incrementar() {
            falhas.incrementAndGet();
        }

        int getQuantidade() {
            return falhas.get();
        }

        long getTempoRestanteSegundos() {
            long diff = JANELA_BLOQUEIO_MS - (System.currentTimeMillis() - timestampInicio);
            return Math.max(1, diff / 1000);
        }

        boolean expirou() {
            return System.currentTimeMillis() - timestampInicio > JANELA_BLOQUEIO_MS;
        }
    }

    private final Map<String, RegistroTentativas> falhasPorEmail = new ConcurrentHashMap<>();

    private String normalizarEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    /**
     * Verifica se o e-mail está temporariamente bloqueado por excesso de tentativas falhas.
     */
    public boolean isEmailBloqueado(String email) {
        String chave = normalizarEmail(email);
        if (chave.isEmpty()) return false;

        RegistroTentativas reg = falhasPorEmail.get(chave);
        if (reg == null) return false;

        if (reg.expirou()) {
            falhasPorEmail.remove(chave);
            return false;
        }

        return reg.getQuantidade() >= MAX_FALHAS_CONSECUTIVAS;
    }

    /**
     * Retorna os segundos restantes de bloqueio para o cabeçalho Retry-After.
     */
    public long getRetryAfterSegundos(String email) {
        String chave = normalizarEmail(email);
        RegistroTentativas reg = falhasPorEmail.get(chave);
        return reg != null ? reg.getTempoRestanteSegundos() : 60;
    }

    /**
     * Registra uma tentativa falha para o e-mail.
     */
    public void registrarFalha(String email) {
        String chave = normalizarEmail(email);
        if (chave.isEmpty()) return;

        falhasPorEmail.compute(chave, (k, v) -> {
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
     * Limpa o histórico de falhas após um login com sucesso.
     */
    public void registrarSucesso(String email) {
        String chave = normalizarEmail(email);
        if (!chave.isEmpty()) {
            falhasPorEmail.remove(chave);
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void limparExpirados() {
        falhasPorEmail.entrySet().removeIf(entry -> entry.getValue().expirou());
    }
}
