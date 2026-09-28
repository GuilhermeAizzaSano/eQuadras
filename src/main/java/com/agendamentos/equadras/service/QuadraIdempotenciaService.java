package com.agendamentos.equadras.service;

import com.agendamentos.equadras.dto.request.QuadraCriacaoDTO;
import com.agendamentos.equadras.dto.response.QuadraResponseDTO;
import com.agendamentos.equadras.exception.RegraNegocioException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** B1: evita quadras duplicadas quando o mesmo formulário é enviado mais de uma vez. Vale para uma instância. */
@Service
public class QuadraIdempotenciaService {

    private record Entrada(QuadraCriacaoDTO dto, CompletableFuture<QuadraResponseDTO> resposta) {}

    private final QuadraService quadraService;
    private final Cache<String, Entrada> entradas = Caffeine.newBuilder()
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

        // A criação roda fora do lock interno do cache; quem chega depois aguarda o mesmo resultado
        Entrada nova = new Entrada(dto, new CompletableFuture<>());
        Entrada existente = entradas.asMap().putIfAbsent(adminId + ":" + chave, nova);
        if (existente != null) {
            if (!existente.dto().equals(dto)) {
                throw new RegraNegocioException("IDEMPOTENCIA_CONFLITO",
                        "Esta chave de idempotência já foi usada com outros dados.");
            }
            return aguardar(existente.resposta());
        }

        try {
            QuadraResponseDTO resposta = quadraService.cadastrar(dto, adminId);
            nova.resposta().complete(resposta);
            return resposta;
        } catch (RuntimeException e) {
            // A falha não fica guardada: um novo envio com a mesma chave tenta de novo
            entradas.asMap().remove(adminId + ":" + chave, nova);
            nova.resposta().completeExceptionally(e);
            throw e;
        }
    }

    private static QuadraResponseDTO aguardar(CompletableFuture<QuadraResponseDTO> resposta) {
        try {
            return resposta.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw e;
        }
    }
}
