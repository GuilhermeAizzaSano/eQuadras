package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.entity.Notificacao;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Notificações em Tempo Real (SSE)", description = "Streaming de eventos e gerenciamento de notificações para administradores.")
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Operation(summary = "Streaming SSE de notificações", description = "Estabelece conexão unidirecional persistente (Server-Sent Events) para receber notificações em tempo real. Requer ROLE_ADMIN.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return notificacaoService.assinar(usuarioLogado.id());
    }

    @Operation(summary = "Listar notificações do administrador", description = "Retorna o histórico de notificações de reservas e pagamentos do administrador autenticado.")
    @GetMapping("/admin")
    public ResponseEntity<List<Notificacao>> listarPorAdmin(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(notificacaoService.listarPorAdmin(usuarioLogado.id()));
    }

    @Operation(summary = "Marcar notificação como lida", description = "Atualiza o estado de leitura da notificação.")
    @PutMapping("/{id}/ler")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        notificacaoService.marcarComoLidaSeDoUsuario(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marcar todas as notificações como lidas", description = "Marca todas as notificações do administrador autenticado como lidas.")
    @PutMapping("/ler-todas")
    public ResponseEntity<Void> marcarTodasComoLidas(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        notificacaoService.marcarTodasComoLidas(usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }
}
