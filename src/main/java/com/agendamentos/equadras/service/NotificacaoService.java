package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.Notificacao;
import com.agendamentos.equadras.dto.response.NotificacaoResponseDTO;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    
    // Mapa para armazenar os emissores SSE ativos por ID do Admin
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final JsonMapper objectMapper;

    public NotificacaoService(NotificacaoRepository notificacaoRepository, UsuarioRepository usuarioRepository, JsonMapper objectMapper) {
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    public SseEmitter assinar(Long usuarioId) {
        SseEmitter antigoEmitter = emitters.get(usuarioId);
        if (antigoEmitter != null) {
            try {
                antigoEmitter.complete();
            } catch (Exception ignored) {
            }
        }

        // Timeout de 1 hora
        SseEmitter emitter = new SseEmitter(3600000L);
        emitters.put(usuarioId, emitter);

        emitter.onCompletion(() -> emitters.remove(usuarioId, emitter));
        emitter.onTimeout(() -> emitters.remove(usuarioId, emitter));
        emitter.onError((e) -> emitters.remove(usuarioId, emitter));

        return emitter;
    }

    /**
     * Persiste uma notificação por admin na transação corrente (lote único) e agenda o envio SSE
     * para depois do commit, sem acessar o banco. Sem transação ativa, envia imediatamente.
     */
    @Transactional
    public void notificarAdmins(Set<Long> adminIds, String mensagem) {
        if (adminIds == null || adminIds.isEmpty()) return;

        List<Long> destinos = List.copyOf(adminIds);
        List<Notificacao> salvas = notificacaoRepository.saveAll(destinos.stream()
                .map(id -> new Notificacao(usuarioRepository.getReferenceById(id), mensagem))
                .toList());
        List<NotificacaoResponseDTO> dtos = salvas.stream().map(NotificacaoResponseDTO::fromEntity).toList();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviarSse(destinos, dtos);
                }
            });
        } else {
            enviarSse(destinos, dtos);
        }
    }

    private void enviarSse(List<Long> adminIds, List<NotificacaoResponseDTO> dtos) {
        for (int i = 0; i < dtos.size(); i++) {
            Long adminId = adminIds.get(i);
            SseEmitter emitter = emitters.get(adminId);
            if (emitter == null) continue;
            NotificacaoResponseDTO dto = dtos.get(i);
            try {
                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("id", dto.id());
                payload.put("mensagem", dto.mensagem());
                payload.put("lida", dto.lida());
                payload.put("dataCriacao", dto.dataCriacao() != null ? dto.dataCriacao().toString() : java.time.LocalDateTime.now().toString());
                emitter.send(SseEmitter.event().name("notificacao").data(objectMapper.writeValueAsString(payload)));
            } catch (IOException | JacksonException e) {
                // Se falhar o envio, a conexão foi perdida
                emitters.remove(adminId);
            }
        }
    }

    public Page<Notificacao> listarPorAdmin(Long adminId, Pageable pageable) {
        return notificacaoRepository.findByAdminIdAndExcluidaFalseOrderByDataCriacaoDesc(adminId, pageable);
    }

    public void marcarComoLida(Long idNotificacao, Long usuarioId) {
        marcarComoLidaSeDoUsuario(idNotificacao, usuarioId);
    }

    @Transactional
    public void marcarComoLidaSeDoUsuario(Long idNotificacao, Long usuarioId) {
        // UPDATE condicional ao dono: sem SELECT prévio e sem revelar se a notificação de outro admin existe
        if (notificacaoRepository.marcarComoLidaDoAdmin(idNotificacao, usuarioId) == 0) {
            throw new IllegalArgumentException("Você não tem permissão para alterar esta notificação.");
        }
    }

    @Transactional
    public void marcarTodasComoLidas(Long adminId) {
        notificacaoRepository.marcarTodasComoLidas(adminId);
    }

    @Transactional
    public void excluirTodas(Long adminId) {
        notificacaoRepository.marcarTodasComoExcluidas(adminId);
    }
}
