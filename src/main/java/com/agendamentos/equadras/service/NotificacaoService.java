package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.entity.Notificacao;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    
    // Mapa para armazenar os emissores SSE ativos por ID do Admin
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public NotificacaoService(NotificacaoRepository notificacaoRepository, UsuarioRepository usuarioRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviarNotificacao(Long adminId, String mensagem) {
        Usuario admin = usuarioRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Admin não encontrado"));

        // 1. Persistir no banco
        Notificacao notificacao = new Notificacao(admin, mensagem);
        Notificacao salva = notificacaoRepository.save(notificacao);

        // 2. Tentar enviar em tempo real se o admin estiver conectado
        SseEmitter emitter = emitters.get(adminId);
        if (emitter != null) {
            try {
                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("id", salva.getId());
                payload.put("mensagem", salva.getMensagem());
                payload.put("lida", salva.isLida());
                payload.put("dataCriacao", salva.getDataCriacao() != null ? salva.getDataCriacao().toString() : java.time.LocalDateTime.now().toString());
                String json = objectMapper.writeValueAsString(payload);
                emitter.send(SseEmitter.event().name("notificacao").data(json));
            } catch (IOException e) {
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

    public void marcarComoLidaSeDoUsuario(Long idNotificacao, Long usuarioId) {
        Notificacao notif = notificacaoRepository.findById(idNotificacao)
            .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));
        if (notif.getAdmin() == null || !notif.getAdmin().getId_usuario().equals(usuarioId)) {
            throw new IllegalArgumentException("Você não tem permissão para alterar esta notificação.");
        }
        notif.setLida(true);
        notificacaoRepository.save(notif);
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
