package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** B1: evita quadras duplicadas quando o mesmo formulário é enviado mais de uma vez. Vale para uma instância. */
@Service
public class QuadraIdempotenciaService {

    private final QuadraService quadraService;
    private final Cache<String, QuadraResponseDTO> respostas = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public QuadraIdempotenciaService(QuadraService quadraService) {
        this.quadraService = quadraService;
    }

    public QuadraResponseDTO cadastrar(QuadraCriacaoDTO dto, Long adminId, String chave) {
        if (chave == null || chave.isBlank()) {
            return quadraService.cadastrar(dto, adminId);
        }
        if (chave.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key deve ter no máximo 100 caracteres.");
        }
        // get(chave, loader) bloqueia chamadas concorrentes com a mesma chave; exceção não fica em cache
        return respostas.get(adminId + ":" + chave, k -> quadraService.cadastrar(dto, adminId));
    }
}
