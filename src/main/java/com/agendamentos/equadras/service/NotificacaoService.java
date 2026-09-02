package com.agendamentos.equadras.service;

import com.agendamentos.equadras.model.Notificacao;
import com.agendamentos.equadras.model.entity.Usuario;
import com.agendamentos.equadras.repository.NotificacaoRepository;
import com.agendamentos.equadras.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    
    // Mapa para armazenar os emissores SSE ativos por ID do Admin
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

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
                String mensagemEscapada = salva.getMensagem()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
                String json = String.format(
                    "{\"id\": %d, \"mensagem\": \"%s\", \"lida\": false, \"dataCriacao\": \"%s\"}",
                    salva.getId(), mensagemEscapada, salva.getDataCriacao().toString()
                );
                emitter.send(SseEmitter.event().name("notificacao").data(json));
            } catch (IOException e) {
                // Se falhar o envio, a conexão foi perdida
                emitters.remove(adminId);
            }
        }
    }

    public List<Notificacao> listarPorAdmin(Long adminId) {
        return notificacaoRepository.findByAdminIdOrderByDataCriacaoDesc(adminId);
    }

    public void marcarComoLida(Long idNotificacao) {
        Notificacao notif = notificacaoRepository.findById(idNotificacao)
            .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));
        notif.setLida(true);
        notificacaoRepository.save(notif);
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
}
