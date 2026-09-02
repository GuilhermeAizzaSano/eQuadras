package com.agendamentos.equadras.controller;

import com.agendamentos.equadras.model.Notificacao;
import com.agendamentos.equadras.security.UsuarioAutenticado;
import com.agendamentos.equadras.security.UsuarioLogado;
import com.agendamentos.equadras.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return notificacaoService.assinar(usuarioLogado.id());
    }

    @GetMapping("/admin")
    public ResponseEntity<List<Notificacao>> listarPorAdmin(@UsuarioLogado UsuarioAutenticado usuarioLogado) {
        return ResponseEntity.ok(notificacaoService.listarPorAdmin(usuarioLogado.id()));
    }

    @PutMapping("/{id}/ler")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id, @UsuarioLogado UsuarioAutenticado usuarioLogado) {
        notificacaoService.marcarComoLidaSeDoUsuario(id, usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }
}
